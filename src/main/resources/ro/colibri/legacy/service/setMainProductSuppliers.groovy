package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue

ExecutionContext ec = context.ec

resultList = []
for (i in items) {
    // set old main supplier as alternate supplier
    var oldMainSupplierPrices = ec.getEntity().find("ProductPrice")
            .condition("productId", i.productId)
            .condition("preferredOrderEnumId", "SpoMain")
            .condition("customerPartyId", i.organizationPartyId)
            .condition("priceTypeEnumId", "PptCurrent")
            .condition("pricePurposeEnumId", "PppPurchase")
            .list()

    for (item in oldMainSupplierPrices) {
        item.set("preferredOrderEnumId", "SpoAlternate").update()
    }

    // create product on the fly
    EntityValue prod = ec.getEntity().makeValue("Product")
    prod.set("productId", i.productId)
    prod.set("productTypeEnumId", "PtAsset")
    prod.set("assetTypeEnumId", "AstTpInventory")
    prod.set("assetClassEnumId", "AsClsInventoryFin")
    prod.createOrUpdate()

    resultList.add(
            ec.service.sync().name("store", "mantle.product.ProductPrice")
                    .parameters([productPriceId: i.supplierId+"_"+i.productId+"_"+i.organizationPartyId,
                                 productId: i.productId,
                                 vendorPartyId: i.supplierId,
                                 preferredOrderEnumId: "SpoMain",
                                 customerPartyId: i.organizationPartyId,
                                 priceTypeEnumId: "PptCurrent",
                                 pricePurposeEnumId: "PppPurchase",
                                 price: i.price, priceUomId: "RON"])
                    .call())
}