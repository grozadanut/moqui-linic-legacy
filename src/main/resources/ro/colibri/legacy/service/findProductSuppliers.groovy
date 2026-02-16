package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityDynamicView
import org.moqui.entity.EntityFind
import ro.colibri.util.Utils

ExecutionContext ec = context.ec

EntityFind efSuppl = ec.entity.find("ProductPrice").distinct(true)
dv = efSuppl.makeEntityDynamicView()
dv.addMemberEntity("PP", "ProductPrice", null, null, null)
dv.addMemberEntity("P", "Product", "PP", null, ["productId":null])
dv.addMemberEntity("SUP", "Organization", "PP", null, ["vendorPartyId":"partyId"])
dv.addAlias("PP", "productPriceId")
dv.addAlias("PP", "productId")
dv.addAlias("PP", "customerPartyId")
dv.addAlias("PP", "priceTypeEnumId")
dv.addAlias("PP", "pricePurposeEnumId")
dv.addAlias("PP", "price")
dv.addAlias("PP", "supplierId", "vendorPartyId", null)
dv.addAlias("PP", "preferredOrderEnumId")
dv.addAlias("P", "requireInventory")
dv.addAlias("SUP", "supplierName", "organizationName", null)

efSuppl.condition("productId", EntityCondition.IS_NOT_NULL, null)
efSuppl.condition("customerPartyId", organizationPartyId)
efSuppl.condition("priceTypeEnumId", "PptCurrent")
efSuppl.condition("pricePurposeEnumId", "PppPurchase")
efSuppl.condition(ec.entity.conditionFactory.makeCondition(
        [ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, null),
         ec.entity.conditionFactory.makeCondition("requireInventory", EntityCondition.EQUALS, "Y")],
        EntityCondition.OR))

resultList = []
efSuppl.selectFields(["productPriceId", "productId", "supplierId", "supplierName", "price", "preferredOrderEnumId"])
Utils.addOrUpdate(resultList, efSuppl.list(), "productPriceId")

EntityFind efPareto = ec.entity.find("ProductCategoryMember").distinct(true)
dvPareto = efPareto.makeEntityDynamicView()
dvPareto.addMemberEntity("PCM", "ProductCategoryMember", null, null, null)
dvPareto.addMemberEntity("PC", "ProductCategory", "PCM", null, ["productCategoryId":null])
dvPareto.addAlias("PC", "ownerPartyId")
dvPareto.addAlias("PC", "productCategoryTypeEnumId")
dvPareto.addAlias("PCM", "pareto", "productCategoryId", null)
dvPareto.addAlias("PCM", "productId")
efPareto.condition("ownerPartyId", EntityCondition.EQUALS, organizationPartyId)
efPareto.condition("productCategoryTypeEnumId", EntityCondition.EQUALS, "PctPareto")
efPareto.selectFields(["productId", "pareto"])
Utils.addOrUpdate(resultList, efPareto.list(), "productId")

EntityFind efFacility = ec.entity.find("ProductFacility").distinct(true)
efFacility.condition("facilityId", EntityCondition.EQUALS, organizationPartyId)
efFacility.selectFields(["productId", "minimumStock"])
Utils.addOrUpdate(resultList, efFacility.list(), "productId")

EntityFind efRequirement = ec.entity.find("Requirement").distinct(true)
EntityDynamicView dvRequirement = efRequirement.makeEntityDynamicView()
dvRequirement.addMemberEntity("R", "Requirement", null, null, null)
dvRequirement.addAlias("R", "facilityId")
dvRequirement.addAlias("R", "requirementTypeEnumId")
dvRequirement.addAlias("R", "productId")
dvRequirement.addAlias("R", "quantityTotal", "quantity", "sum")
efRequirement.condition("facilityId", organizationPartyId)
efRequirement.condition("requirementTypeEnumId", "RqTpInventory")
efRequirement.selectFields(["productId", "quantityTotal"])
Utils.addOrUpdate(resultList, efRequirement.list(), "productId")