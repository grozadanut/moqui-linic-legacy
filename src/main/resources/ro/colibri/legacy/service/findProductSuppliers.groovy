package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

EntityFind ef = ec.entity.find("ProductPrice").distinct(true)

dv = ef.makeEntityDynamicView()
dv.addMemberEntity("PP", "ProductPrice", null, null, null)
dv.addMemberEntity("P", "Product", "PP", null, ["productId":null])
dv.addMemberEntity("PF", "ProductFacility", "P", true, ["productId":null])
dv.addMemberEntity("PCM", "ProductCategoryMember", "P", true, ["productId":null])
dv.addMemberEntity("PC", "ProductCategory", "PCM", true, ["productCategoryId":null])
dv.addMemberEntity("SUP", "Organization", "PP", null, ["vendorPartyId":"partyId"])
dv.addAlias("PP", "productPriceId")
dv.addAlias("PP", "productId")
dv.addAlias("PP", "customerPartyId")
dv.addAlias("PP", "priceTypeEnumId")
dv.addAlias("PP", "pricePurposeEnumId")
dv.addAlias("PP", "price")
dv.addAlias("PP", "supplierId", "vendorPartyId", null)
dv.addAlias("PP", "preferredOrderEnumId")
dv.addAlias("PF", "facilityId")
dv.addAlias("PF", "minimumStock")
dv.addAlias("SUP", "supplierName", "organizationName", null)
dv.addAlias("P", "requireInventory")
dv.addAlias("PC", "ownerPartyId")
dv.addAlias("PC", "productCategoryTypeEnumId")
dv.addAlias("PCM", "pareto", "productCategoryId", null)

ef.condition("productId", EntityCondition.IS_NOT_NULL, null)
ef.condition("customerPartyId", organizationPartyId)
ef.condition("priceTypeEnumId", "PptCurrent")
ef.condition("pricePurposeEnumId", "PppPurchase")
reqinvCondList = [ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, null),
              ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, "Y")]
ef.condition(ec.entity.conditionFactory.makeCondition(reqinvCondList, EntityCondition.OR))

paretoFindCondList = [ec.entity.conditionFactory.makeCondition("ownerPartyId", EntityCondition.EQUALS, organizationPartyId),
                  ec.entity.conditionFactory.makeCondition("productCategoryTypeEnumId", EntityCondition.EQUALS, "PctPareto")]
paretoCondList = [ec.entity.conditionFactory.makeCondition(paretoFindCondList, EntityCondition.AND),
                  ec.entity.conditionFactory.makeCondition("pareto", EntityCondition.EQUALS, null)]
ef.condition(ec.entity.conditionFactory.makeCondition(paretoCondList, EntityCondition.OR))

ef.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("facilityId", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("facilityId", EntityCondition.EQUALS, organizationPartyId)],
        EntityCondition.OR))

ef.selectFields(["productPriceId", "productId", "supplierId", "supplierName", "pareto", "price", "preferredOrderEnumId",
                 "minimumStock"])

resultList = []
resultList.addAll(ef.list())