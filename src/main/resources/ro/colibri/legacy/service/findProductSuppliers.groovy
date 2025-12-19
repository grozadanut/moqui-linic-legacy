package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

EntityFind ef = ec.entity.find("Product").distinct(true)

dv = ef.makeEntityDynamicView()
dv.addMemberEntity("P", "Product", null, null, null)
dv.addMemberEntity("PP", "ProductPrice", "P", true, ["productId":null])
dv.addMemberEntity("PF", "ProductFacility", "P", true, ["productId":null])
dv.addMemberEntity("PCM", "ProductCategoryMember", "P", true, ["productId":null])
dv.addMemberEntity("PC", "ProductCategory", "PCM", true, ["productCategoryId":null])
dv.addMemberEntity("SUP", "Organization", "PP", true, ["vendorPartyId":"partyId"])
dv.addAlias("P", "productId")
dv.addAlias("P", "requireInventory")
dv.addAlias("PP", "productPriceId")
dv.addAlias("PP", "customerPartyId")
dv.addAlias("PP", "priceTypeEnumId")
dv.addAlias("PP", "pricePurposeEnumId")
dv.addAlias("PP", "price")
dv.addAlias("PP", "supplierId", "vendorPartyId", null)
dv.addAlias("PP", "preferredOrderEnumId")
dv.addAlias("PF", "facilityId")
dv.addAlias("PF", "minimumStock")
dv.addAlias("SUP", "supplierName", "organizationName", null)
dv.addAlias("PC", "ownerPartyId")
dv.addAlias("PC", "productCategoryTypeEnumId")
dv.addAlias("PCM", "pareto", "productCategoryId", null)

ef.condition("productId", EntityCondition.IS_NOT_NULL, null)
ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("productPriceId", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("customerPartyId", EntityCondition.EQUALS, organizationPartyId)],
        EntityCondition.OR))

ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("productPriceId", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("priceTypeEnumId", EntityCondition.EQUALS, "PptCurrent")],
        EntityCondition.OR))
ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("productPriceId", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("pricePurposeEnumId", EntityCondition.EQUALS, "PppPurchase")],
        EntityCondition.OR))

ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, "Y")],
         EntityCondition.OR))

paretoFindCondList = [ec.entity.conditionFactory.makeCondition("ownerPartyId", EntityCondition.EQUALS, organizationPartyId),
                  ec.entity.conditionFactory.makeCondition("productCategoryTypeEnumId", EntityCondition.EQUALS, "PctPareto")]
ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition(paretoFindCondList, EntityCondition.AND),
         ec.entity.conditionFactory.makeCondition("pareto", EntityCondition.EQUALS, null)],
        EntityCondition.OR))

ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("facilityId", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("facilityId", EntityCondition.EQUALS, organizationPartyId)],
        EntityCondition.OR))

ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("supplierId", EntityCondition.IS_NOT_NULL, null),
         ec.entity.conditionFactory.makeCondition("pareto", EntityCondition.IS_NOT_NULL, null),
         ec.entity.conditionFactory.makeCondition("minimumStock", EntityCondition.IS_NOT_NULL, null)],
        EntityCondition.OR))

ef.selectFields(["productPriceId", "productId", "supplierId", "supplierName", "pareto", "price", "preferredOrderEnumId",
                 "minimumStock"])

resultList = []
resultList.addAll(ef.list())