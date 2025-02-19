package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue

import java.text.MessageFormat

ExecutionContext ec = context.ec

resultList = []
for (i in items) {
    agrItems = ec.getEntity().find("AgreementItem")
            .condition("agreementItemTypeEnumId", "AitPurchase")
            .condition("productId", i.productId)
            .condition("agreementItemSeqId", i.productId)
            .list()

    for (item in agrItems) {
        agreement = item.findRelatedOne("Agreement", false, true)
        if (agreement?.get("agreementTypeEnumId") == "AgrProduct" &&
                agreement?.get("organizationPartyId") == i.organizationPartyId &&
                agreement?.get("otherRoleTypeId") == "Supplier")
            item.delete()
    }

    agreementId = MessageFormat.format("AgrSuppl_{0}_{1}", i.organizationPartyId, i.supplierId)
    final EntityValue agreement = ec.getEntity().makeValue("Agreement")
    agreement.set("agreementId", agreementId)
    agreement.set("agreementTypeEnumId", "AgrProduct")
    agreement.set("organizationPartyId", i.organizationPartyId)
    agreement.set("otherRoleTypeId", "Supplier")
    agreement.set("otherPartyId", i.supplierId)
    agreement.createOrUpdate()

    final EntityValue agreementItem = ec.getEntity().makeValue("AgreementItem")
    agreementItem.set("agreementId", agreementId)
    agreementItem.set("agreementItemTypeEnumId", "AitPurchase")
    agreementItem.set("productId", i.productId)
    agreementItem.set("agreementItemSeqId", i.productId)
    agreementItem.createOrUpdate()

    resultList.add(agreementItem)
}