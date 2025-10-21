package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFind
import org.moqui.util.MNode
import org.moqui.util.RestClient

import java.sql.Timestamp

ExecutionContext ec = context.ec
Timestamp start = context.start
Timestamp end = context.end
var location = System.getProperty("ANAF_CONNECTOR_BASE_URL")+"/invoices/search/between"

String parmsStr = RestClient.parametersMapToString([start: start.toLocalDateTime().toLocalDate(), end: end.toLocalDateTime().toLocalDate()])
if (parmsStr != null && !parmsStr.isEmpty())
    location = location + "?" + parmsStr

RestClient.RestResponse response = ec.service.rest().method(RestClient.Method.GET)
        .uri(location)
        .call()

if (response.statusCode < 200 || response.statusCode >= 300) {
    ec.logger.warn("Remote REST service findAnafInvoices error " + response.statusCode + " (" + response.reasonPhrase + ") in response to " + location + ", response text:\n" + response.text())
    ec.getMessage().addError("Remote service error ${response.statusCode}: ${response.reasonPhrase}")
    return null
}

var receivedAnafInvoices = response.jsonObject()
for (receivedAnafInvoice in receivedAnafInvoices) {
    var id = receivedAnafInvoice["id"]
    var issueDate = receivedAnafInvoice["issueDate"]
    var xmlRaw = receivedAnafInvoice["xmlRaw"]

    if (ec.getEntity().find("moqui.service.message.SystemMessage")
            .condition("systemMessageId", id)
            .count() == 0) {
        // create new messages for received anaf einvoices
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: id, messageDate: issueDate, messageText: xmlRaw])
                .call()
    }
}

resultList = []
EntityFind ef = ec.entity.find("moqui.service.message.SystemMessage")

dv = ef.makeEntityDynamicView()
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

for (msg in ef.list()) {
    MNode rootNode = MNode.parseText(null, msg.messageText)
    var invoiceTotal = rootNode.first("cac:LegalMonetaryTotal").first("cbc:TaxInclusiveAmount").text
    var taxExclusiveAmount = rootNode.first("cac:LegalMonetaryTotal").first("cbc:TaxExclusiveAmount").text
    var taxTotal = rootNode.first("cac:TaxTotal").first("cbc:TaxAmount").text
    var senderName = rootNode.first("cac:AccountingSupplierParty").first("cac:Party")
            .first("cac:PartyLegalEntity").first("cbc:RegistrationName").text

    resultList.add(["id": msg.id, "senderId": msg.senderId, "issueDate": msg.issueDate, "invoiceNumber": msg.invoiceNumber,
                    "invoiceId": msg.invoiceId, "statusId": msg.statusId, "invoiceTotal": invoiceTotal,
                    "taxTotal": taxTotal, "taxExclusiveAmount": taxExclusiveAmount, "senderName": senderName,
                    "rawXml": msg.messageText])
}