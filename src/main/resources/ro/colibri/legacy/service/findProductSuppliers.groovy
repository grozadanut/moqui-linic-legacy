package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

EntityFind ef = ec.entity.find("AgreementItem").distinct(true)

dv = ef.makeEntityDynamicView()
dv.addMemberEntity("AI", "AgreementItem", null, null, null)
dv.addMemberEntity("AGR", "Agreement", "AI", null, ["agreementId":null])
dv.addMemberEntity("ORG", "Organization", "AGR", null, ["otherPartyId":"partyId"])
dv.addAlias("AI", "agreementItemTypeEnumId")
dv.addAlias("AI", "productId")
dv.addAlias("AGR", "agreementTypeEnumId")
dv.addAlias("AGR", "otherRoleTypeId")
dv.addAlias("AGR", "organizationPartyId")
dv.addAlias("AGR", "supplierId", "otherPartyId", null)
dv.addAlias("ORG", "supplierName", "organizationName", null)

ef.condition("agreementItemTypeEnumId", "AitPurchase")
ef.condition("productId", EntityCondition.IS_NOT_NULL, null)
ef.condition("agreementTypeEnumId", "AgrProduct")
ef.condition("otherRoleTypeId", "Supplier")
ef.condition("organizationPartyId", organizationPartyId)

ef.selectFields(["productId", "supplierId", "supplierName"])

resultList = []
resultList.addAll(ef.list())