package ro.flexbiz.billing.mapper;

import ro.colibri.embeddable.Address;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.flexbiz.billing.dto.InvoiceOldDto;
import ro.flexbiz.billing.mapper.impl.InvoiceMapperImpl;
import ro.flexbiz.billing.mapper.impl.OperatiuneUtil;
import ro.flexbiz.billing.pojo.Invoice;
import ro.flexbiz.billing.pojo.InvoiceLine;
import ro.flexbiz.billing.pojo.TaxCategory;
import ro.flexbiz.billing.pojo.TaxSubtotal;
import ro.flexbiz.util.commons.LocalDateUtils;
import ro.flexbiz.util.commons.NumberUtils;
import ro.flexbiz.util.commons.PresentationUtils;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static ro.flexbiz.util.commons.PresentationUtils.safeString;

public interface InvoiceMapper {
    InvoiceMapper INSTANCE = new InvoiceMapperImpl();

    Invoice toInvoice(InvoiceOldDto invOld);
    InvoiceLine toInvoiceLine(Operatiune op);
    TaxCategory operatiuneToTaxCategory(Operatiune op);
    default ro.flexbiz.billing.pojo.Address extractAddress(final Address address) {
        if (address == null)
            return null;

        final ro.flexbiz.billing.pojo.Address target = new ro.flexbiz.billing.pojo.Address();
        target.setCountry(PresentationUtils.safeString(address.getCountry(), "RO"));
        target.setCountrySubentity(address.getJudet());
        target.setCity(address.getOras());
        target.setPrimaryLine(address.getStrada());
        target.setPostalZone(address.getNr());
        return target;
    }
    default String getInvoiceNumber(final InvoiceOldDto invOld) {
        return safeString(invOld.getSeriaFactura()) + safeString(invOld.getAccDoc().getGestiune(), Gestiune::getImportName) + "-" +
                safeString(invOld.getAccDoc().getNrDoc());
    }
    default List<TaxSubtotal> getTaxSubtotals(final AccountingDocument accDoc) {
        if (accDoc == null)
            return List.of();

        return accDoc.getOperatiuni_Stream()
                .collect(Collectors.groupingBy(op -> OperatiuneUtil.getVanzareTvaPercentCalculated(op)))
                .entrySet().stream()
                .map(vatToOps ->
                {
                    final TaxSubtotal taxSubtotal = new TaxSubtotal();
                    taxSubtotal.setTaxableAmount(vatToOps.getValue().stream()
                            .map(Operatiune::getValoareVanzareFaraTVA)
                            .reduce(BigDecimal::add)
                            .orElse(BigDecimal.ZERO));
                    taxSubtotal.setTaxAmount(vatToOps.getValue().stream()
                            .map(Operatiune::getValoareVanzareTVA)
                            .reduce(BigDecimal::add)
                            .orElse(BigDecimal.ZERO));
                    final TaxCategory taxCategory = new TaxCategory();
                    taxCategory.setCode( NumberUtils.equal(vatToOps.getKey(), BigDecimal.ZERO) ? "Z" : "S" );
                    taxCategory.setPercent(vatToOps.getKey());
                    taxCategory.setTaxScheme("VAT");
                    taxSubtotal.setTaxCategory(taxCategory);
                    return taxSubtotal;
                })
                .collect(Collectors.toList());
    }
    default String displayCapSocial(final String firmaCapSocial) {
        return "Capital social "+firmaCapSocial;
    }
    default String mapPaymentId(final AccountingDocument accDoc) {
        return MessageFormat.format("FF_{0}/{1}",
                safeString(accDoc, AccountingDocument::getNrDoc),
                safeString(accDoc, AccountingDocument::getDataDoc,
                        dataDoc -> LocalDateUtils.displayLocalDateTime(dataDoc, LocalDateUtils.DATE_FORMATTER)));
    }

    default Instant map(final LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.of("Europe/Bucharest")).toInstant();
    }
    default Instant map(final LocalDate value) {
        return value == null ? null : value.atStartOfDay().atZone(ZoneId.of("Europe/Bucharest")).toInstant();
    }
}
