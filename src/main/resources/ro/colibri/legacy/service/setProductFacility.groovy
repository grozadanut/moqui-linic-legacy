package ro.colibri.legacy.service

import org.moqui.context.ExecutionContext

ExecutionContext ec = context.ec

for (i in items) {
    ec.getEntity().makeValue("ProductFacility")
            .set("productId", i.productId)
            .set("facilityId", i.facilityId)
            .set("minimumStock", i.minimumStock)
            .createOrUpdate()
}