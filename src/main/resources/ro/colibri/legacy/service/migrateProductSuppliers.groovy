package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext

ExecutionContext ec = context.ec

var organizationPartyIds = ["L1", "L2"]

for(organizationPartyId in organizationPartyIds) {
    var supplierPrices = ec.getEntity().find("ProductPrice")
            .condition("customerPartyId", organizationPartyId)
            .condition("priceTypeEnumId", "PptCurrent")
            .condition("pricePurposeEnumId", "PppPurchase")
            .list()

    for (price in supplierPrices) {
        price.cloneValue()
                .set("productPriceId", price.vendorPartyId+"_"+price.productId+"_"+price.customerPartyId).create()
        price.delete()
    }
}