package ro.colibri.legacy.reconciliation.receipts

import ro.flexbiz.util.commons.NumberUtils
import ro.flexbiz.util.commons.reconciliation.ReconciliationAnalyzer
import ro.flexbiz.util.commons.reconciliation.model.Discrepancy
import ro.flexbiz.util.commons.reconciliation.model.DiscrepancyType
import ro.flexbiz.util.commons.reconciliation.model.IdentityMatch
import ro.flexbiz.util.commons.reconciliation.model.ReconciliationResult
import ro.flexbiz.util.commons.reconciliation.model.ReconciliationStatus

class ReceiptAnalyzer implements ReconciliationAnalyzer {
    @Override
    ReconciliationResult analyze(IdentityMatch im) {
        List<Discrepancy> discrepancies = []
        BigDecimal totalLeft = im.left.stream()
                .map {it.fields.getBigDecimal("total")}
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
        BigDecimal totalRight = im.right.stream()
                .map {it.fields.getBigDecimal("total")}
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
        ReconciliationStatus status = NumberUtils.equal(totalLeft, totalRight) ? ReconciliationStatus.RECONCILED :
                ReconciliationStatus.MISMATCH

        if (!NumberUtils.equal(totalLeft, totalRight))
            discrepancies.add(new Discrepancy("total", NumberUtils.subtract(totalLeft, totalRight), DiscrepancyType.DIFFERENT))

        return new ReconciliationResult(im,  status, discrepancies)
    }
}
