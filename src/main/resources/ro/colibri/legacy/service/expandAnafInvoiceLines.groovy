package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import org.moqui.service.ServiceException
import org.moqui.util.MNode

ExecutionContext ec = context.ec

msg = ec.getEntity().find("moqui.service.message.SystemMessage")
        .condition("systemMessageId", systemMessageId)
        .one()

MNode rootNode = MNode.parseText(null, msg.messageText)

lines = []
if (msg.docType == "Invoice") {
    lines = rootNode.children("cac:InvoiceLine")
} else if (msg.docType == "CreditNote") {
    lines = rootNode.children("cac:CreditNoteLine")
} else
    throw new ServiceException(msg.docType + " document type not supported")

for (line in lines) {
    lineId = line.first("cbc:ID").text

    EntityValue lineMsg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
    lineMsg.set("systemMessageId", systemMessageId+"_"+lineId)
    lineMsg.set("systemMessageTypeId", "ANAFReceivedInvoiceLine")
    lineMsg.set("statusId", "SmsgConsumed")
    lineMsg.set("isOutgoing", "N")
    lineMsg.set("initDate", ec.user.nowTimestamp)
    lineMsg.set("processedDate", ec.user.nowTimestamp)
    lineMsg.set("parentMessageId", systemMessageId)
    lineMsg.set("messageText", line.toString())
    lineMsg.create()
}