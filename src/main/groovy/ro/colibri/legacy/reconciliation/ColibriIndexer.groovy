package ro.colibri.legacy.reconciliation

import ro.colibri.entities.comercial.Document
import ro.flexbiz.util.commons.PresentationUtils
import ro.flexbiz.util.commons.StringUtils
import ro.flexbiz.util.commons.reconciliation.Indexer
import ro.flexbiz.util.commons.reconciliation.model.Index
import ro.flexbiz.util.commons.reconciliation.model.NormalizedRecord

class ColibriIndexer implements Indexer {
    @Override
    Set<Index> index(NormalizedRecord nr) {
        Set indexes = new HashSet()
        if (StringUtils.globalIsMatch(nr.getFields().get("type"), "balance", StringUtils.TextFilterMethod.EQUALS))
            indexes.add(new Index("balance", nr.getFields().get("type")))

        // DEBIT/PLATA
        // IF line starts with ignore case 'COMIS' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc=CARD | docNr=NC | date=closingDate | doc.name=COM | doc.total=sum(lines) | banca=contBancarId
        // ELSE IF line.partner = 'LINIC SRL' THEN doc.gestiune=L1 | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc='ORDIN PLATA' | docNr=OP | date=line.date | doc.name=line.description | doc.total=line.total | banca=contBancarId
        // ELSE Partner=line.partner | TipDoc.PLATA | doc='ORDIN PLATA' | date=line.date | doc.total=line.total | banca=contBancarId
        if (nr.fields.tipDoc == Document.TipDoc.PLATA) {
            indexes.add(new Index("commissions", "${nr.fields.gestiuneId}|${nr.fields.partnerName}|${nr.fields.doc}|${nr.fields.name}|${nr.fields.contBancarId}"))
            if ("Linic".equalsIgnoreCase(nr.fields.get("gestiuneName")))
                indexes.add(new Index("transfersToLinic", "${nr.fields.partnerName}|${nr.fields.doc}|${nr.fields.contBancarId}|${PresentationUtils.displayBigDecimal(nr.fields.total)}"))
            indexes.add(new Index("otherOutgPayments", "${nr.fields.partnerName}|${nr.fields.doc}|${nr.fields.contBancarId}|${PresentationUtils.displayBigDecimal(nr.fields.total)}"))
        }
        // CREDIT/INCASARE
        // IF line contains 'Depunere numerar' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.INCASARE | doc='CHITANTA' | date=line.date | doc.name=INC | doc.total=line.total | banca=contBancarId
        // ELSE IF line starts with 'AK' AND contains 'POS'
        //   THEN doc.gestiune=gestiuneId | Partner='CARD INCASARE' | TipDoc.INCASARE | doc='CARD' | docNr='NC' | date=line.date | doc.name='INC' | doc.total=line.total | banca=contBancarId
        //   OR doc.gestiune=gestiuneId | Partner not 'CARD INCASARE' or 'RAIFF...' | TipDoc.INCASARE | doc='CARD' | date=line.date |  doc.total=line.total | banca=contBancarId
        else if (nr.fields.tipDoc == Document.TipDoc.INCASARE) {
            indexes.add(new Index("deposits", "${nr.fields.gestiuneId}|${nr.fields.partnerName}|${nr.fields.doc}|${nr.fields.name}|${nr.fields.contBancarId}|${PresentationUtils.displayBigDecimal(nr.fields.total)}"))
            indexes.add(new Index("posTotals", "${nr.fields.gestiuneId}|${nr.fields.doc}|${nr.fields.contBancarId}|${PresentationUtils.displayBigDecimal(nr.fields.total)}"))
            indexes.add(new Index("pos", "${nr.fields.gestiuneId}|${nr.fields.doc}|${nr.fields.contBancarId}"))
            indexes.add(new Index("otherIncPayments", "${nr.fields.partnerName}|${nr.fields.contBancarId}|${PresentationUtils.displayBigDecimal(nr.fields.total)}"))
        }
        indexes.add(new Index("byTotals", "${nr.fields.tipDoc}|${nr.fields.contBancarId}|${PresentationUtils.displayBigDecimal(nr.fields.total)}"))

        return indexes
    }
}
