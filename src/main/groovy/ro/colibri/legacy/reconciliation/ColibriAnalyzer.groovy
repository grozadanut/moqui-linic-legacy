package ro.colibri.legacy.reconciliation

import ro.flexbiz.util.commons.ListUtils
import ro.flexbiz.util.commons.LocalDateUtils
import ro.flexbiz.util.commons.NumberUtils
import ro.flexbiz.util.commons.reconciliation.ReconciliationAnalyzer
import ro.flexbiz.util.commons.reconciliation.model.*

class ColibriAnalyzer implements ReconciliationAnalyzer {
    @Override
    ReconciliationResult analyze(IdentityMatch im) {
        if (ListUtils.notEmpty(im.getLeft()) && ListUtils.isEmpty(im.getRight()))
            return new ReconciliationResult(im, ReconciliationStatus.LEFT_ONLY, List.of());
        else if (ListUtils.isEmpty(im.getLeft()) && ListUtils.notEmpty(im.getRight()))
            return new ReconciliationResult(im, ReconciliationStatus.RIGHT_ONLY, List.of());

        if (im.status == IdentityStatus.CONFIRMED) {
            if (im.signals[0]?.fieldKey == "type")
                return reconcileBalances(im)
            else
                return reconcileTotals(im)
        } else if (im.status == IdentityStatus.PROBABLE) {
            return reconcileTotals(im)
        }
        return new ReconciliationResult(im, ReconciliationStatus.MISMATCH, List.of())
    }

    ReconciliationResult reconcileBalances(IdentityMatch im) {
        ReconciliationStatus status = ReconciliationStatus.RECONCILED
        List<Discrepancy> discrepancies = []
        if (!NumberUtils.equal(im.left[0].fields.openingBalance, im.right[0].fields.openingBalance)) {
            status = ReconciliationStatus.MISMATCH
            discrepancies.add(new Discrepancy("openingBalance", NumberUtils.subtract(im.left[0].fields.openingBalance, im.right[0].fields.openingBalance), DiscrepancyType.DIFFERENT))
        }
        if (!NumberUtils.equal(im.left[0].fields.closingBalance, im.right[0].fields.closingBalance)) {
            status = ReconciliationStatus.MISMATCH
            discrepancies.add(new Discrepancy("closingBalance", NumberUtils.subtract(im.left[0].fields.closingBalance, im.right[0].fields.closingBalance), DiscrepancyType.DIFFERENT))
        }
        if (!LocalDateUtils.equal(im.left[0].fields.openingDate, im.right[0].fields.openingDate))
            discrepancies.add(new Discrepancy("openingDate", LocalDateUtils.compare(im.left[0].fields.openingDate, im.right[0].fields.openingDate), DiscrepancyType.TOLERATED))
        if (!LocalDateUtils.equal(im.left[0].fields.closingDate, im.right[0].fields.closingDate))
            discrepancies.add(new Discrepancy("closingDate", LocalDateUtils.compare(im.left[0].fields.closingDate, im.right[0].fields.closingDate), DiscrepancyType.TOLERATED))
        return new ReconciliationResult(im, status, discrepancies)
    }

    ReconciliationResult reconcileTotals(IdentityMatch im) {
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
