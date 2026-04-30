package ro.colibri.legacy.service.ui

import java.sql.Timestamp
import java.time.LocalDateTime

import static ro.flexbiz.util.commons.PresentationUtils.NEWLINE
import com.google.common.collect.ImmutableList
import jakarta.persistence.PreUpdate
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
import ro.flexbiz.util.commons.PresentationUtils
import ro.flexbiz.util.commons.StringUtils

class PartnerServices {
    static Map<String, Object> storeLegacyPayment(ExecutionContext ec) {
        if (ec.entity.find("mantle.account.payment.Payment")
                .condition("paymentId", ec.context.paymentId)
                .one() != null)
            return [:]

        // create Party on demand
        ec.getEntity().makeValue("Party").set("partyId", ec.context.fromPartyId).createOrUpdate()

        EntityValue payment = ec.entity.makeValue("mantle.account.payment.Payment")
        payment.set("paymentId", ec.context.paymentId)
        payment.set("paymentTypeEnumId", "PtInvoicePayment")
        payment.set("fromPartyId", ec.context.fromPartyId)
        payment.set("toPartyId", ec.context.toPartyId)
        payment.set("statusId", "PmntDelivered")
        payment.set("effectiveDate", ec.user.nowTimestamp)
        payment.set("amount", ec.context.amount)
        payment.create()

        if (StringUtils.notEmpty(ec.context.affiliatePartnerId)) {
            // create Party on demand
            ec.getEntity().makeValue("Party").set("partyId", ec.context.affiliatePartnerId).createOrUpdate()

            EntityValue paymentParty = ec.entity.makeValue("mantle.account.payment.PaymentParty")
            paymentParty.set("paymentId", ec.context.paymentId)
            paymentParty.set("partyId", ec.context.affiliatePartnerId)
            paymentParty.set("roleTypeId", "Affiliate")
            paymentParty.create()
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
            partyContactMech.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)))
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

        // find by either partner code or phone number
        if (StringUtils.notEmpty(searchKeyword))
            ef.condition(ecf.makeCondition([
                    ecf.makeCondition([partyIdTypeEnumId: "PtidAffiliateId", idValue: searchKeyword.toUpperCase()]),
                    ecf.makeCondition([ecf.makeCondition([phoneContactMechPurposeId: "PhonePrimary"]),
                                       ecf.makeConditionDate("phoneFromDate", "phoneThruDate", ec.user.nowTimestamp),
                                       ecf.makeCondition("contactNumber", EntityCondition.LIKE, "%${searchKeyword}")])],
                    EntityCondition.JoinOperator.OR))

        // Deduplicate by partyId: a party with both a PostalAddress and a TelecomNumber produces
        // multiple rows from the view. We prefer the row that carries a contactNumber so that the
        // phone is always surfaced in the result.
        Map<String, EntityValue> partyMap = new LinkedHashMap<>()
        for (EntityValue party in ef.list()) {
            String pid = party.partyId as String
            EntityValue existing = partyMap.get(pid)
            if (existing == null || (existing.contactNumber == null && party.contactNumber != null))
                partyMap.put(pid, party)
        }

        List<Map<String, Object>> resultList = []
        for (EntityValue party in partyMap.values()) {
            InvocationResult result = StringUtils.notEmpty(searchKeyword) ?
                    LegacySyncServices.customerDebtDocs(NumberUtils.parseToLong(party.partyId)) :
                    InvocationResult.ok()
            ImmutableList<RulajPartener> unpaidPartners = result.extra(InvocationResult.PARTNER_RULAJ_KEY)
            ImmutableList<AccountingDocument> accDocs = result.extra(InvocationResult.ACCT_DOC_KEY)

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

    private static BigDecimal affiliatePartyPaymentsTotal(ExecutionContext ec, String affiliatePartyId) {
        def ecf = ec.entity.conditionFactory
        // Sum all Payment amounts where the affiliate appears via PaymentParty (role Affiliate), or as fromPartyId
        EntityFind ef = ec.entity.find("linic.legacy.payment.PaymentSummary").distinct(true)
        ef.selectFields(["amountTotal"])
        ef.condition("statusId", "PmntDelivered")
        ef.condition("toPartyId", "L2")
        ef.condition(ecf.makeCondition([
                ecf.makeCondition([partyId: affiliatePartyId, roleTypeId: "Affiliate"]),
                ecf.makeCondition("fromPartyId", EntityCondition.EQUALS, affiliatePartyId)],
                EntityCondition.JoinOperator.OR))

        return ef.list().stream()
                .map {it.getBigDecimal("amountTotal")}
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
    }

    static Map<String, Object> checkPartnerAgreements(ExecutionContext ec) {
        String paymentId = ec.context.paymentId as String

        // 1. Find the affiliatePartyId associated with this payment, if any
        // affiliatePartyId = PaymentParty.partyId WHERE PaymentParty.roleTypeId = "Affiliate"
        // if null affiliatePartyId = Payment.fromPartyId
        String affiliatePartyId = null
        EntityValue payment = ec.entity.find("mantle.account.payment.Payment")
                .condition("paymentId", paymentId)
                .one()
        EntityValue paymentAffiliate = ec.entity.find("mantle.account.payment.PaymentParty")
                .condition("paymentId", paymentId)
                .condition("roleTypeId", "Affiliate")
                .one()
        if (paymentAffiliate)
            affiliatePartyId = paymentAffiliate.partyId as String
        if (!affiliatePartyId)
            affiliatePartyId = payment.fromPartyId as String
        if (!affiliatePartyId) return [:]

        // 2. Check if this party is enrolled in the "PartenerColibri" agreement as Affiliate
        // agreementParty = AgreementParty WHERE agreementId = "PartenerColibri" AND roleTypeId = "Affiliate" AND partyId = affiliatePartyId
        // if agreementParty is null then EXIT
        EntityValue agreementParty = ec.entity.find("mantle.party.agreement.AgreementParty")
                .condition("agreementId", "PartenerColibri")
                .condition("roleTypeId", "Affiliate")
                .condition("partyId", affiliatePartyId)
                .one()
        if (!agreementParty) return [:]

        // 3. Find the last reached threshold: most recent FinancialAccountTrans deposit from "L2" to this affiliate
        //    whose finAccountTransId follows the pattern "PC${affiliatePartyId}_<N>" — pick the one with the largest N
        // lastThresholdReached = FinancialAccountTrans WHERE toPartyId = affiliatePartyId AND finAccountTransTypeEnumId = "FattDeposit" AND fromPartyId = "L2" AND finAccountId = affiliatePartyId
        // ORDER BY finAccountTransId.split("_")[1].toInt GET LARGEST
        EntityList depositTrans = ec.entity.find("mantle.account.financial.FinancialAccountTrans")
                .condition("finAccountId", affiliatePartyId)
                .condition("fromPartyId", "L2")
                .condition("toPartyId", affiliatePartyId)
                .condition("finAccountTransTypeEnumId", "FattDeposit")
                .list()

        EntityValue lastThresholdReached = null
        int lastThresholdIndex = 0
        for (EntityValue trans in depositTrans) {
            String transId = trans.finAccountTransId as String
            if (transId?.startsWith("PC${affiliatePartyId}_")) {
                try {
                    int idx = transId.split("_")[1].toInteger()
                    if (idx > lastThresholdIndex) {
                        lastThresholdIndex = idx
                        lastThresholdReached = trans
                    }
                } catch (NumberFormatException ex) {
                    /* skip non-matching IDs */
                    ec.logger.trace(ex.getMessage(), ex)
                }
            }
        }

        // 4. Determine the next threshold AgreementTerm
        // nextThreshold = AgreementTerm WHERE agreementTermId = "PartenerColibri_${lastThresholdIndex + 1}" ?: "PartenerColibri_1"
        // if nextThreshold is null then EXIT
        int nextThresholdIndex = lastThresholdIndex + 1
        String nextTermId = lastThresholdReached ? "PartenerColibri_${nextThresholdIndex}" : "PartenerColibri_1"
        EntityValue nextThreshold = ec.entity.find("mantle.party.agreement.AgreementTerm")
                .condition("agreementTermId", nextTermId)
                .one()
        if (!nextThreshold) return [:]

        EntityValue lastReachedTerm = null
        if (lastThresholdIndex > 0) {
            String lastTermId = "PartenerColibri_${lastThresholdIndex}"
            lastReachedTerm = ec.entity.find("mantle.party.agreement.AgreementTerm")
                    .condition("agreementTermId", lastTermId)
                    .one()
        }

        // 5. Calculate the total payments attributed to this affiliate
        BigDecimal affiliatePartyPaymentsTotal = affiliatePartyPaymentsTotal(ec, affiliatePartyId)

        EntityFind ef = ec.entity.find("mantle.party.FindAffiliateView").distinct(true)
        ef.selectFields(["contactNumber", "phoneAllowSolicitation"])
        ef.condition("partyId", affiliatePartyId)
        ef.condition("phoneContactMechPurposeId", "PhonePrimary")
        ef.conditionDate("phoneFromDate", "phoneThruDate", ec.user.nowTimestamp)
        EntityValue phone = ef.list().getFirst()

        // 6. Check threshold and guard against duplicate processing (idempotency via finAccountTransId)
        // if affiliatePartyPaymentsTotal > nextThreshold.minQuantity AND NOT EXISTS FinancialAccountTrans WHERE finAccountTransId = "PC${affiliatePartyId}_${lastThresholdIndex + 1}"
        if (affiliatePartyPaymentsTotal >= nextThreshold.getBigDecimal("minQuantity")) {
            // Guard: only proceed if the threshold trans doesn't already exist
            String nextTransId = "PC${affiliatePartyId}_${nextThresholdIndex}"
            EntityValue existingTrans = ec.entity.find("mantle.account.financial.FinancialAccountTrans")
                    .condition("finAccountTransId", nextTransId)
                    .one()
            if (!existingTrans) {
                // create FinancialAccountTrans
                //   finAccountTransId = "PC${partner.id}_${lastThresholdIndex + 1}"
                //   finAccountTransTypeEnumId = "FattDeposit"
                //   finAccountId = partner.id
                //   fromPartyId = "L2"
                //   toPartyId = affiliatePartyId
                //   transactionDate = now
                //   amount = nextThreshold.termNumber - (lastReachedTerm.termNumber ?: 0)
                BigDecimal bonusAmount = NumberUtils.subtract(nextThreshold.getBigDecimal("termNumber"),
                        lastReachedTerm?.getBigDecimal("termNumber"))

                // Create the FinancialAccountTrans milestone deposit
                ec.service.sync().name("create#mantle.account.financial.FinancialAccountTrans")
                        .parameter("finAccountTransId", nextTransId)
                        .parameter("finAccountTransTypeEnumId", "FattDeposit")
                        .parameter("reasonEnumId", "FatrCsCredit")
                        .parameter("finAccountId", affiliatePartyId)   // partner's FinancialAccount has finAccountId = partyId
                        .parameter("fromPartyId", "L2")
                        .parameter("toPartyId", affiliatePartyId)
                        .parameter("transactionDate", ec.user.nowTimestamp)
                        .parameter("entryDate", ec.user.nowTimestamp)
                        .parameter("amount", bonusAmount)
                        .disableAuthz().call()

                // create legacy DiscountDoc incasare
                LegacySyncServices.createDiscountDoc(Long.valueOf(affiliatePartyId),
                        "Prag ${nextThresholdIndex} Partener Colibri: ${nextThreshold.getBigDecimal("termNumber").toString()} lei",
                        bonusAmount, BigDecimal.ONE)

                // send threshold reached SMS to affiliate
                sendThresholdReachedSms(ec, affiliatePartyId, phone, nextThreshold.getBigDecimal("minQuantity"),
                        nextThreshold.getBigDecimal("termNumber"))
            }
        } else {
            // send payment received SMS to affiliate
            sendPaymentReceivedSms(ec, affiliatePartyId, phone,
                    affiliatePartyPaymentsTotal, payment.getBigDecimal("amount"),
                    nextThreshold.getBigDecimal("minQuantity"))
        }

        return [:]
    }

    private static void sendThresholdReachedSms(ExecutionContext ec, String affiliatePartyId, EntityValue phone,
                                                BigDecimal nextThresholdQuantity, BigDecimal nextThresholdTerm) {
        if (!phone || (phone.phoneAllowSolicitation != null && !phone.phoneAllowSolicitation)) return

        InvocationResult result = LegacySyncServices.customerDebtDocs(Long.valueOf(affiliatePartyId))
        ImmutableList<RulajPartener> unpaidPartners = result.extra(InvocationResult.PARTNER_RULAJ_KEY)
        ImmutableList<AccountingDocument> accDocs = result.extra(InvocationResult.ACCT_DOC_KEY)

        BigDecimal discAcum = unpaidPartners.stream().findFirst().map {it.discAcum}
                .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discAcum}.orElse(BigDecimal.ZERO)}
        BigDecimal discChelt = unpaidPartners.stream().findFirst().map {it.discChelt}
                .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discChelt}.orElse(BigDecimal.ZERO)}
        BigDecimal discDisponibil = unpaidPartners.stream().findFirst().map {it.discDisponibil}
                .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discDisponibil}.orElse(BigDecimal.ZERO)}

        StringBuilder sb = new StringBuilder()
        sb.append("Felicitari! Tocmai ai trecut peste pragul de ${PresentationUtils.displayBigDecimal(nextThresholdQuantity)} lei platiti si ai deblocat suma de ${PresentationUtils.displayBigDecimal(nextThresholdTerm)} lei.").append(NEWLINE)
        if (NumberUtils.greaterThan(discAcum, BigDecimal.ZERO))
            sb.append("Discount acumulat: ${PresentationUtils.displayBigDecimal(discAcum)} lei.").append(NEWLINE)
        if (NumberUtils.greaterThan(discChelt, BigDecimal.ZERO))
            sb.append("Discount cheltuit: ${PresentationUtils.displayBigDecimal(discChelt)} lei.").append(NEWLINE)
        if (NumberUtils.greaterThan(discDisponibil, BigDecimal.ZERO))
            sb.append("Discount disponibil: ${PresentationUtils.displayBigDecimal(discDisponibil)} lei.").append(NEWLINE)
        sb.append("Multumim pentru colaborare! Showroom Colibri")

        List phoneNumbers = [StringUtils.sanitizePhoneNumber(phone.contactNumber), "+40754476519"]
        ec.service.sync().name("UIServices.send#ColibriSms")
                .parameters([phoneNumbers: phoneNumbers, text: sb.toString()])
                .disableAuthz()
                .call()
    }

    private static void sendPaymentReceivedSms(ExecutionContext ec, String affiliatePartyId, EntityValue phone,
                                               BigDecimal affiliatePartyPaymentsTotal, BigDecimal amount, BigDecimal nextThresholdQuantity) {
        if (!phone || (phone.phoneAllowSolicitation != null && !phone.phoneAllowSolicitation)) return

        InvocationResult result = LegacySyncServices.customerDebtDocs(Long.valueOf(affiliatePartyId))
        ImmutableList<RulajPartener> unpaidPartners = result.extra(InvocationResult.PARTNER_RULAJ_KEY)
        ImmutableList<AccountingDocument> accDocs = result.extra(InvocationResult.ACCT_DOC_KEY)

        BigDecimal discAcum = unpaidPartners.stream().findFirst().map {it.discAcum}
                .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discAcum}.orElse(BigDecimal.ZERO)}
        BigDecimal discChelt = unpaidPartners.stream().findFirst().map {it.discChelt}
                .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discChelt}.orElse(BigDecimal.ZERO)}
        BigDecimal discDisponibil = unpaidPartners.stream().findFirst().map {it.discDisponibil}
                .orElseGet {accDocs.stream().findFirst().map {it.rulajPartener.discDisponibil}.orElse(BigDecimal.ZERO)}

        StringBuilder sb = new StringBuilder()
        sb.append("Salut! Am primit plata ta in valoare de ${PresentationUtils.displayBigDecimal(amount)} lei.").append(NEWLINE)
                .append("Suma totala platita pana acum: ${PresentationUtils.displayBigDecimal(affiliatePartyPaymentsTotal)} lei.").append(NEWLINE)
        if (nextThresholdQuantity)
            sb.append("Pana la urmatorul prag de ${PresentationUtils.displayBigDecimal(nextThresholdQuantity)}, mai ai de acumulat: ${PresentationUtils.displayBigDecimal(nextThresholdQuantity-affiliatePartyPaymentsTotal)} lei.").append(NEWLINE)
        if (NumberUtils.greaterThan(discAcum, BigDecimal.ZERO))
            sb.append("Discount acumulat: ${PresentationUtils.displayBigDecimal(discAcum)} lei.").append(NEWLINE)
        if (NumberUtils.greaterThan(discChelt, BigDecimal.ZERO))
            sb.append("Discount cheltuit: ${PresentationUtils.displayBigDecimal(discChelt)} lei.").append(NEWLINE)
        if (NumberUtils.greaterThan(discDisponibil, BigDecimal.ZERO))
            sb.append("Discount disponibil: ${PresentationUtils.displayBigDecimal(discDisponibil)} lei.").append(NEWLINE)
        sb.append("Multumim pentru colaborare! Showroom Colibri")

        List phoneNumbers = [StringUtils.sanitizePhoneNumber(phone.contactNumber), "+40754476519"]
        ec.service.sync().name("UIServices.send#ColibriSms")
                .parameters([phoneNumbers: phoneNumbers, text: sb.toString()])
                .disableAuthz()
                .call()
    }
}
