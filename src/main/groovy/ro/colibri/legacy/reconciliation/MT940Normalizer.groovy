package ro.colibri.legacy.reconciliation

import com.google.common.collect.ImmutableList
import com.prowidesoftware.swift.model.field.Field61
import com.prowidesoftware.swift.model.field.Field86
import com.prowidesoftware.swift.model.mt.mt9xx.MT940
import groovy.transform.CompileStatic
import ro.colibri.entities.comercial.Document
import ro.colibri.entities.comercial.Gestiune
import ro.colibri.legacy.service.LegacySyncServices
import ro.flexbiz.util.commons.LocalDateUtils
import ro.flexbiz.util.commons.PresentationUtils
import ro.flexbiz.util.commons.StringUtils
import ro.flexbiz.util.commons.model.GenericValue
import ro.flexbiz.util.commons.reconciliation.Normalizer
import ro.flexbiz.util.commons.reconciliation.model.NormalizedRecord

import java.time.LocalDate
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Stream

@CompileStatic
class MT940Normalizer implements Normalizer {
    private int gestiuneId;
    private int contBancarId;

    MT940Normalizer(int gestiuneId, int contBancarId) {
        this.gestiuneId = gestiuneId;
        this.contBancarId = contBancarId;
    }

    @Override
    Stream<NormalizedRecord> normalize(Object o) {
        MT940 mt = (MT940) o;
        List<NormalizedRecord> records = new ArrayList<>();

        ImmutableList<Gestiune> allGestiuni = LegacySyncServices.allGestiuni();
        Gestiune gestiune = allGestiuni.stream()
                .filter(g -> g.getId() == gestiuneId)
                .findFirst()
                .get();
        Gestiune l1 = allGestiuni.stream()
                .filter(g -> g.getImportName().equals("L1"))
                .findFirst()
                .get();

        BigDecimal openingBalance = mt.getField60F().amount();
        LocalDate openingDate = LocalDateUtils.toLocalDate(mt.getField61().get(0).date());
        BigDecimal closingBalance = mt.getField62F().amount();
        LocalDate closingDate = LocalDateUtils.toLocalDate(mt.getField61().get(mt.getField61().size()-1).date());
        GenericValue balance = GenericValue.of();
        balance.put("type", "balance");
        balance.put("openingBalance", openingBalance);
        balance.put("closingBalance", closingBalance);
        balance.put("openingDate", openingDate);
        balance.put("closingDate", closingDate);
        records.add(new NormalizedRecord(mt.getField60F().toString()+PresentationUtils.NEWLINE+mt.getField62F().toString(), balance));

        for (int i = 0; i < mt.getField61().size(); i++) {
            GenericValue gv = GenericValue.of();
            Field61 f61 = mt.getField61().get(i);
            Field86 f86 = mt.getField86().get(i);
            String details = f86.getValue().replace("000^20", "")
                    .replace("^21", "") // ^21 is line separator
                    .replace("\n", "")
                    .replace("\r", "");
            Pattern pattern = Pattern.compile("\\^32(.+)\$");
            Matcher matcher = pattern.matcher(details);
            String partner = matcher.find() ? matcher.group(1).replace("^33", "")
                    .toUpperCase().replace("  ", " ").trim() : null;
            partner = partner?.startsWith("SC") ? partner.substring(2).trim() : partner;

            if (f61.getDebitCreditMark().equalsIgnoreCase("D")) {
                int index = details.indexOf("^24");
                String description = index != -1 ? details.substring(0, index).replace("^22", " ") : null;

                // IF line starts with ignore case 'COMIS' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc=CARD | docNr=NC | date=closingDate | doc.name=COM | doc.total=sum(lines) | banca=contBancarId
                // ELSE IF line.partner = 'LINIC SRL' THEN doc.gestiune=L1 | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc='ORDIN PLATA' | docNr=OP | date=line.date | doc.name=line.description | doc.total=line.total | banca=contBancarId
                // ELSE Partner=line.partner | TipDoc.PLATA | doc='ORDIN PLATA' | date=line.date | doc.total=line.total | banca=contBancarId
                if (details.toUpperCase().startsWith("COMIS")) {
                    gv.put("gestiuneId", gestiuneId);
                    gv.put("gestiuneName", gestiune.getName());
                    gv.put("partnerName", "RAIFF "+gestiune.getName().toUpperCase());
                    gv.put("tipDoc", Document.TipDoc.PLATA);
                    gv.put("doc", "CARD");
                    gv.put("nrDoc", "NC");
                    gv.put("dataDoc", closingDate);
                    gv.put("name", "COM");
                    gv.put("total", f61.amount());
                    gv.put("contBancarId", contBancarId);
                } else if (StringUtils.globalIsMatch(partner, "LINIC SRL", StringUtils.TextFilterMethod.EQUALS)) {
                    gv.put("gestiuneId", l1.getId());
                    gv.put("gestiuneName", l1.getName());
                    gv.put("partnerName", "RAIFF "+gestiune.getName().toUpperCase());
                    gv.put("tipDoc", Document.TipDoc.PLATA);
                    gv.put("doc", "ORDIN PLATA");
                    gv.put("nrDoc", "OP");
                    gv.put("dataDoc", LocalDateUtils.toLocalDate(f61.date()));
                    gv.put("name", description);
                    gv.put("total", f61.amount());
                    gv.put("contBancarId", contBancarId);
                } else {
                    gv.put("gestiuneId", gestiuneId);
                    gv.put("partnerName", partner);
                    gv.put("tipDoc", Document.TipDoc.PLATA);
                    gv.put("doc", "ORDIN PLATA");
                    gv.put("dataDoc", LocalDateUtils.toLocalDate(f61.date()));
                    gv.put("name", description);
                    gv.put("total", f61.amount());
                    gv.put("contBancarId", contBancarId);
                }
            } else if (f61.getDebitCreditMark().equalsIgnoreCase("C")) {
                // IF line contains 'Depunere numerar' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.INCASARE | doc='CHITANTA' | date=line.date | doc.name=INC | doc.total=line.total | banca=contBancarId
                // ELSE IF line starts with 'AK' AND contains 'POS'
                //   THEN doc.gestiune=gestiuneId | Partner='CARD INCASARE' | TipDoc.INCASARE | doc='CARD' | docNr='NC' | date=line.date | doc.name='INC' | doc.total=line.total | banca=contBancarId
                //   OR doc.gestiune=gestiuneId | Partner not 'CARD INCASARE' or 'RAIFF...' | TipDoc.INCASARE | doc='CARD' | date=line.date |  doc.total=line.total | banca=contBancarId
                if (StringUtils.globalIsMatch(details, "Depunere numerar", StringUtils.TextFilterMethod.CONTAINS)) {
                    gv.put("gestiuneId", gestiuneId);
                    gv.put("gestiuneName", gestiune.getName());
                    gv.put("partnerName", "RAIFF " + gestiune.getName().toUpperCase());
                    gv.put("tipDoc", Document.TipDoc.INCASARE);
                    gv.put("doc", "CHITANTA");
                    gv.put("dataDoc", LocalDateUtils.toLocalDate(f61.date()));
                    gv.put("name", "INC");
                    gv.put("total", f61.amount());
                    gv.put("contBancarId", contBancarId);
                } else if (details.startsWith("AK") && details.contains("POS")) {
                    gv.put("gestiuneId", gestiuneId);
                    gv.put("gestiuneName", gestiune.getName());
                    gv.put("partnerName", "CARD INCASARE");
                    gv.put("tipDoc", Document.TipDoc.INCASARE);
                    gv.put("doc", "CARD");
                    gv.put("nrDoc", "NC");
                    gv.put("dataDoc", LocalDateUtils.toLocalDate(f61.date()));
                    gv.put("name", "INC");
                    gv.put("total", f61.amount());
                    gv.put("contBancarId", contBancarId);
                } else { // unknown case; probably some bank transfer
                    gv.put("gestiuneId", gestiuneId);
                    gv.put("partnerName", partner);
                    gv.put("tipDoc", Document.TipDoc.INCASARE);
                    gv.put("dataDoc", LocalDateUtils.toLocalDate(f61.date()));
                    gv.put("name", details);
                    gv.put("total", f61.amount());
                    gv.put("contBancarId", contBancarId);
                }
            }
            records.add(new NormalizedRecord(f61.getLines()+PresentationUtils.NEWLINE+f86.getLines(), gv));
        }

        return records.stream();
    }
}
