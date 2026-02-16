package ro.colibri.legacy.service.ui

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityDynamicView
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

EntityFind efRequirement = ec.entity.find("Requirement").distinct(true)
EntityDynamicView dvRequirement = efRequirement.makeEntityDynamicView()
dvRequirement.addMemberEntity("R", "Requirement", null, null, null)
dvRequirement.addMemberEntity("PP", "ProductPrice", "R", true, ["productId":null])
dvRequirement.addMemberEntity("SUP", "Organization", "PP", true, ["vendorPartyId":"partyId"])
dvRequirement.addAlias("R", "facilityId")
dvRequirement.addAlias("R", "requirementTypeEnumId")
dvRequirement.addAlias("R", "productId")
dvRequirement.addAlias("R", "statusId")
dvRequirement.addAlias("R", "description")
dvRequirement.addAlias("R", "quantityTotal", "quantity", "sum")
dvRequirement.addAlias("PP", "customerPartyId")
dvRequirement.addAlias("PP", "priceTypeEnumId")
dvRequirement.addAlias("PP", "pricePurposeEnumId")
dvRequirement.addAlias("PP", "preferredOrderEnumId")
dvRequirement.addAlias("PP", "price")
dvRequirement.addAlias("SUP", "supplierName", "organizationName", null)

efRequirement.condition("facilityId", facilityId)
efRequirement.condition("requirementTypeEnumId", "RqTpInventory")
efRequirement.condition("statusId", statusId)
efRequirement.condition(ec.entity.conditionFactory.makeCondition(
        "priceTypeEnumId", EntityCondition.EQUALS, "PptCurrent", true))
efRequirement.condition(ec.entity.conditionFactory.makeCondition(
        "pricePurposeEnumId",EntityCondition.EQUALS, "PppPurchase", true))
efRequirement.condition(ec.entity.conditionFactory.makeCondition(
        "customerPartyId", EntityCondition.EQUALS, facilityId, true))

efRequirement.selectFields(["productId", "quantityTotal", "supplierName", "price", "preferredOrderEnumId", "description"])
resultList = efRequirement.list().groupBy { [productId:it.productId, quantityTotal:it.quantityTotal] }
        .collect { k, v ->
            [productId:k.productId,
             quantityTotal:k.quantityTotal,
             supplierNames:v.sort { it.preferredOrderEnumId}
                     .sort {it.price}
                     .collect { it.supplierName+"("+it.price+")" }
                     .join(", "),
             description: v[0].description]
        }