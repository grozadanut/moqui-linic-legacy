package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.util.MNode

ExecutionContext ec = context.ec

if (ec.getEntity().find("moqui.service.message.SystemMessage")
        .condition("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        .condition("parentMessageId", systemMessageId)
        .count() == 0) {
    ec.service.sync().name("AnafServices.expand#AnafInvoiceLines")
            .parameters([systemMessageId: systemMessageId])
            .call()
}

msg = ec.getEntity().find("moqui.service.message.SystemMessage")
        .condition("systemMessageId", systemMessageId)
        .one()

resultList = []
for (lineMsg in ec.getEntity().find("moqui.service.message.SystemMessage")
        .condition("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        .condition("parentMessageId", systemMessageId)
        .list()) {
    MNode lineNode = MNode.parseText(null, lineMsg.messageText)
    var lineId = lineNode.first("cbc:ID").text
    var name = lineNode.first("cac:Item").first("cbc:Name").text
    var price = lineNode.first("cac:Price").first("cbc:PriceAmount").text
    var priceCurrency = lineNode.first("cac:Price").first("cbc:PriceAmount")
            .attribute("currencyID")
    var quantity = msg.docType == "Invoice" ? lineNode.first("cbc:InvoicedQuantity").text
            : lineNode.first("cbc:CreditedQuantity").text
    var uom = msg.docType == "Invoice" ? lineNode.first("cbc:InvoicedQuantity").attribute("unitCode")
            : lineNode.first("cbc:CreditedQuantity").attribute("unitCode")
    var total = lineNode.first("cbc:LineExtensionAmount").text
    var totalCurrency = lineNode.first("cbc:LineExtensionAmount").attribute("currencyID")

    resultList.add(["id": lineMsg.systemMessageId, "lineId": lineId, "name": name, "price": price,
                    "priceCurrency": priceCurrency, "quantity": quantity, "uom": uom, "total": total,
                    "totalCurrency": totalCurrency])
}