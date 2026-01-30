package ro.colibri.legacy.service.ui

import com.google.common.collect.ImmutableList
import org.moqui.context.ExecutionContext
import ro.colibri.entities.comercial.Operatiune
import ro.colibri.entities.comercial.Product
import ro.colibri.entities.comercial.mappings.ProductGestiuneMapping
import ro.colibri.legacy.service.LegacySyncServices

ExecutionContext ec = context.ec
productList = []

for (var entity in ec.entity.find("mantle.product.ProductFindView")
        .condition("idValue", lookupId)
        .list()) {
    Map<String, Object> p = entity.getMap()

    var moquiPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
            .parameter("productStoreId", facilityId)
            .parameter("productId", p.productId)
            .call()
            .get("price")

    Product legacyProduct
    if (lookupId) {
        Operatiune op = new Operatiune()
        op.setBarcode(lookupId)
        legacyProduct = LegacySyncServices.convertToProducts(ImmutableList.of(op)).stream().findFirst().orElse(null)
    }

    var price = moquiPrice ?: legacyProduct?.pricePerUom
    p.put("priceNicename", price + " RON/" + legacyProduct?.uom)

    Set<ProductGestiuneMapping> stocuri = legacyProduct?.stocuri
    p.put("stockL1", stocuri.find { it.gestiune.importName.equals("L1")}?.stoc + " " + legacyProduct?.uom)
    p.put("stockL2", stocuri.find { it.gestiune.importName.equals("L2")}?.stoc + " " + legacyProduct?.uom)

    if (lookupId) {
        var supplierPrice = ec.entity.find("mantle.product.ProductPrice")
                .condition("productId", p.productId)
                .condition("customerPartyId", facilityId)
                .condition("preferredOrderEnumId", "SpoMain")
                .one()

        var supplierName = supplierPrice ? ec.entity.find("mantle.party.Organization")
                .condition("partyId", supplierPrice.vendorPartyId)
                .one() : null

        if (supplierName?.organizationName?.equals("DUNCA CONSTRUCT SRL"))
            tileName = ro.colibri.util.Utils.extractTileName(p.productName)
    }

    productList.add(p)
}

return [productList: productList, tileName: tileName]