package ro.flexbiz.billing.mapper.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.user.User;
import ro.flexbiz.billing.dto.InvoiceOldDto;
import ro.flexbiz.billing.mapper.InvoiceMapper;
import ro.flexbiz.billing.pojo.*;

//@Generated(
//        value = "org.mapstruct.ap.MappingProcessor",
//        date = "2026-05-29T14:08:28+0300",
//        comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.39.0.v20240820-0604, environment: Java 21.0.4 (Eclipse Adoptium)"
//)
public class InvoiceMapperImpl implements InvoiceMapper {

    @Override
    public Invoice toInvoice(InvoiceOldDto invOld) {
        if ( invOld == null ) {
            return null;
        }

        Invoice invoice = new Invoice();

        if ( invOld.getAccDoc() != null ) {
            if ( invoice.getAccountingSupplier() == null ) {
                invoice.setAccountingSupplier( new Party() );
            }
            accountingDocumentToParty( invOld.getAccDoc(), invoice.getAccountingSupplier() );
        }
        if ( invoice.getAccountingSupplier() == null ) {
            invoice.setAccountingSupplier( new Party() );
        }
        invoiceOldDtoToParty( invOld, invoice.getAccountingSupplier() );
        invoice.setAccountingCustomer( accountingDocumentToParty1( invOld.getAccDoc() ) );
        invoice.setPayeeFinancialAccount( invoiceOldDtoToFinancialAccount( invOld ) );
        invoice.setId( invOldAccDocId( invOld ) );
        invoice.setInvoiceNumber( getInvoiceNumber( invOld ) );
        invoice.setIssueDate( map( invOldAccDocDataDoc( invOld ) ) );
        invoice.setDueDate( map( invOldAccDocScadenta( invOld ) ) );
        invoice.setPaymentId( mapPaymentId( invOld.getAccDoc() ) );
        invoice.setTaxAmount( invOldAccDocTotalTva( invOld ) );
        invoice.setTaxSubtotals( getTaxSubtotals( invOld.getAccDoc() ) );
        invoice.setLineExtensionAmount( invOldAccDocVanzareTotalFaraTva( invOld ) );
        invoice.setTaxExclusiveAmount( invOldAccDocVanzareTotalFaraTva( invOld ) );
        invoice.setTaxInclusiveAmount( invOldAccDocTotal( invOld ) );
        invoice.setPrepaidAmount( invOldAccDocTotalLinked( invOld ) );
        invoice.setPayableAmount( invOldAccDocTotalUnlinked( invOld ) );
        Set<Operatiune> operatiuni = invOldAccDocOperatiuni( invOld );
        invoice.setLines( operatiuneSetToInvoiceLineList( operatiuni ) );

        invoice.setDocumentCurrencyCode( "RON" );
        invoice.setPaymentMeansCode( "30" );
        invoice.setTaxCurrencyCode( "RON" );

        return invoice;
    }

    @Override
    public InvoiceLine toInvoiceLine(Operatiune op) {
        if ( op == null ) {
            return null;
        }

        InvoiceLine invoiceLine = new InvoiceLine();

        invoiceLine.setSellersItemIdentification( op.getBarcode() );
        invoiceLine.setQuantity( op.getCantitate() );
        invoiceLine.setUom(OperatiuneUtil.getUomInternational(op) );
        invoiceLine.setClassifiedTaxCategory( operatiuneToTaxCategory( op ) );
        invoiceLine.setPrice( OperatiuneUtil.getPretVanzareUnitarFaraTVA(op) );
        invoiceLine.setLineExtensionAmount( op.getValoareVanzareFaraTVA() );
        invoiceLine.setTaxAmount( op.getValoareVanzareTVA() );
        invoiceLine.setId( op.getId() );
        invoiceLine.setName( op.getName() );

        invoiceLine.setBaseQuantity( new BigDecimal( "1" ) );

        return invoiceLine;
    }

    @Override
    public TaxCategory operatiuneToTaxCategory(Operatiune op) {
        if ( op == null ) {
            return null;
        }

        TaxCategory taxCategory = new TaxCategory();

        taxCategory.setPercent( OperatiuneUtil.getVanzareTvaPercentCalculated(op) );

        taxCategory.setCode( "S" );
        taxCategory.setTaxScheme( "VAT" );

        return taxCategory;
    }

    private String accountingDocumentOperatorName(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        User operator = accountingDocument.getOperator();
        if ( operator == null ) {
            return null;
        }
        String name = operator.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    protected void accountingDocumentToAddress(AccountingDocument accountingDocument, Address mappingTarget) {
        if ( accountingDocument == null ) {
            return;
        }

        mappingTarget.setCountry( "RO" );
    }

    protected void accountingDocumentToParty(AccountingDocument accountingDocument, Party mappingTarget) {
        if ( accountingDocument == null ) {
            return;
        }

        mappingTarget.setContactName( accountingDocumentOperatorName( accountingDocument ) );
        if ( mappingTarget.getPostalAddress() == null ) {
            mappingTarget.setPostalAddress( new Address() );
        }
        accountingDocumentToAddress( accountingDocument, mappingTarget.getPostalAddress() );
    }

    protected void invoiceOldDtoToAddress(InvoiceOldDto invoiceOldDto, Address mappingTarget) {
        if ( invoiceOldDto == null ) {
            return;
        }

        mappingTarget.setCountrySubentity( invoiceOldDto.getFirmaBillingAddressCodJudet() );
        mappingTarget.setCity( invoiceOldDto.getFirmaBillingAddressCity() );
        mappingTarget.setPrimaryLine( invoiceOldDto.getFirmaBillingAddressStreet() );
    }

    protected void invoiceOldDtoToParty(InvoiceOldDto invoiceOldDto, Party mappingTarget) {
        if ( invoiceOldDto == null ) {
            return;
        }

        if ( mappingTarget.getPostalAddress() == null ) {
            mappingTarget.setPostalAddress( new Address() );
        }
        invoiceOldDtoToAddress( invoiceOldDto, mappingTarget.getPostalAddress() );
        mappingTarget.setTaxId( invoiceOldDto.getFirmaCui() );
        mappingTarget.setRegistrationName( invoiceOldDto.getFirmaName() );
        mappingTarget.setRegistrationId( invoiceOldDto.getFirmaRegCom() );
        mappingTarget.setCompanyLegalForm( displayCapSocial( invoiceOldDto.getFirmaCapSocial() ) );
        mappingTarget.setTelephone( invoiceOldDto.getFirmaPhone() );
        mappingTarget.setElectronicMail( invoiceOldDto.getFirmaEmail() );
    }

    private String accountingDocumentPartnerCodFiscal(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        String codFiscal = partner.getCodFiscal();
        if ( codFiscal == null ) {
            return null;
        }
        return codFiscal;
    }

    private String accountingDocumentPartnerName(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        String name = partner.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String accountingDocumentPartnerRegCom(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        String regCom = partner.getRegCom();
        if ( regCom == null ) {
            return null;
        }
        return regCom;
    }

    private ro.colibri.embeddable.Address accountingDocumentPartnerAddress(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        ro.colibri.embeddable.Address address = partner.getAddress();
        if ( address == null ) {
            return null;
        }
        return address;
    }

    private String accountingDocumentPartnerDelegatName(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        Delegat delegat = partner.getDelegat();
        if ( delegat == null ) {
            return null;
        }
        String name = delegat.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String accountingDocumentPartnerPhone(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        String phone = partner.getPhone();
        if ( phone == null ) {
            return null;
        }
        return phone;
    }

    private String accountingDocumentPartnerEmail(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }
        Partner partner = accountingDocument.getPartner();
        if ( partner == null ) {
            return null;
        }
        String email = partner.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    protected Party accountingDocumentToParty1(AccountingDocument accountingDocument) {
        if ( accountingDocument == null ) {
            return null;
        }

        Party party = new Party();

        party.setTaxId( accountingDocumentPartnerCodFiscal( accountingDocument ) );
        party.setRegistrationName( accountingDocumentPartnerName( accountingDocument ) );
        party.setRegistrationId( accountingDocumentPartnerRegCom( accountingDocument ) );
        party.setPostalAddress( extractAddress( accountingDocumentPartnerAddress( accountingDocument ) ) );
        party.setContactName( accountingDocumentPartnerDelegatName( accountingDocument ) );
        party.setTelephone( accountingDocumentPartnerPhone( accountingDocument ) );
        party.setElectronicMail( accountingDocumentPartnerEmail( accountingDocument ) );

        return party;
    }

    protected FinancialAccount invoiceOldDtoToFinancialAccount(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }

        FinancialAccount financialAccount = new FinancialAccount();

        financialAccount.setId( invoiceOldDto.getFirmaIban() );
        financialAccount.setName( invoiceOldDto.getFirmaName() );

        return financialAccount;
    }

    private Long invOldAccDocId(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        Long id = accDoc.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private LocalDateTime invOldAccDocDataDoc(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        LocalDateTime dataDoc = accDoc.getDataDoc();
        if ( dataDoc == null ) {
            return null;
        }
        return dataDoc;
    }

    private LocalDate invOldAccDocScadenta(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        LocalDate scadenta = accDoc.getScadenta();
        if ( scadenta == null ) {
            return null;
        }
        return scadenta;
    }

    private BigDecimal invOldAccDocTotalTva(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        BigDecimal totalTva = accDoc.getTotalTva();
        if ( totalTva == null ) {
            return null;
        }
        return totalTva;
    }

    private BigDecimal invOldAccDocVanzareTotalFaraTva(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        BigDecimal vanzareTotalFaraTva = AccDocUtil.getVanzareTotalFaraTva(accDoc);
        if ( vanzareTotalFaraTva == null ) {
            return null;
        }
        return vanzareTotalFaraTva;
    }

    private BigDecimal invOldAccDocTotal(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        BigDecimal total = accDoc.getTotal();
        if ( total == null ) {
            return null;
        }
        return total;
    }

    private BigDecimal invOldAccDocTotalLinked(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        BigDecimal totalLinked = AccDocUtil.getTotalLinked(accDoc);
        if ( totalLinked == null ) {
            return null;
        }
        return totalLinked;
    }

    private BigDecimal invOldAccDocTotalUnlinked(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        BigDecimal totalUnlinked = AccDocUtil.getTotalUnlinked(accDoc);
        if ( totalUnlinked == null ) {
            return null;
        }
        return totalUnlinked;
    }

    private Set<Operatiune> invOldAccDocOperatiuni(InvoiceOldDto invoiceOldDto) {
        if ( invoiceOldDto == null ) {
            return null;
        }
        AccountingDocument accDoc = invoiceOldDto.getAccDoc();
        if ( accDoc == null ) {
            return null;
        }
        Set<Operatiune> operatiuni = accDoc.getOperatiuni();
        if ( operatiuni == null ) {
            return null;
        }
        return operatiuni;
    }

    protected List<InvoiceLine> operatiuneSetToInvoiceLineList(Set<Operatiune> set) {
        if ( set == null ) {
            return null;
        }

        List<InvoiceLine> list = new ArrayList<InvoiceLine>( set.size() );
        for ( Operatiune operatiune : set ) {
            list.add( toInvoiceLine( operatiune ) );
        }

        return list;
    }
}
