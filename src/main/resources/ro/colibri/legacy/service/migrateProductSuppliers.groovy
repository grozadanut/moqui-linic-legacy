package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

var organizationPartyIds = ["L1", "L2"]

for(organizationPartyId in organizationPartyIds) {
    EntityFind ef = ec.entity.find("AgreementItem").distinct(true)

    dv = ef.makeEntityDynamicView()
    dv.addMemberEntity("AI", "AgreementItem", null, null, null)
    dv.addMemberEntity("AGR", "Agreement", "AI", null, ["agreementId":null])
    dv.addMemberEntity("ORG", "Organization", "AGR", null, ["otherPartyId":"partyId"])
    dv.addMemberEntity("P", "Product", "AI", null, ["productId":null])
    dv.addMemberEntity("PCM", "ProductCategoryMember", "P", true, ["productId":null])
    dv.addMemberEntity("PC", "ProductCategory", "PCM", true, ["productCategoryId":null])
    dv.addAlias("AI", "agreementItemTypeEnumId")
    dv.addAlias("AI", "productId")
    dv.addAlias("AGR", "agreementTypeEnumId")
    dv.addAlias("AGR", "otherRoleTypeId")
    dv.addAlias("AGR", "organizationPartyId")
    dv.addAlias("AGR", "supplierId", "otherPartyId", null)
    dv.addAlias("ORG", "supplierName", "organizationName", null)
    dv.addAlias("P", "requireInventory")
    dv.addAlias("PC", "ownerPartyId")
    dv.addAlias("PC", "productCategoryTypeEnumId")
    dv.addAlias("PCM", "pareto", "productCategoryId", null)

    ef.condition("agreementItemTypeEnumId", "AitPurchase")
    ef.condition("productId", EntityCondition.IS_NOT_NULL, null)
    ef.condition("agreementTypeEnumId", "AgrProduct")
    ef.condition("otherRoleTypeId", "Supplier")
    ef.condition("organizationPartyId", organizationPartyId)
    reqinvCondList = [ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, null),
                      ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, "Y")]
    ef.condition(ec.entity.conditionFactory.makeCondition(reqinvCondList, EntityCondition.OR))

    paretoFindCondList = [ec.entity.conditionFactory.makeCondition("ownerPartyId", EntityCondition.EQUALS, organizationPartyId),
                          ec.entity.conditionFactory.makeCondition("productCategoryTypeEnumId", EntityCondition.EQUALS, "PctPareto")]
    paretoCondList = [ec.entity.conditionFactory.makeCondition(paretoFindCondList, EntityCondition.AND),
                      ec.entity.conditionFactory.makeCondition("pareto", EntityCondition.EQUALS, null)]
    ef.condition(ec.entity.conditionFactory.makeCondition(paretoCondList, EntityCondition.OR))

    ef.selectFields(["productId", "supplierId", "supplierName", "pareto"])

    for (var p : ef.list()) {
        ec.service.sync().name("store", "mantle.product.ProductPrice")
                .parameters([productPriceId: p.supplierId+"_"+p.productId,
                             productId: p.productId,
                             vendorPartyId: p.supplierId,
                             preferredOrderEnumId: "SpoMain",
                             customerPartyId: organizationPartyId,
                             priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase",
                             price: 0, priceUomId: "RON"])
                .call()
    }
}

ec.service.sync().name("delete", "AgreementItem").parameters([agreementId: "*", agreementItemSeqId: "*"]).call()
ec.service.sync().name("delete", "Agreement").parameters([agreementId: "*"]).call()