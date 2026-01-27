package ro.colibri.legacy.service.ui

import org.moqui.context.ExecutionContext

ExecutionContext ec = context.ec

productList = ec.entity.find("mantle.product.ProductFindView")
        .condition("idValue", lookupId)
        .list()