package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue

ExecutionContext ec = context.ec

lineMsg = ec.getEntity().find("moqui.service.message.SystemMessage")
        .condition("systemMessageId", systemMessageId)
        .one()

if (lineMsg.statusId != 'SmsgConsumed') {
    ec.message.addError(ec.resource.expand('''System message [${systemMessageId}] has status [${lineMsg.statusId}] and must be SmsgConsumed, not receiving.''' ?: "Error in actions",''))
    return
}

if (lineMsg.systemMessageTypeId != 'ANAFReceivedInvoiceLine') {
    ec.message.addError(ec.resource.expand('''System message [${systemMessageId}] has message type [${lineMsg.systemMessageTypeId}] and must be ANAFReceivedInvoiceLine, not receiving.''' ?: "Error in actions",''))
    return
}

// Sistemul actualizeaza codul furnizorului pentru produsul respectiv
if (supplierId != null && productId != null && facilityPartyId != null &&
        supplierProductId != null) {
    // create supplier and product on demand
    EntityValue prod = ec.getEntity().makeValue("mantle.product.Product")
    prod.set("productId", productId)
    prod.createOrUpdate()
    final EntityValue party = ec.getEntity().makeValue("Party")
    party.set("partyId", supplierId)
    party.createOrUpdate()

    ec.service.sync().name("store", "mantle.product.ProductPrice")
            .parameters([productPriceId: supplierId+"_"+productId+"_"+facilityPartyId,
                         productId: productId,
                         vendorPartyId: supplierId,
                         customerPartyId: facilityPartyId,
                         "otherPartyItemId": supplierProductId,
                         "otherPartyItemName": supplierProductName])
            .call()
}

// marcheaza linia de pe eFactura ca receptionata
ec.service.sync().name("update#moqui.service.message.SystemMessage")
        .parameters([systemMessageId:systemMessageId, statusId:'SmsgConfirmed', processedDate:nowDate])
        .call()

// Dupa ce ultima linie a fost receptionata, Sistemul marcheaza eFactura ca receptionata.
if (lineMsg.parentMessageId != null) {
    var allLinesNo = ec.getEntity().find("moqui.service.message.SystemMessage")
            .condition("parentMessageId", lineMsg.parentMessageId)
            .count()
    var receivedLinesNo = ec.getEntity().find("moqui.service.message.SystemMessage")
            .condition("parentMessageId", lineMsg.parentMessageId)
            .condition("statusId", "SmsgConfirmed")
            .count()
    if (allLinesNo == receivedLinesNo)
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId:lineMsg.parentMessageId, invoiceId:invoiceId])
                .call()
}
