package ro.flexbiz.billing.mapper.impl;

import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import static ro.flexbiz.util.commons.NumberUtils.*;
import static ro.flexbiz.util.commons.StringUtils.isEmpty;

public class OperatiuneUtil {
    public static List<BigDecimal> VAT_RATES_RO = List.of(new BigDecimal("0.21"), new BigDecimal("0.11"),
            new BigDecimal("0.19"), new BigDecimal("0.09"), new BigDecimal("0.05"), new BigDecimal("0"));

    public static String getUomInternational(Operatiune op) {
        String uom = op.getUom();
        if (isEmpty(uom))
            return "C62"; //C62=unitate

        if (uom.trim().equalsIgnoreCase("BUC"))
            return "C62"; //C62=unitate
        if (uom.trim().equalsIgnoreCase("KG"))
            return "KGM"; //KGM=kilogram
        if (uom.trim().equalsIgnoreCase("ML") || uom.trim().equalsIgnoreCase("M"))
            return "LM"; //LM=metru liniar
        if (uom.trim().equalsIgnoreCase("MP") || uom.trim().equalsIgnoreCase("M2"))
            return "MTK"; //MTK=metru patrat
        if (uom.trim().equalsIgnoreCase("KM"))
            return "KMT"; //KMT=kilometru
        if (uom.trim().equalsIgnoreCase("PER") || uom.trim().equalsIgnoreCase("SET"))
            return "SET"; //SET=set
        if (uom.trim().equalsIgnoreCase("PLACA"))
            return "XPG"; //XPG=placa

        return "C62"; //C62=unitate
    }

    public static BigDecimal getPretVanzareUnitarFaraTVA(Operatiune op) {
        final BigDecimal tvaPercent = getVanzareTvaPercentCalculated(op);
        final BigDecimal tvaExtractDivisor = add(tvaPercent, BigDecimal.ONE);
        final BigDecimal tvaUnitar = AccountingDocument.extractTvaAmount(op.getPretVanzareUnitarCuTVA(), tvaExtractDivisor);
        return subtract(op.getPretVanzareUnitarCuTVA(), tvaUnitar);
    }

    /**
     * If the fields are already completed, calculates the VAT amount based on sale value.
     * IMPORTANT: as the VAT is calculated based on sale value, this will NOT work for ops
     * that only have acquisition value(eg.: materie prima)
     */
    public static BigDecimal getVanzareTvaPercentCalculated(Operatiune op)
    {
        if (equal(op.getValoareVanzareFaraTVA(), BigDecimal.ZERO))
            return BigDecimal.ZERO;
        return findClosest(VAT_RATES_RO, divide(op.getValoareVanzareTVA(), op.getValoareVanzareFaraTVA(), 2, RoundingMode.HALF_EVEN).abs());
    }

    /**
     * Returns the value in the list that is closest to specific input value.
     */
    public static BigDecimal findClosest(final List<BigDecimal> list, final BigDecimal value)
    {
        return list.stream()
                .min(Comparator.comparing(a -> value.subtract(a).abs()))
                .orElse(BigDecimal.ZERO);
    }
}
