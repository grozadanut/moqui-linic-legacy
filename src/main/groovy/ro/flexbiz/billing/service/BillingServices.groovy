package ro.flexbiz.billing.service

import com.google.common.collect.ImmutableList
import org.moqui.context.ExecutionContext
import org.moqui.service.ServiceException
import ro.colibri.entities.comercial.AccountingDocument
import ro.colibri.entities.comercial.Document
import ro.colibri.entities.comercial.PersistedProp
import ro.colibri.legacy.service.LegacySyncServices
import ro.flexbiz.billing.dto.InvoiceOldDto
import ro.flexbiz.billing.mapper.InvoiceMapper

class BillingServices {
    static Map<String, Object> getInvoice(ExecutionContext ec) {
        long invoiceId = ec.context.invoiceId
        final AccountingDocument accDoc = LegacySyncServices.findById(invoiceId)

        if (Document.TipDoc.VANZARE != accDoc.getTipDoc())
            throw new ServiceException("TipDoc trebuie sa fie VANZARE!")
        if (!AccountingDocument.FACTURA_NAME.equalsIgnoreCase(accDoc.getDoc()))
            throw new ServiceException("Documentul trebuie sa fie FACTURA!")

        final ImmutableList<PersistedProp> props = LegacySyncServices.allPersistedProps_NO_CACHE()
        final String seriaFactura = findOrDefault(props, PersistedProp.SERIA_FACTURA_KEY, "")
        final String firmaName = findOrDefault(props, PersistedProp.FIRMA_NAME_KEY, "")
        final String firmaCui = findOrDefault(props, PersistedProp.FIRMA_CUI_KEY, "")
        final String firmaRegCom = findOrDefault(props, PersistedProp.FIRMA_REG_COM_KEY, "")
        final String firmaCapSocial = findOrDefault(props, PersistedProp.FIRMA_CAP_SOCIAL_KEY, "")
        final String firmaPhone = findOrDefault(props, PersistedProp.FIRMA_PHONE_KEY, "")
        final String firmaEmail = findOrDefault(props, PersistedProp.FIRMA_EMAIL_KEY, "")
        final String firmaIban = findOrDefault(props, PersistedProp.FIRMA_MAIN_BANK_ACC_KEY, "")
                .replaceAll("\\s+","") // remove whitespace
        final String firmaBillingAddressStreet = findOrDefault(props, "firma_billing_primary_line", "")
        final String firmaBillingAddressCity = findOrDefault(props, "firma_billing_city", "")
        final String firmaBillingAddressCodJudet = findOrDefault(props, "firma_billing_cod_judet", "")

        final InvoiceOldDto invOld = new InvoiceOldDto(accDoc, seriaFactura, firmaName, firmaCui, firmaRegCom, firmaCapSocial,
                firmaBillingAddressStreet, firmaBillingAddressCity, firmaBillingAddressCodJudet, firmaPhone, firmaEmail, firmaIban)

        return [invoice: InvoiceMapper.INSTANCE.toInvoice(invOld)]
    }

    private static String findOrDefault(final List<PersistedProp> props, final String key, final String defaultValue) {
        return props.stream().filter(prop -> key.equalsIgnoreCase(prop.getKey()))
                .findFirst()
                .map(PersistedProp::getValue)
                .orElse(defaultValue)
    }
}
