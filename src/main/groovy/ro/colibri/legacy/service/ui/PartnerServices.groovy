package ro.colibri.legacy.service.ui

import com.google.common.collect.ImmutableList
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFind
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import ro.colibri.entities.comercial.AccountingDocument
import ro.colibri.legacy.service.LegacySyncServices
import ro.colibri.util.InvocationResult
import ro.colibri.wrappers.RulajPartener
import ro.flexbiz.util.commons.NumberUtils
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

        if (StringUtils.notEmpty(ec.context.affiliatePartnerId)) {
            EntityValue paymentParty = ec.entity.makeValue("mantle.account.payment.PaymentParty")
            paymentParty.set("paymentId", ec.context.paymentId)
            paymentParty.set("partyId", ec.context.affiliatePartnerId)
            paymentParty.set("roleTypeId", "Affiliate")
            paymentParty.createOrUpdate()
        }

        ec.service.async().name("UIServices.check#PartnerAgreements").call()
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
        partyIdentification.set("idValue", partnerCode.toUpperCase())
        partyIdentification.create()

        // mantle.account.financial.FinancialAccount
        EntityValue financialAccount = ec.entity.makeValue("mantle.account.financial.FinancialAccount")
        financialAccount.set("finAccountId", partyId)
        financialAccount.set("finAccountTypeId", "ServiceCredit")
        financialAccount.set("statusId", "FaActive")
        financialAccount.set("finAccountName", "Partener Colibri")
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

    static Map<String, Object> findAffiliate(ExecutionContext ec) {
        String searchKeyword = (String) ec.context.searchKeyword
        def ecf = ec.entity.conditionFactory

        EntityFind ef = ec.entity.find("mantle.party.FindAffiliateView").distinct(true)
        ef.selectFields(["partyId", "organizationName", "firstName", "contactNumber", "idValue"])
        ef.condition("roleTypeId", "Affiliate")
        ef.condition("partyIdTypeEnumId", "PtidAffiliateId")

        if (StringUtils.notEmpty(searchKeyword))
            ef.condition(ecf.makeCondition([
                    ecf.makeCondition("idValue", EntityCondition.EQUALS, searchKeyword.toUpperCase()),
                    ecf.makeCondition("contactNumber", org.moqui.entity.EntityCondition.LIKE, "%${searchKeyword}")],
                    EntityCondition.JoinOperator.OR))

        List<Map<String, Object>> resultList = []
        for (EntityValue party in ef.list()) {
            InvocationResult result = StringUtils.notEmpty(searchKeyword) ?
                    LegacySyncServices.customerDebtDocs(NumberUtils.parseToLong(party.partyId)) :
                    InvocationResult.ok()
            ImmutableList<RulajPartener> unpaidPartners = result.extra(InvocationResult.PARTNER_RULAJ_KEY);
            ImmutableList<AccountingDocument> accDocs = result.extra(InvocationResult.ACCT_DOC_KEY);

            resultList.add([
                    partyId: party.partyId,
                    partnerCode: party.idValue,
                    name: party.organizationName ?: party.firstName,
                    phone: party.contactNumber,
                    discDisponibil: unpaidPartners.stream().findFirst().map {it.discDisponibil}
                            .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discDisponibil}.orElse(BigDecimal.ZERO)}
            ])
        }

        return [resultList: resultList]
    }

    static Map<String, Object> checkPartnerAgreements(ExecutionContext ec) {
        // 1. Resolve the affiliate party ID from context (passed via async call from storeLegacyPayment / createColibriPartner)
//        String paymentId = ec.context.paymentId as String
//
//        // Determine affiliatePartyId: first check PaymentParty with role "Affiliate", fallback to Payment.fromPartyId
//        String affiliatePartyId = null
//        if (paymentId) {
//            EntityValue paymentParty = ec.entity.find("mantle.account.payment.PaymentParty")
//                    .condition("paymentId", paymentId)
//                    .condition("roleTypeId", "Affiliate")
//                    .one()
//            if (paymentParty) {
//                affiliatePartyId = paymentParty.partyId as String
//            }
//            if (!affiliatePartyId) {
//                EntityValue payment = ec.entity.find("mantle.account.payment.Payment")
//                        .condition("paymentId", paymentId)
//                        .one()
//                if (payment) affiliatePartyId = payment.fromPartyId as String
//            }
//        } else {
//            // No paymentId in context — try direct affiliatePartyId if provided
//            affiliatePartyId = ec.context.affiliatePartyId as String
//        }
//        if (!affiliatePartyId) return [:]
//
//        // 2. Check if this party is enrolled in the "PartenerColibri" agreement as Affiliate
//        EntityValue agreementParty = ec.entity.find("mantle.party.agreement.AgreementParty")
//                .condition("agreementId", "PartenerColibri")
//                .condition("roleTypeId", "Affiliate")
//                .condition("partyId", affiliatePartyId)
//                .one()
//        if (!agreementParty) return [:]
//
//        // 3. Find the last reached threshold: most recent FinancialAccountTrans deposit from "L2" to this affiliate
//        //    whose finAccountTransId follows the pattern "PartenerColibri_<N>" — pick the one with the largest N
//        EntityList depositTrans = ec.entity.find("mantle.account.financial.FinancialAccountTrans")
//                .condition("toPartyId", affiliatePartyId)
//                .condition("finAccountTransTypeEnumId", "FattDeposit")
//                .condition("fromPartyId", "L2")
//                .list()
//
//        EntityValue lastThresholdReached = null
//        int lastThresholdIndex = 0
//        for (EntityValue trans in depositTrans) {
//            String transId = trans.finAccountTransId as String
//            if (transId?.startsWith("PartenerColibri_")) {
//                try {
//                    int idx = transId.split("_")[1].toInteger()
//                    if (idx > lastThresholdIndex) {
//                        lastThresholdIndex = idx
//                        lastThresholdReached = trans
//                    }
//                } catch (NumberFormatException ignored) { /* skip non-matching IDs */ }
//            }
//        }
//
//        // 4. Determine the next threshold AgreementTerm
//        String nextTermId = lastThresholdReached ? "PartenerColibri_${lastThresholdIndex + 1}" : "PartenerColibri_1"
//        EntityValue nextThreshold = ec.entity.find("mantle.party.agreement.AgreementTerm")
//                .condition("agreementTermId", nextTermId)
//                .one()
//        if (!nextThreshold) return [:]
//
//        // 5. Calculate the total payments attributed to this affiliate
//        //    Sum all Payment amounts where the affiliate appears via PaymentParty (role Affiliate), or as fromPartyId
//        EntityList affiliatePaymentParties = ec.entity.find("mantle.account.payment.PaymentParty")
//                .condition("partyId", affiliatePartyId)
//                .condition("roleTypeId", "Affiliate")
//                .selectField("paymentId")
//                .list()
//        List<String> affiliatePaymentIds = affiliatePaymentParties.collect { it.paymentId as String }
//
//        BigDecimal affiliatePartyPaymentsTotal = BigDecimal.ZERO
//        if (affiliatePaymentIds) {
//            EntityList payments = ec.entity.find("mantle.account.payment.Payment")
//                    .condition(ec.entity.conditionFactory.makeCondition(
//                            "paymentId", EntityCondition.IN, affiliatePaymentIds))
//                    .selectField("amount")
//                    .list()
//            for (EntityValue p in payments) {
//                if (p.amount) affiliatePartyPaymentsTotal = affiliatePartyPaymentsTotal.add(p.amount as BigDecimal)
//            }
//        }
//
//        // 6. Check threshold and guard against duplicate processing (idempotency via finAccountTransId)
//        BigDecimal minQuantity = nextThreshold.termText ? new BigDecimal(nextThreshold.termText as String) : BigDecimal.ZERO
//        if (affiliatePartyPaymentsTotal > minQuantity) {
//            // Guard: only proceed if the threshold trans doesn't already exist
//            EntityValue existingTrans = ec.entity.find("mantle.account.financial.FinancialAccountTrans")
//                    .condition("finAccountTransId", nextTermId)
//                    .one()
//            if (!existingTrans) {
//                BigDecimal lastTermNumber = lastThresholdReached ? (lastThresholdReached.termNumber as BigDecimal ?: BigDecimal.ZERO) : BigDecimal.ZERO
//                BigDecimal bonusAmount = (nextThreshold.termNumber as BigDecimal) - lastTermNumber
//
//                // Create the FinancialAccountTrans milestone deposit
//                EntityValue fat = ec.entity.makeValue("mantle.account.financial.FinancialAccountTrans")
//                fat.set("finAccountTransId", nextTermId)
//                fat.set("finAccountTransTypeEnumId", "FattDeposit")
//                fat.set("finAccountId", affiliatePartyId)   // partner's FinancialAccount has finAccountId = partyId
//                fat.set("fromPartyId", "L2")
//                fat.set("toPartyId", affiliatePartyId)
//                fat.set("transactionDate", ec.user.nowTimestamp)
//                fat.set("entryDate", ec.user.nowTimestamp)
//                fat.set("amount", bonusAmount)
//                fat.create()
//
//                // TODO: create legacy DiscountDoc incasare
//                // TODO: send threshold reached SMS to affiliate
//            }
//        }

        return [:]
    }
}
