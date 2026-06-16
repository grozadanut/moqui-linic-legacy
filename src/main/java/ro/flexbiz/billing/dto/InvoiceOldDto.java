package ro.flexbiz.billing.dto;

import ro.colibri.entities.comercial.AccountingDocument;

import java.util.Objects;

public class InvoiceOldDto {
    final private AccountingDocument accDoc;
    final private String seriaFactura;
    final private String firmaName;
    final private String firmaCui;
    final private String firmaRegCom;
    final private String firmaCapSocial;
    final private String firmaBillingAddressStreet;
    final private String firmaBillingAddressCity;
    final private String firmaBillingAddressCodJudet;
    final private String firmaPhone;
    final private String firmaEmail;
    final private String firmaIban;

    public InvoiceOldDto(AccountingDocument accDoc, String seriaFactura, String firmaName, String firmaCui, String firmaRegCom, String firmaCapSocial, String firmaBillingAddressStreet, String firmaBillingAddressCity, String firmaBillingAddressCodJudet, String firmaPhone, String firmaEmail, String firmaIban) {
        this.accDoc = accDoc;
        this.seriaFactura = seriaFactura;
        this.firmaName = firmaName;
        this.firmaCui = firmaCui;
        this.firmaRegCom = firmaRegCom;
        this.firmaCapSocial = firmaCapSocial;
        this.firmaBillingAddressStreet = firmaBillingAddressStreet;
        this.firmaBillingAddressCity = firmaBillingAddressCity;
        this.firmaBillingAddressCodJudet = firmaBillingAddressCodJudet;
        this.firmaPhone = firmaPhone;
        this.firmaEmail = firmaEmail;
        this.firmaIban = firmaIban;
    }

    public AccountingDocument getAccDoc() {
        return accDoc;
    }

    public String getSeriaFactura() {
        return seriaFactura;
    }

    public String getFirmaName() {
        return firmaName;
    }

    public String getFirmaCui() {
        return firmaCui;
    }

    public String getFirmaRegCom() {
        return firmaRegCom;
    }

    public String getFirmaCapSocial() {
        return firmaCapSocial;
    }

    public String getFirmaBillingAddressStreet() {
        return firmaBillingAddressStreet;
    }

    public String getFirmaBillingAddressCity() {
        return firmaBillingAddressCity;
    }

    public String getFirmaBillingAddressCodJudet() {
        return firmaBillingAddressCodJudet;
    }

    public String getFirmaPhone() {
        return firmaPhone;
    }

    public String getFirmaEmail() {
        return firmaEmail;
    }

    public String getFirmaIban() {
        return firmaIban;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InvoiceOldDto that = (InvoiceOldDto) o;
        return Objects.equals(accDoc, that.accDoc) && Objects.equals(seriaFactura, that.seriaFactura) && Objects.equals(firmaName, that.firmaName) && Objects.equals(firmaCui, that.firmaCui) && Objects.equals(firmaRegCom, that.firmaRegCom) && Objects.equals(firmaCapSocial, that.firmaCapSocial) && Objects.equals(firmaBillingAddressStreet, that.firmaBillingAddressStreet) && Objects.equals(firmaBillingAddressCity, that.firmaBillingAddressCity) && Objects.equals(firmaBillingAddressCodJudet, that.firmaBillingAddressCodJudet) && Objects.equals(firmaPhone, that.firmaPhone) && Objects.equals(firmaEmail, that.firmaEmail) && Objects.equals(firmaIban, that.firmaIban);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accDoc, seriaFactura, firmaName, firmaCui, firmaRegCom, firmaCapSocial, firmaBillingAddressStreet, firmaBillingAddressCity, firmaBillingAddressCodJudet, firmaPhone, firmaEmail, firmaIban);
    }

    @Override
    public String toString() {
        return "InvoiceOldDto{" +
                "accDoc=" + accDoc +
                ", seriaFactura='" + seriaFactura + '\'' +
                ", firmaName='" + firmaName + '\'' +
                ", firmaCui='" + firmaCui + '\'' +
                ", firmaRegCom='" + firmaRegCom + '\'' +
                ", firmaCapSocial='" + firmaCapSocial + '\'' +
                ", firmaBillingAddressStreet='" + firmaBillingAddressStreet + '\'' +
                ", firmaBillingAddressCity='" + firmaBillingAddressCity + '\'' +
                ", firmaBillingAddressCodJudet='" + firmaBillingAddressCodJudet + '\'' +
                ", firmaPhone='" + firmaPhone + '\'' +
                ", firmaEmail='" + firmaEmail + '\'' +
                ", firmaIban='" + firmaIban + '\'' +
                '}';
    }
}
