package ro.flexbiz.billing.service

import com.google.common.collect.ImmutableList
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import ro.colibri.beans.ManagerBean
import ro.colibri.beans.ManagerBeanRemote
import ro.colibri.beans.VanzariBean
import ro.colibri.beans.VanzariBeanRemote
import ro.colibri.embeddable.Address
import ro.colibri.embeddable.Delegat
import ro.colibri.entities.comercial.*
import ro.colibri.entities.comercial.mappings.AccountingDocumentMapping
import ro.colibri.entities.user.Company
import ro.colibri.entities.user.User
import ro.colibri.legacy.service.ServiceLocator
import ro.flexbiz.billing.pojo.Invoice
import spock.lang.Shared
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BillingServiceTests extends Specification {
    @Shared
    ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def setup() {
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
        ec.artifactExecution.enableAuthz()
    }

    def "whenGetInvoice_thenTransformAccDocToInvoice"() {
        given:
        final AccountingDocument accDoc = new AccountingDocument(null, 1L,
                null, null, null, null, null, null, null, null, null, null, null, false, false, null, null, null, null, false, false, false, 0, null, null, null, null, null, null, false)
        final Company company = new Company()
        company.setId(1)
        accDoc.setCompany(company)
        accDoc.setDataDoc(LocalDateTime.of(2023, 10, 17, 13, 13))
        accDoc.setDoc(AccountingDocument.FACTURA_NAME)
        accDoc.setNrDoc("101")
        accDoc.setScadenta(LocalDate.of(2023, 11, 16))
        accDoc.setTipDoc(Document.TipDoc.VANZARE)

        final Gestiune linicGest = new Gestiune()
        linicGest.setImportName("L1")
        linicGest.setName("Linic")
        accDoc.setGestiune(linicGest)

        final User danut = new User()
        danut.setEmail("danut@yahoo.com")
        danut.setCnp("1xxx12254")
        danut.setName("Groza Danut")
        danut.setPhone("0712354789")
        accDoc.setOperator(danut)

        final Partner customer = new Partner()
        customer.setEmail("client@yahoo.com")
        customer.setName("CLIENT INC RO")
        customer.setPhone("0745613215")
        final Address customerAddress = new Address()
        customerAddress.setCountry("RO")
        customerAddress.setJudet("RO-BH")
        customerAddress.setNr("415300")
        customerAddress.setOras("Chet")
        customerAddress.setStrada("Principala")
        customer.setAddress(customerAddress)
        customer.setCodFiscal("12345678")
        customer.setRegCom("J40/1251/2023")
        final Delegat delegat = new Delegat()
        delegat.setName("Client")
        customer.setDelegat(delegat)
        accDoc.setPartner(customer)

        final Operatiune op = new Operatiune()
        op.setId(1L)
        op.setBarcode("59")
        op.setCantitate(new BigDecimal("100"))
        op.setName("MATERIALE 40KG")
        op.setPretVanzareUnitarCuTVA(new BigDecimal("45.22"))
        op.setValoareVanzareFaraTVA(new BigDecimal("3800"))
        op.setValoareVanzareTVA(new BigDecimal("722"))
        op.setUom("BUC")
        accDoc.getOperatiuni().add(op)

        accDoc.getPaidBy().add(new AccountingDocumentMapping(null, null, null, new BigDecimal("1000")))

        ServiceLocator.clearCache()
        def vanzariBean = Mock(VanzariBeanRemote)
        vanzariBean.accountingDocumentById(1L, true) >> accDoc
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(VanzariBean.class, VanzariBeanRemote.class),
                vanzariBean)

        def managerBean = Mock(ManagerBeanRemote)
        managerBean.allPersistedProps() >> ImmutableList.of(
                new PersistedProp().setKey(PersistedProp.FIRMA_NAME_KEY).setValue("SC LINIC SRL"),
                new PersistedProp().setKey(PersistedProp.FIRMA_CUI_KEY).setValue("RO14998343"),
                new PersistedProp().setKey(PersistedProp.FIRMA_REG_COM_KEY).setValue("J05/1111/2002"),
                new PersistedProp().setKey(PersistedProp.FIRMA_MAIN_BANK_ACC_KEY).setValue("RO48 BTRL 0050 1202 K652 77XX - RON"),
                new PersistedProp().setKey("firma_billing_primary_line").setValue("Str Principala nr 218A"),
                new PersistedProp().setKey("firma_billing_city").setValue("MARGINE"),
                new PersistedProp().setKey("firma_billing_cod_judet").setValue("RO-BH"))
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(ManagerBean.class, ManagerBeanRemote.class),
                managerBean)

        when:
        final Invoice invoice = ec.service.sync().name("BillingServices.get#Invoice")
                .parameter("invoiceId", 1L)
                .call().invoice

        then:
        invoice.getId() == 1
        invoice.getAllowanceCharges() == null
        invoice.getAllowanceTotalAmount() == null
        invoice.getChargeTotalAmount() == null
        invoice.getDocumentCurrencyCode() == "RON"
        invoice.getDueDate() == LocalDateTime.of(2023, 11, 16, 0, 0).atZone(ZoneId.of("Europe/Bucharest")).toInstant()
        invoice.getInvoiceNumber() == "L1-101"
        invoice.getIssueDate() == LocalDateTime.of(2023, 10, 17, 13, 13).atZone(ZoneId.of("Europe/Bucharest")).toInstant()
        invoice.getLineExtensionAmount() == new BigDecimal("3800")
        invoice.getNote() == null
        invoice.getPayableAmount() == new BigDecimal("3522")
        invoice.getPayableRoundingAmount() == null
        invoice.getPaymentMeansCode() == "30"
        invoice.getPaymentId() == "FF_101/2023-10-17"
        invoice.getPayeeFinancialAccount().getFinancialInstitutionBranch() == null
        invoice.getPayeeFinancialAccount().getCurrency() == null
        invoice.getPayeeFinancialAccount().getId() == "RO48BTRL00501202K65277XX-RON"
        invoice.getPayeeFinancialAccount().getName() == "SC LINIC SRL"
        invoice.getPrepaidAmount() == new BigDecimal("1000")
        invoice.getTaxAmount() == new BigDecimal("722")
        invoice.getTaxCurrencyCode() == "RON"
        invoice.getTaxExclusiveAmount() == new BigDecimal("3800")
        invoice.getTaxInclusiveAmount() == new BigDecimal("4522")
        invoice.getTaxSubtotals().size() == 1
        invoice.getTaxSubtotals().get(0).getTaxableAmount() == new BigDecimal("3800")
        invoice.getTaxSubtotals().get(0).getTaxAmount() == new BigDecimal("722")
        invoice.getTaxSubtotals().get(0).getTaxCategory().getCode() == "S"
        invoice.getTaxSubtotals().get(0).getTaxCategory().getPercent() == new BigDecimal("0.19")
        invoice.getTaxSubtotals().get(0).getTaxCategory().getTaxExemptionReason() == null
        invoice.getTaxSubtotals().get(0).getTaxCategory().getTaxScheme() == "VAT"
        invoice.getPayeeParty() == null

        invoice.getAccountingSupplier().getBusinessName() == null
        invoice.getAccountingSupplier().getContactName() == "Groza Danut"
        invoice.getAccountingSupplier().getElectronicMail() == ""
        invoice.getAccountingSupplier().getPostalAddress().getCity() == "MARGINE"
        invoice.getAccountingSupplier().getPostalAddress().getCountry() == "RO"
        invoice.getAccountingSupplier().getPostalAddress().getCountrySubentity() == "RO-BH"
        invoice.getAccountingSupplier().getPostalAddress().getPostalZone() == null
        invoice.getAccountingSupplier().getPostalAddress().getPrimaryLine() == "Str Principala nr 218A"
        invoice.getAccountingSupplier().getRegistrationName() == "SC LINIC SRL"
        invoice.getAccountingSupplier().getRegistrationId() == "J05/1111/2002"
        invoice.getAccountingSupplier().getCompanyLegalForm() == "Capital social "
        invoice.getAccountingSupplier().getTaxId() == "RO14998343"
        invoice.getAccountingSupplier().getTelephone() == ""

        invoice.getAccountingCustomer().getBusinessName() == null
        invoice.getAccountingCustomer().getContactName() == "Client"
        invoice.getAccountingCustomer().getElectronicMail() == "client@yahoo.com"
        invoice.getAccountingCustomer().getPostalAddress().getCity() == "Chet"
        invoice.getAccountingCustomer().getPostalAddress().getCountry() == "RO"
        invoice.getAccountingCustomer().getPostalAddress().getCountrySubentity() == "RO-BH"
        invoice.getAccountingCustomer().getPostalAddress().getPostalZone() == "415300"
        invoice.getAccountingCustomer().getPostalAddress().getPrimaryLine() == "Principala"
        invoice.getAccountingCustomer().getRegistrationName() == "CLIENT INC RO"
        invoice.getAccountingCustomer().getRegistrationId() == "J40/1251/2023"
        invoice.getAccountingCustomer().getCompanyLegalForm() == null
        invoice.getAccountingCustomer().getTelephone() == "0745613215"
        invoice.getAccountingCustomer().getTaxId() == "12345678"

        invoice.getLines().size() == 1
        invoice.getLines().get(0).getAllowanceCharges() == null
        invoice.getLines().get(0).getBuyersItemIdentification() == null
        invoice.getLines().get(0).getClassifiedTaxCategory().getCode() == "S"
        invoice.getLines().get(0).getClassifiedTaxCategory().getPercent() == new BigDecimal("0.19")
        invoice.getLines().get(0).getClassifiedTaxCategory().getTaxExemptionReason() == null
        invoice.getLines().get(0).getClassifiedTaxCategory().getTaxScheme() == "VAT"
        invoice.getLines().get(0).getId() == 1L
        invoice.getLines().get(0).getLineExtensionAmount() == new BigDecimal("3800")
        invoice.getLines().get(0).getName() == "MATERIALE 40KG"
        invoice.getLines().get(0).getNote() == null
        invoice.getLines().get(0).getPrice() == new BigDecimal("38")
        invoice.getLines().get(0).getBaseQuantity() == new BigDecimal("1")
        invoice.getLines().get(0).getQuantity() == new BigDecimal("100")
        invoice.getLines().get(0).getSellersItemIdentification() == "59"
        invoice.getLines().get(0).getTaxAmount() == new BigDecimal("722")
        invoice.getLines().get(0).getUom() == "C62"
    }

    def "whenAmountIs05AndTva01_thenTvaPercentShouldBe21"() {
        given:
        final AccountingDocument accDoc = new AccountingDocument(null, 1L,
                null, null, null, null, null, null, null, null, null, null, null, false, false, null, null, null, null, false, false, false, 0, null, null, null, null, null, null, false)
        final Company company = new Company()
        company.setId(1)
        accDoc.setCompany(company)
        accDoc.setTipDoc(Document.TipDoc.VANZARE)
        accDoc.setDoc(AccountingDocument.FACTURA_NAME)
        final Operatiune op = new Operatiune()
        op.setPretVanzareUnitarCuTVA(new BigDecimal("0.6"))
        op.setValoareVanzareFaraTVA(new BigDecimal("0.5"))
        op.setValoareVanzareTVA(new BigDecimal("0.1"))
        op.setCantitate(BigDecimal.ONE)
        accDoc.getOperatiuni().add(op)

        ServiceLocator.clearCache()
        def vanzariBean = Mock(VanzariBeanRemote)
        vanzariBean.accountingDocumentById(1L, true) >> accDoc
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(VanzariBean.class, VanzariBeanRemote.class),
                vanzariBean)

        def managerBean = Mock(ManagerBeanRemote)
        managerBean.allPersistedProps() >> ImmutableList.of()
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(ManagerBean.class, ManagerBeanRemote.class),
                managerBean)

        when:
        final Invoice invoice = ec.service.sync().name("BillingServices.get#Invoice")
                .parameter("invoiceId", 1L)
                .call().invoice

        then:
        invoice.getId() == 1
        invoice.getLineExtensionAmount() == new BigDecimal("0.5")
        invoice.getPayableAmount() == new BigDecimal("0.6")
        invoice.getTaxAmount() == new BigDecimal("0.1")
        invoice.getTaxExclusiveAmount() == new BigDecimal("0.5")
        invoice.getTaxInclusiveAmount() == new BigDecimal("0.6")
        invoice.getTaxSubtotals().size() == 1
        invoice.getTaxSubtotals().get(0).getTaxableAmount() == new BigDecimal("0.5")
        invoice.getTaxSubtotals().get(0).getTaxAmount() == new BigDecimal("0.1")
        invoice.getTaxSubtotals().get(0).getTaxCategory().getCode() == "S"
        invoice.getTaxSubtotals().get(0).getTaxCategory().getPercent() == new BigDecimal("0.21")
        invoice.getTaxSubtotals().get(0).getTaxCategory().getTaxExemptionReason() == null
        invoice.getTaxSubtotals().get(0).getTaxCategory().getTaxScheme() == "VAT"

        invoice.getLines().size() == 1
        invoice.getLines().get(0).getClassifiedTaxCategory().getCode() == "S"
        invoice.getLines().get(0).getClassifiedTaxCategory().getPercent() == new BigDecimal("0.21")
        invoice.getLines().get(0).getClassifiedTaxCategory().getTaxExemptionReason() == null
        invoice.getLines().get(0).getClassifiedTaxCategory().getTaxScheme() == "VAT"
        invoice.getLines().get(0).getLineExtensionAmount() == new BigDecimal("0.5")
        invoice.getLines().get(0).getPrice() == new BigDecimal("0.5")
        invoice.getLines().get(0).getQuantity() == new BigDecimal("1")
        invoice.getLines().get(0).getTaxAmount() == new BigDecimal("0.1")
    }

    def "whenAccDocNotFound_thenThrowException"() {
        given:
        ServiceLocator.clearCache()
        def vanzariBean = Mock(VanzariBeanRemote)
        vanzariBean.accountingDocumentById(1L, true) >> null
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(VanzariBean.class, VanzariBeanRemote.class),
                vanzariBean)

        def managerBean = Mock(ManagerBeanRemote)
        managerBean.allPersistedProps() >> ImmutableList.of()
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(ManagerBean.class, ManagerBeanRemote.class),
                managerBean)

        when:
        final Invoice invoice = ec.service.sync().name("BillingServices.get#Invoice")
                .parameter("invoiceId", 1L)
                .call().invoice

        then:
        invoice == null
        ec.message.errorsString.contains("Cannot invoke method getTipDoc() on null object")
    }

    def "whenAccDocIsNotFactura_thenThrowException"() {
        given:
        final AccountingDocument accDoc = new AccountingDocument(null, 1L,
                null, null, null, null, null, null, null, null, null, null, null, false, false, null, null, null, null, false, false, false, 0, null, null, null, null, null, null, false)
        final Company company = new Company()
        company.setId(1)
        accDoc.setCompany(company)
        accDoc.setTipDoc(Document.TipDoc.VANZARE)
        accDoc.setDoc(AccountingDocument.AVIZ_NAME)

        ServiceLocator.clearCache()
        def vanzariBean = Mock(VanzariBeanRemote)
        vanzariBean.accountingDocumentById(1L, true) >> accDoc
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(VanzariBean.class, VanzariBeanRemote.class),
                vanzariBean)

        def managerBean = Mock(ManagerBeanRemote)
        managerBean.allPersistedProps() >> ImmutableList.of()
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(ManagerBean.class, ManagerBeanRemote.class),
                managerBean)

        when:
        final Invoice invoice = ec.service.sync().name("BillingServices.get#Invoice")
                .parameter("invoiceId", 1L)
                .call().invoice

        then:
        invoice == null
        ec.message.errorsString.contains("Documentul trebuie sa fie FACTURA!")
    }

    def "whenTipDocIsNotVanzare_thenThrowException"() {
        given:
        final AccountingDocument accDoc = new AccountingDocument(null, 1L,
                null, null, null, null, null, null, null, null, null, null, null, false, false, null, null, null, null, false, false, false, 0, null, null, null, null, null, null, false)
        final Company company = new Company()
        company.setId(1)
        accDoc.setCompany(company)
        accDoc.setDoc(AccountingDocument.FACTURA_NAME)
        accDoc.setTipDoc(Document.TipDoc.CUMPARARE)

        ServiceLocator.clearCache()
        def vanzariBean = Mock(VanzariBeanRemote)
        vanzariBean.accountingDocumentById(1L, true) >> accDoc
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(VanzariBean.class, VanzariBeanRemote.class),
                vanzariBean)

        def managerBean = Mock(ManagerBeanRemote)
        managerBean.allPersistedProps() >> ImmutableList.of()
        ServiceLocator.SERVICES_CACHE.put(ServiceLocator.jndiNameStateless(ManagerBean.class, ManagerBeanRemote.class),
                managerBean)

        when:
        final Invoice invoice = ec.service.sync().name("BillingServices.get#Invoice")
                .parameter("invoiceId", 1L)
                .call().invoice

        then:
        invoice == null
        ec.message.errorsString.contains("TipDoc trebuie sa fie VANZARE!")
    }
}
