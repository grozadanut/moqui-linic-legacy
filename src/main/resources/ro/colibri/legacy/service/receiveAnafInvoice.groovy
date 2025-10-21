package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import org.moqui.service.ServiceException
import org.moqui.util.MNode

ExecutionContext ec = context.ec

MNode rootNode = MNode.parseText(null, messageText)
final String docType = rootNode.nodeName

EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
msg.set("messageId", rootNode.first("cbc:ID").text)
msg.set("senderId", rootNode.first("cac:AccountingSupplierParty").first("cac:Party")
        .first("cac:PartyTaxScheme").first("cbc:CompanyID").text)
msg.set("receiverId", rootNode.first("cac:AccountingCustomerParty").first("cac:Party")
        .first("cac:PartyTaxScheme").first("cbc:CompanyID").text)
msg.set("docControl", rootNode.first("cac:LegalMonetaryTotal").first("cbc:TaxInclusiveAmount").text)

if (docType.equalsIgnoreCase("Invoice")) {
    msg.set("docType", rootNode.first("cbc:InvoiceTypeCode").text)
} else if (docType.equalsIgnoreCase("CreditNote")) {
    msg.set("docType", rootNode.first("cbc:CreditNoteTypeCode").text)
} else
    throw new ServiceException(docType + " document type not supported")

msg.set("systemMessageId", systemMessageId)
msg.set("systemMessageTypeId", "ANAFReceivedInvoice")
msg.set("statusId", "SmsgConfirmed")
msg.set("isOutgoing", "N")
msg.set("initDate", ec.user.nowTimestamp)
msg.set("processedDate", ec.user.nowTimestamp)
msg.set("messageText", messageText)
msg.set("messageDate", messageDate)
msg.set("docSubType", docType)
msg.create()