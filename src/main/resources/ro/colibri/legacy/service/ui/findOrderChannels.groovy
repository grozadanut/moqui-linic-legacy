package ro.colibri.legacy.service.ui

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityFind

ExecutionContext ec = context.ec

EntityFind ef = ec.entity.find("mantle.party.contact.PartyContactMechTelecomNumber").distinct(true)
ef.selectFields(["contactNumber", "countryCode"])
ef.condition("partyId", supplierId)
ef.condition("contactMechPurposeId", "PhoneShippingOrigin")

resultList = []
resultList.add([channelId: "print", channelName: "Printeaza"])
for (tel in ef.list()) {
    resultList.add([channelId: "whatsapp", channelName: "WhatsApp", phone: "+"+tel.countryCode+tel.contactNumber])
}

if (ec.entity.find("mantle.party.PartyRole")
        .condition("partyId", supplierId)
        .condition("roleTypeId", "OrgInternal")
        .one() != null)
    resultList.add([channelId: "transfer", channelName: "Transfera"])