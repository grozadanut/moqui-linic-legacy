package ui

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification

class FindRequirementsTests extends Specification {
    @Shared
    ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginAnonymousIfNoUser()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def setup() {
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.artifactExecution.enableAuthz()
    }

    def "should sum totals for RqmtStCreated requirements"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FRT-Party-1", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FRT-Party-1", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FRT-Party-1", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FRT-Party-1"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FRT-P-1", productName: "Only requirements"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-1-1", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStProposed",
                             facilityId: "FRT-Party-1", productId:"FRT-P-1", quantity: "1"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-1-2", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStCreated",
                             facilityId: "FRT-Party-1", productId:"FRT-P-1", quantity: "10"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-1-22", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStCreated",
                             facilityId: "FRT-Party-1", productId:"FRT-P-1", quantity: "2"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-1-3", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStApproved",
                             facilityId: "FRT-Party-1", productId:"FRT-P-1", quantity: "100"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-1-4", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStOrdered",
                             facilityId: "FRT-Party-1", productId:"FRT-P-1", quantity: "1000"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-1-IGNORE", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStOrdered",
                             facilityId: "L2", productId:"FRT-P-1", quantity: "5"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("UIServices.find#Requirements")
                .parameters([facilityId: "FRT-Party-1", statusId: "RqmtStCreated"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productId") == "FRT-P-1"
            get("requiredQuantityTotal").toString() == "12"
        }

        cleanup:
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-1-1"])
                .call()
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-1-2"])
                .call()
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-1-22"])
                .call()
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-1-3"])
                .call()
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-1-4"])
                .call()
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-1-IGNORE"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FRT-P-1"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FRT-Party-1"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FRT-Party-1"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FRT-Party-1"])
                .call()
    }

    def "with multiple suppliers"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FRT-Party-2", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FRT-Party-2", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FRT-Party-2", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FRT-Party-2"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FRT-P-2", productName: "Multiple suppliers"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FRT-PP-6-1", productId: "FRT-P-2",
                             vendorPartyId: "L1", customerPartyId: "FRT-Party-2",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase",
                             preferredOrderEnumId: "SpoMain", price: "10"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FRT-PP-6-2", productId: "FRT-P-2",
                             vendorPartyId: "L2", customerPartyId: "FRT-Party-2",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase",
                             preferredOrderEnumId: "SpoAlternate", price: "20"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FRT-PP-6-3", productId: "FRT-P-2",
                             vendorPartyId: "L1", customerPartyId: "FRT-Party-2",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase",
                             preferredOrderEnumId: "SpoAlternate", price: "25"])
                .call()
        ec.service.sync().name("store#Requirement")
                .parameters([requirementId: "FRT-REQ-2-1", requirementTypeEnumId: "RqTpInventory", statusId: "RqmtStCreated",
                             facilityId: "FRT-Party-2", productId:"FRT-P-2", quantity: "2"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("UIServices.find#Requirements")
                .parameters([facilityId: "FRT-Party-2", statusId: "RqmtStCreated"])
                .call()
                .get("resultList")

        result.size() == 1
        with ((Map) result.get(0)) {
            get("productId") == "FRT-P-2"
            get("requiredQuantityTotal").toString() == "2"
            get("supplierNames").toString() == "Linic(10), Colibri(20), Linic(25)"
        }

        cleanup:
        ec.service.sync().name("delete#Requirement")
                .parameters([requirementId: "FRT-REQ-2-1"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FRT-PP-6-1"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FRT-PP-6-2"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FRT-PP-6-3"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FRT-P-2"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FRT-Party-2"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FRT-Party-2"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FRT-Party-2"])
                .call()
    }
}
