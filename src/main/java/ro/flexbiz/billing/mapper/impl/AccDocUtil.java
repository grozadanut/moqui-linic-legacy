package ro.flexbiz.billing.mapper.impl;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.mappings.AccountingDocumentMapping;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Predicate;

public class AccDocUtil {
    public static BigDecimal getTotalLinked(AccountingDocument accDoc) {
        return totalLinked(accDoc, o -> true);
    }

    public static BigDecimal totalLinked(AccountingDocument accDoc, final Predicate<AccountingDocumentMapping> filter) {
        switch (accDoc.getTipDoc()) {
            case CUMPARARE:
            case VANZARE:
                return accDoc.getPaidBy().stream()
                        .filter(filter)
                        .map(AccountingDocumentMapping::getTotal)
                        .reduce(java.math.BigDecimal::add)
                        .orElse(BigDecimal.ZERO);

            case PLATA:
            case INCASARE:
                return accDoc.getPaidDocs().stream()
                        .filter(filter)
                        .map(AccountingDocumentMapping::getTotal)
                        .reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO);

            default:
                throw new UnsupportedOperationException(
                        "Tip doc " + accDoc.getTipDoc() + " not supported for calculating total unlinked!");
        }
    }

    /**
     * In case we have operations, we don't use the total field;
     * We calculate the total by adding the sum of all the operations
     */
    public static BigDecimal getVanzareTotalFaraTva(AccountingDocument accDoc) {
        return accDoc.getOperatiuni().stream()
                .map(Operatiune::getValoareVanzareFaraTVA)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    public static BigDecimal getTotalUnlinked(AccountingDocument accDoc) {
        return totalUnlinked(accDoc, o -> true);
    }

    public static BigDecimal totalUnlinked(AccountingDocument accDoc, final Predicate<AccountingDocumentMapping> filter) {
        return accDoc.getTotal().subtract(totalLinked(accDoc, filter));
    }
}
