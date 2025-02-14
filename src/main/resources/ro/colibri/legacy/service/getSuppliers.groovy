package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

EntityFind ef = ec.entity.find("mantle.party.FindPartyView").distinct(true)

ef.selectFields(["partyId", "organizationName"])

ef.condition("partyTypeEnumId", "PtyOrganization")
ef.condition("roleTypeId", "Supplier")

resultList = []
resultList.addAll(ef.list())