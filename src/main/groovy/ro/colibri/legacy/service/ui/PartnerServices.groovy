package ro.colibri.legacy.service.ui

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import ro.flexbiz.util.commons.StringUtils

import java.sql.Timestamp
import java.time.LocalDateTime

class PartnerServices {
    static Map<String, Object> storeLegacyPayment(ExecutionContext ec) {
        EntityValue payment = ec.entity.makeValue("mantle.account.payment.Payment")
        payment.set("paymentId", ec.context.paymentId)
        payment.set("paymentTypeEnumId", "PtInvoicePayment")
        payment.set("fromPartyId", ec.context.fromPartyId)
        payment.set("toPartyId", ec.context.toPartyId)
        payment.set("statusId", "PmntDelivered")
        payment.set("effectiveDate", ec.user.nowTimestamp)
        payment.set("amount", ec.context.amount)
        payment.createOrUpdate()

        if (ec.context.affiliatePartnerId) {
            EntityValue paymentParty = ec.entity.makeValue("mantle.account.payment.PaymentParty")
            paymentParty.set("paymentId", ec.context.paymentId)
            paymentParty.set("partyId", ec.context.affiliatePartnerId)
            paymentParty.set("roleTypeId", "Affiliate")
            paymentParty.createOrUpdate()
        }

        return [paymentId: ec.context.paymentId]
    }
    static Map<String, Object> createColibriPartner(ExecutionContext ec) {
        String partyId = ec.context.partyId as String
        String partnerCode = ec.context.partnerCode as String
        Boolean isPerson = ec.context.isPerson as Boolean
        String phone = ec.context.phone as String

        // Party
        final EntityValue party = ec.getEntity().makeValue("Party")
        party.set("partyId", partyId)
        party.set("partyTypeEnumId", isPerson ? "PtyPerson" : "PtyOrganization")
        party.create()

        if (isPerson) {
            final EntityValue pg = ec.getEntity().makeValue("Person")
            pg.set("partyId", partyId)
            pg.set("firstName", ec.context.name)
            pg.create()
        } else {
            final EntityValue pg = ec.getEntity().makeValue("Organization")
            pg.set("partyId", partyId)
            pg.set("organizationName", ec.context.name)
            pg.create()
        }

        // Phone
        if (!StringUtils.isEmpty(phone)) {
            String contactMechId = partyId + "_PHONE"
            EntityValue contactMech = ec.getEntity().makeValue("mantle.party.contact.ContactMech")
            contactMech.set("contactMechId", contactMechId)
            contactMech.set("contactMechTypeEnumId", "CmtTelecomNumber")
            contactMech.createOrUpdate()

            EntityValue telecomNumber = ec.getEntity().makeValue("mantle.party.contact.TelecomNumber")
            telecomNumber.set("contactMechId", contactMechId)
            telecomNumber.set("contactNumber", phone)
            telecomNumber.createOrUpdate()

            EntityValue partyContactMech = ec.getEntity().makeValue("mantle.party.contact.PartyContactMech")
            partyContactMech.set("partyId", partyId)
            partyContactMech.set("contactMechId", contactMechId)
            partyContactMech.set("contactMechPurposeId", "PhonePrimary")
            partyContactMech.set("fromDate", ec.user.nowTimestamp)
            partyContactMech.createOrUpdate()
        }

        // mantle.party.PartyRole
        EntityValue partyRole = ec.entity.makeValue("mantle.party.PartyRole")
        partyRole.set("partyId", partyId)
        partyRole.set("roleTypeId", "Affiliate")
        partyRole.create()

        // mantle.party.PartyIdentification
        EntityValue partyIdentification = ec.entity.makeValue("mantle.party.PartyIdentification")
        partyIdentification.set("partyId", partyId)
        partyIdentification.set("partyIdTypeEnumId", "PtidAffiliateId")
        partyIdentification.set("idValue", partnerCode)
        partyIdentification.create()

        // mantle.account.financial.FinancialAccount
        EntityValue financialAccount = ec.entity.makeValue("mantle.account.financial.FinancialAccount")
        financialAccount.set("finAccountId", partyId)
        financialAccount.set("finAccountTypeId", "ServiceCredit")
        financialAccount.set("statusId", "FaActive")
        financialAccount.set("organizationPartyId", "L2")
        financialAccount.set("ownerPartyId", partyId)
        financialAccount.create()

        // mantle.party.agreement.AgreementParty
        EntityValue agreementParty = ec.entity.makeValue("mantle.party.agreement.AgreementParty")
        agreementParty.set("agreementId", "PartenerColibri")
        agreementParty.set("partyId", partyId)
        agreementParty.set("roleTypeId", "Affiliate")
        agreementParty.create()

        return [partyId: partyId]
    }
}
