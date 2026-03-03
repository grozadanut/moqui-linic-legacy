package ro.colibri.legacy.reconciliation

import groovy.transform.CompileStatic
import ro.flexbiz.util.commons.LocalDateUtils
import ro.flexbiz.util.commons.NumberUtils
import ro.flexbiz.util.commons.StringUtils
import ro.flexbiz.util.commons.reconciliation.IdentityMatcher
import ro.flexbiz.util.commons.reconciliation.model.*

import java.time.LocalDate
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
class RowMatcher implements IdentityMatcher {
    @Override
    Stream<IdentityMatch> score(Index index, List<NormalizedRecord> left, List<NormalizedRecord> right) {
        switch (index.strategy) {
            case "balance":
                return Stream.of(new IdentityMatch(left, right, BigDecimal.ONE, IdentityStatus.CONFIRMED,
                        List.of(new MatchSignal("type", BigDecimal.ONE, "balance"))))
            // DEBIT/PLATA
            case "commissions":
                return matchCommisions(left, right)
            case "transfersToLinic":
                return matchTransfers(left, right)
            case "otherOutgPayments":
                return matchOutgPayments(left, right)
            // CREDIT/INCASARE
            case "deposits":
                return matchDeposits(left, right)
            case "posTotals":
                return matchPosTotals(left, right)
            case "pos":
                return matchPos(left, right)
            case "otherIncPayments":
                if (!left.isEmpty() && !right.isEmpty())
                    return Stream.of(new IdentityMatch(left, right, new BigDecimal("0.7"), IdentityStatus.CONFIRMED,
                            List.of(new MatchSignal("otherIncPayments", BigDecimal.ONE, "matched by TipDoc.INCASARE, partnerName, and total"))))
            // DEBIT OR CREDIT
            case "byTotals":
                return Stream.of(new IdentityMatch(left, right, new BigDecimal("0.5"), IdentityStatus.PROBABLE,
                        List.of(new MatchSignal("byTotals", BigDecimal.ONE, "matched by TipDoc and total"))))
        }
        return Stream.of()
    }

    def Stream<IdentityMatch> matchCommisions(List<NormalizedRecord> left, List<NormalizedRecord> right) {
        // IF line starts with ignore case 'COMIS' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc=CARD | docNr=NC | date=closingDate | doc.name=COM | doc.total=sum(lines) | banca=contBancarId
        List<NormalizedRecord> leftMatch = left.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.BEGINS_WITH)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "CARD", StringUtils.TextFilterMethod.EQUALS)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("name"), "COM", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())
        List<NormalizedRecord> rightMatch = right.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.BEGINS_WITH)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "CARD", StringUtils.TextFilterMethod.EQUALS)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("name"), "COM", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())

        if (!leftMatch.isEmpty() || !rightMatch.isEmpty())
             return Stream.of(new IdentityMatch(leftMatch, rightMatch, new BigDecimal("0.95"), IdentityStatus.CONFIRMED,
                     List.of(new MatchSignal("commissions", BigDecimal.ONE, "matched by TipDoc.PLATA, RAIFF [COLIBRI/LINIC], 'CARD' and 'COM'"))))
        return Stream.of()
    }

    def Stream<IdentityMatch> matchTransfers(List<NormalizedRecord> left, List<NormalizedRecord> right) {
        // ELSE IF line.partner = 'LINIC SRL' THEN doc.gestiune=L1 | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc='ORDIN PLATA' | docNr=OP | date=line.date | doc.name=line.description | doc.total=line.total | banca=contBancarId
        List<NormalizedRecord> leftMatch = left.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.BEGINS_WITH)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "ORDIN PLATA", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())
        List<NormalizedRecord> rightMatch = right.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.BEGINS_WITH)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "ORDIN PLATA", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())

        if (!leftMatch.isEmpty() || !rightMatch.isEmpty())
            return Stream.of(new IdentityMatch(leftMatch, rightMatch, new BigDecimal("0.9"), IdentityStatus.CONFIRMED,
                    List.of(new MatchSignal("transfersToLinic", BigDecimal.ONE, "matched by TipDoc.PLATA, RAIFF [COLIBRI/LINIC], 'ORDIN PLATA' and total"))))
        return Stream.of()
    }

    def Stream<IdentityMatch> matchOutgPayments(List<NormalizedRecord> left, List<NormalizedRecord> right) {
        // ELSE Partner=line.partner | TipDoc.PLATA | doc='ORDIN PLATA' | date=line.date | doc.total=line.total | banca=contBancarId
        List<NormalizedRecord> leftMatch = left.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "ORDIN PLATA", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())
        List<NormalizedRecord> rightMatch = right.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "ORDIN PLATA", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())

        if (!leftMatch.isEmpty() && !rightMatch.isEmpty())
            return Stream.of(new IdentityMatch(leftMatch, rightMatch, new BigDecimal("0.85"), IdentityStatus.CONFIRMED,
                    List.of(new MatchSignal("otherOutgPayments", BigDecimal.ONE, "matched by TipDoc.PLATA, partnerName, 'ORDIN PLATA' and total"))))
        return Stream.of()
    }

    def Stream<IdentityMatch> matchDeposits(List<NormalizedRecord> left, List<NormalizedRecord> right) {
        // IF line contains 'Depunere numerar' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.INCASARE | doc='CHITANTA' | date=line.date | doc.name=INC | doc.total=line.total | banca=contBancarId
        List<NormalizedRecord> leftMatch = left.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.BEGINS_WITH)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "CHITANTA", StringUtils.TextFilterMethod.EQUALS)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("name"), "INC", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())
        List<NormalizedRecord> rightMatch = right.stream()
                .filter { StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.BEGINS_WITH)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("doc"), "CHITANTA", StringUtils.TextFilterMethod.EQUALS)}
                .filter { StringUtils.globalIsMatch(it.fields.getString("name"), "INC", StringUtils.TextFilterMethod.EQUALS)}
                .collect(Collectors.toList())

        if (!leftMatch.isEmpty() || !rightMatch.isEmpty())
            return Stream.of(new IdentityMatch(leftMatch, rightMatch, new BigDecimal("0.8"), IdentityStatus.CONFIRMED,
                    List.of(new MatchSignal("deposits", BigDecimal.ONE, "matched by TipDoc.INCASARE, RAIFF [COLIBRI/LINIC], 'CHITANTA', 'INC' and total"))))
        return Stream.of()
    }

    def Stream<IdentityMatch> matchPosTotals(List<NormalizedRecord> left, List<NormalizedRecord> right) {
        // ELSE IF line starts with 'AK' AND contains 'POS'
        //   THEN doc.gestiune=gestiuneId | Partner='CARD INCASARE' | TipDoc.INCASARE | doc='CARD' | docNr='NC' | date=line.date | doc.name='INC' | doc.total=line.total | banca=contBancarId
        //   OR doc.gestiune=gestiuneId | Partner not 'CARD INCASARE' or 'RAIFF...' | TipDoc.INCASARE | doc='CARD' | date=line.date |  doc.total=line.total | banca=contBancarId
        Predicate<NormalizedRecord> paysEcrReceipt = (NormalizedRecord it) ->
                StringUtils.globalIsMatch(it.fields.getString("partnerName"), "CARD INCASARE", StringUtils.TextFilterMethod.EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("doc"), "CARD", StringUtils.TextFilterMethod.EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("nrDoc"), "NC", StringUtils.TextFilterMethod.EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("name"), "INC", StringUtils.TextFilterMethod.EQUALS)
        Predicate<NormalizedRecord> paysInvoice = (NormalizedRecord it) ->
                StringUtils.globalIsMatch(it.fields.getString("partnerName"), "CARD INCASARE", StringUtils.TextFilterMethod.NOT_EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.NOT_BEGINS_WITH) &&
                        StringUtils.globalIsMatch(it.fields.getString("doc"), "CARD", StringUtils.TextFilterMethod.EQUALS)

        List<NormalizedRecord> leftMatch = left.stream()
                .filter {paysEcrReceipt.test(it)} // NOTE: on the bank statement ECR receipts and invoices are not separated
                .collect(Collectors.toList())
        List<NormalizedRecord> rightMatch = right.stream()
                .filter {paysEcrReceipt.test(it) || paysInvoice.test(it)}
                .collect(Collectors.toList())

        if (!leftMatch.isEmpty() && !rightMatch.isEmpty())
            return Stream.of(new IdentityMatch(leftMatch, rightMatch, new BigDecimal("0.75"), IdentityStatus.CONFIRMED,
                    List.of(new MatchSignal("posTotals", BigDecimal.ONE,
                            "matched by TipDoc.INCASARE, [CARD INCASARE, 'CARD', 'NC', 'INC'] OR [not CARD INCASARE and not begins with RAIFF, 'CARD'] and total"))))
        return Stream.of()
    }

    def Stream<IdentityMatch> matchPos(List<NormalizedRecord> left, List<NormalizedRecord> right) {
        // ELSE IF line starts with 'AK' AND contains 'POS'
        //   THEN doc.gestiune=gestiuneId | Partner='CARD INCASARE' | TipDoc.INCASARE | doc='CARD' | docNr='NC' | date=line.date | doc.name='INC' | doc.total=line.total | banca=contBancarId
        //   OR doc.gestiune=gestiuneId | Partner not 'CARD INCASARE' or 'RAIFF...' | TipDoc.INCASARE | doc='CARD' | date=line.date |  doc.total=line.total | banca=contBancarId
        Predicate<NormalizedRecord> paysEcrReceipt = (NormalizedRecord it) ->
                StringUtils.globalIsMatch(it.fields.getString("partnerName"), "CARD INCASARE", StringUtils.TextFilterMethod.EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("doc"), "CARD", StringUtils.TextFilterMethod.EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("nrDoc"), "NC", StringUtils.TextFilterMethod.EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("name"), "INC", StringUtils.TextFilterMethod.EQUALS)
        Predicate<NormalizedRecord> paysInvoice = (NormalizedRecord it) ->
                StringUtils.globalIsMatch(it.fields.getString("partnerName"), "CARD INCASARE", StringUtils.TextFilterMethod.NOT_EQUALS) &&
                        StringUtils.globalIsMatch(it.fields.getString("partnerName"), "RAIFF", StringUtils.TextFilterMethod.NOT_BEGINS_WITH) &&
                        StringUtils.globalIsMatch(it.fields.getString("doc"), "CARD", StringUtils.TextFilterMethod.EQUALS)

        List<NormalizedRecord> leftMatch = left.stream()
                .filter {paysEcrReceipt.test(it)} // NOTE: on the bank statement ECR receipts and invoices are not separated
                .sorted(Comparator.<NormalizedRecord, LocalDate>comparing {it.fields.getLocalDate("dataDoc")})
                .collect(Collectors.toList())
        List<NormalizedRecord> rightMatch = right.stream()
                .filter {paysEcrReceipt.test(it) || paysInvoice.test(it)}
                .sorted(Comparator.<NormalizedRecord, LocalDate>comparing {it.fields.getLocalDate("dataDoc")})
                .collect(Collectors.toList())

        return leftMatch.stream()
                .map {l ->
                    List<NormalizedRecord> beforeLeft = rightMatch.stream()
                            .filter { LocalDateUtils.isBefore(it.fields.getLocalDate("dataDoc"), l.fields.getLocalDate("dataDoc"))}
                            .sorted(Comparator.<NormalizedRecord, LocalDate>comparing {it.fields.getLocalDate("dataDoc")}.reversed())
                            .collect(Collectors.toList())
                    def beforeLeftIt = beforeLeft.iterator()
                    List<NormalizedRecord> foundRight = []
                    while (beforeLeftIt.hasNext()) {
                        foundRight.add(beforeLeftIt.next())

                        if (NumberUtils.equal(l.fields.getBigDecimal("total"),
                                foundRight.stream().map {it.fields.getBigDecimal("total")}.reduce(BigDecimal::add).orElse(BigDecimal.ZERO))) {
                            rightMatch.removeAll(foundRight)
                            return new IdentityMatch(List.of(l), foundRight, new BigDecimal("0.74"), IdentityStatus.PROBABLE,
                                    List.of(new MatchSignal("pos", BigDecimal.ONE,
                                            "matched by TipDoc.INCASARE, [CARD INCASARE, 'CARD', 'NC', 'INC'] OR [not CARD INCASARE and not begins with RAIFF, 'CARD'] and tried to match left.total with sum(right)")))
                        }
                    }
                    return null
                }
                .filter(Objects::nonNull)
    }
}
