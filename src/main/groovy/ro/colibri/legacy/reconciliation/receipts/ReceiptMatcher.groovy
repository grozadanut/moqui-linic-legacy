package ro.colibri.legacy.reconciliation.receipts

import ro.flexbiz.util.commons.reconciliation.IdentityMatcher
import ro.flexbiz.util.commons.reconciliation.model.IdentityMatch
import ro.flexbiz.util.commons.reconciliation.model.IdentityStatus
import ro.flexbiz.util.commons.reconciliation.model.Index
import ro.flexbiz.util.commons.reconciliation.model.MatchSignal
import ro.flexbiz.util.commons.reconciliation.model.NormalizedRecord

import java.util.stream.Stream

class ReceiptMatcher implements IdentityMatcher {
    @Override
    Stream<IdentityMatch> score(Index index, List<NormalizedRecord> left, List<NormalizedRecord> right) {
        if (left.isEmpty() || right.isEmpty())
            return Stream.of()

        switch (index.strategy) {
            case "type":
                if (index.value == "Z")
                    return Stream.of(new IdentityMatch(left, right, BigDecimal.ONE, IdentityStatus.CONFIRMED,
                            List.of(new MatchSignal("type", BigDecimal.ONE, "Z total line"))))

                if (index.value == "DISC")
                    return Stream.of(new IdentityMatch(left, right, new BigDecimal("0.95"), IdentityStatus.PROBABLE,
                            List.of(new MatchSignal("type", BigDecimal.ONE, "Type matches"))))
                return Stream.of()
            case "typeTotal":
                if (index.value == "DISC")
                    return Stream.of(new IdentityMatch(left, right, BigDecimal.ONE, IdentityStatus.CONFIRMED,
                            List.of(new MatchSignal("typeTotal", BigDecimal.ONE, "Type and Total matches"))))
                return Stream.of()
            case "nameQuantity":
                return Stream.of(new IdentityMatch(left, right, BigDecimal.ONE, IdentityStatus.CONFIRMED,
                        List.of(new MatchSignal("nameQuantity", BigDecimal.ONE, "Name and Quantity matches"))))
            case "name":
                return Stream.of(new IdentityMatch(left, right, new BigDecimal("0.9"), IdentityStatus.PROBABLE,
                        List.of(new MatchSignal("name", BigDecimal.ONE, "Name matches"))))
        }
        return Stream.of()
    }
}