package ro.colibri.legacy.reconciliation

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import ro.colibri.entities.comercial.AccountingDocument
import ro.colibri.entities.comercial.ContBancar
import ro.colibri.entities.comercial.Document
import ro.colibri.util.InvocationResult
import ro.flexbiz.util.commons.model.GenericValue
import ro.flexbiz.util.commons.reconciliation.Normalizer
import ro.flexbiz.util.commons.reconciliation.model.NormalizedRecord

import java.time.LocalDate
import java.util.stream.Stream

import static ro.colibri.util.ListUtils.toImmutableList

class AccDocNormalizer implements Normalizer {
    @Override
    Stream<NormalizedRecord> normalize(Object o) {
        InvocationResult result = (InvocationResult) o;
        List<NormalizedRecord> records = new ArrayList<>();
        final Map<LocalDate, List<AccountingDocument>> accDocsByDate = result.extra(InvocationResult.ACCT_DOC_KEY);
        final ImmutableMap<LocalDate, BigDecimal> solduriInitiale = result.extra(InvocationResult.SOLD_INITIAL_KEY);
        ImmutableList<AccountingDocument> accDocs = accDocsByDate.values().stream()
                .flatMap(List::stream)
                .collect(toImmutableList());

        final LocalDate openingDate = solduriInitiale.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(LocalDate.now());
        final LocalDate closingDate = solduriInitiale.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<LocalDate, BigDecimal> e) -> e.getKey()).reversed())
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(LocalDate.now());
        final BigDecimal soldInitial = solduriInitiale.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(BigDecimal.ZERO);
        final BigDecimal subtotalPlata = accDocs.stream()
                .filter(accDoc -> Document.TipDoc.PLATA.equals(accDoc.getTipDoc()))
                .map(AccountingDocument::getTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        final BigDecimal subtotalIncasare = accDocs.stream()
                .filter(accDoc -> Document.TipDoc.INCASARE.equals(accDoc.getTipDoc()))
                .map(AccountingDocument::getTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        BigDecimal soldFinal = soldInitial.add(subtotalIncasare).subtract(subtotalPlata);
        GenericValue balance = GenericValue.of();
        balance.put("type", "balance");
        balance.put("openingBalance", soldInitial);
        balance.put("closingBalance", soldFinal);
        balance.put("openingDate", openingDate);
        balance.put("closingDate", closingDate);
        records.add(new NormalizedRecord(null, balance));

        accDocs.forEach(accDoc -> {
            GenericValue fields = GenericValue.of();
            fields.put("gestiuneId", accDoc.getGestiune().getId());
            fields.put("gestiuneName", accDoc.getGestiune().getName());
            fields.put("partnerName", accDoc.getPartner().getName());
            fields.put("tipDoc", accDoc.getTipDoc());
            fields.put("doc", accDoc.getDoc());
            fields.put("nrDoc", accDoc.getNrDoc());
            fields.put("dataDoc", accDoc.getDataDoc_toLocalDate());
            fields.put("name", accDoc.getName());
            fields.put("total", accDoc.getTotal());
            fields.put("contBancarId", Optional.ofNullable(accDoc.getContBancar()).map(ContBancar::getId).orElse(null));
            records.add(new NormalizedRecord(accDoc.getId(), fields));
        });

        return records.stream();
    }
}
