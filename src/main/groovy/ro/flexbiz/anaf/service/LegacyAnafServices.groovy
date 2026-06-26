package ro.flexbiz.anaf.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityDynamicView
import org.moqui.entity.EntityFind
import org.moqui.entity.EntityList
import org.moqui.util.MNode

import java.sql.Timestamp
import java.util.stream.Collectors

class LegacyAnafServices {
    static Map<String, Object> findAnafInvoicesV2(ExecutionContext ec) {
        Timestamp start = ec.context.start
        Timestamp end = ec.context.end

        List receivedAnafInvoices = ec.service.sync()
                .name("ReceivedInvoiceServices.received#InvoicesBetween")
                .parameters([from: start, thru: end])
                .call()
                .get("resultList")
        receivedAnafInvoices.addAll(ec.service.sync()
                .name("ReceivedCreditNoteServices.find#AllByIssueDateBetween")
                .parameters([from: start, thru: end])
                .call()
                .get("resultList"))

        for (receivedAnafInvoice in receivedAnafInvoices) {
            var id = receivedAnafInvoice["id"]
            var issueDate = receivedAnafInvoice["issueDate"]
            var xmlRaw = receivedAnafInvoice["xmlRaw"]

            if (ec.getEntity().find("moqui.service.message.SystemMessage")
                    .condition("systemMessageId", id)
                    .count() == 0) {
                // create new messages for received anaf einvoices
                ec.service.sync().name("AnafServices.consume#AnafInvoice")
                        .parameters([systemMessageId: id, messageDate: issueDate, messageText: xmlRaw])
                        .call()
            }
        }

        List resultList = []
        EntityFind ef = ec.entity.find("moqui.service.message.SystemMessage")

        EntityDynamicView dv = ef.makeEntityDynamicView()
        dv.addMemberEntity("SM", "SystemMessage", null, null, null)
        dv.addMemberEntity("ISM", "InvoiceSystemMessage", "SM", true, ["systemMessageId":null])
        dv.addAlias("SM", "systemMessageTypeId")
        dv.addAlias("SM", "id", "systemMessageId", null)
        dv.addAlias("SM", "senderId")
        dv.addAlias("SM", "issueDate", "messageDate", null)
        dv.addAlias("SM", "invoiceNumber", "messageId", null)
        dv.addAlias("SM", "statusId")
        dv.addAlias("SM", "messageText")
        dv.addAlias("ISM", "invoiceId")

        ef.condition("systemMessageTypeId", "ANAFReceivedInvoice")
        ef.condition("issueDate", EntityCondition.BETWEEN, [start, end])

        EntityList receivedInvoices = ef.list()

        for (msg in receivedInvoices) {
            MNode rootNode = MNode.parseText(null, msg.messageText)
            var invoiceTotal = rootNode.first("cac:LegalMonetaryTotal").first("cbc:TaxInclusiveAmount")?.text ?:
                    rootNode.first("cac:LegalMonetaryTotal").first("cbc:PayableAmount")?.text
            var taxExclusiveAmount = rootNode.first("cac:LegalMonetaryTotal").first("cbc:TaxExclusiveAmount")?.text
            var taxTotal = rootNode.first("cac:TaxTotal")?.first("cbc:TaxAmount")?.text
            var senderName = rootNode.first("cac:AccountingSupplierParty").first("cac:Party")
                    .first("cac:PartyLegalEntity")?.first("cbc:RegistrationName")?.text ?:
                    rootNode.first("cac:AccountingSupplierParty").first("cac:Party")
                            .first("cac:PartyName")?.first("cbc:Name")?.text

            resultList.add(["id": msg.id, "senderId": msg.senderId, "issueDate": msg.issueDate, "invoiceNumber": msg.invoiceNumber,
                            "invoiceId": msg.invoiceId, "statusId": msg.statusId, "invoiceTotal": invoiceTotal,
                            "taxTotal": taxTotal, "taxExclusiveAmount": taxExclusiveAmount, "senderName": senderName,
                            "rawXml": msg.messageText])
        }

        List<String> receivedInvoicesIds = receivedInvoices.stream().map { it.id }.collect(Collectors.toList())
        for (msg in ec.entity.find("ro.flexbiz.efactura.ReceivedMessage")
                .condition("creationDate", EntityCondition.ComparisonOperator.BETWEEN, [start, end])
                .condition("id", EntityCondition.ComparisonOperator.NOT_IN, receivedInvoicesIds)
                .condition("statusId", EntityCondition.ComparisonOperator.NOT_IN, ["AnafRecMsgBillReceived", "AnafRecMsgBillSent"])
                .list())
            resultList.add(["id": msg.id, "senderId": msg.taxId, "issueDate": msg.creationDate,
                            "messageType": ec.l10n.localize(msg.statusId), "details": msg.details])

        return [resultList: resultList]
    }
}
