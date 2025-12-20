import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification

class FindProductSuppliersTests extends Specification {
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

    def "only supplier"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-1", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-1", organizationName: "Test party"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-1", productName: "Only supplier"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-1", productId: "FPST-P-1",
                             vendorPartyId: "L1", customerPartyId: "FPST-Party-1",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-1"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productPriceId") == "FPST-PP-1"
            get("productId") == "FPST-P-1"
            get("supplierId") == "L1"
            get("supplierName") == "Linic"
        }

        cleanup:
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-1"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-1"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-1"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-1"])
                .call()
    }

    def "only pareto"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-2", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-2", organizationName: "Test party"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-2", productName: "Only pareto"])
                .call()
        ec.service.sync().name("store#ProductCategory")
                .parameters([productCategoryId: "FPST-Party-2A", productCategoryTypeEnumId: "PctPareto",
                             ownerPartyId: "FPST-Party-2"])
                .call()
        ec.service.sync().name("store#ProductCategoryMember")
                .parameters([productCategoryId: "FPST-Party-2A", productId: "FPST-P-2"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-2"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productId") == "FPST-P-2"
            get("pareto") == "FPST-Party-2A"
        }

        cleanup:
        ec.service.sync().name("delete#ProductCategoryMember")
                .parameters([productCategoryId: "FPST-Party-2A", productId: "FPST-P-2", fromDate: "*"])
                .call()
        ec.service.sync().name("delete#ProductCategory")
                .parameters([productCategoryId: "FPST-Party-2A"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-2"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-2"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-2"])
                .call()
    }

    def "only minimum stock"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-3", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-3", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FPST-Party-3", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FPST-Party-3"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-3", productName: "Only minimum stock"])
                .call()
        ec.service.sync().name("store#ProductFacility")
                .parameters([productId: "FPST-P-3",
                             facilityId: "FPST-Party-3", minimumStock: "3"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-3"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productId") == "FPST-P-3"
            get("minimumStock").toString() == "3"
        }

        cleanup:
        ec.service.sync().name("delete#ProductFacility")
                .parameters([productId: "FPST-P-3", facilityId: "FPST-Party-3"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-3"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FPST-Party-3"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-3"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-3"])
                .call()
    }

    def "all supplier, pareto and stock minim"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-4", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-4", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FPST-Party-4", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FPST-Party-4"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-4", productName: "All"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-4", productId: "FPST-P-4",
                             vendorPartyId: "L1", customerPartyId: "FPST-Party-4",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductCategory")
                .parameters([productCategoryId: "FPST-Party-4A", productCategoryTypeEnumId: "PctPareto",
                             ownerPartyId: "FPST-Party-4"])
                .call()
        ec.service.sync().name("store#ProductCategoryMember")
                .parameters([productCategoryId: "FPST-Party-4A", productId: "FPST-P-4"])
                .call()
        ec.service.sync().name("store#ProductFacility")
                .parameters([productId: "FPST-P-4",
                             facilityId: "FPST-Party-4", minimumStock: "4"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-4"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productPriceId") == "FPST-PP-4"
            get("productId") == "FPST-P-4"
            get("supplierId") == "L1"
            get("supplierName") == "Linic"
            get("pareto") == "FPST-Party-4A"
            get("minimumStock").toString() == "4"
        }

        cleanup:
        ec.service.sync().name("delete#ProductFacility")
                .parameters([productId: "FPST-P-4", facilityId: "FPST-Party-4"])
                .call()
        ec.service.sync().name("delete#ProductCategoryMember")
                .parameters([productCategoryId: "FPST-Party-4A", productId: "FPST-P-4", fromDate: "*"])
                .call()
        ec.service.sync().name("delete#ProductCategory")
                .parameters([productCategoryId: "FPST-Party-4A"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-4"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-4"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FPST-Party-4"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-4"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-4"])
                .call()
    }

    def "both supplier and stock minim"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-5", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-5", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FPST-Party-5", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FPST-Party-5"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-5"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-5", productId: "FPST-P-5",
                             vendorPartyId: "L1", customerPartyId: "FPST-Party-5",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductFacility")
                .parameters([productId: "FPST-P-5",
                             facilityId: "FPST-Party-5", minimumStock: "5"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-5"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productPriceId") == "FPST-PP-5"
            get("productId") == "FPST-P-5"
            get("supplierId") == "L1"
            get("supplierName") == "Linic"
            get("minimumStock").toString() == "5"
        }

        cleanup:
        ec.service.sync().name("delete#ProductFacility")
                .parameters([productId: "FPST-P-5", facilityId: "FPST-Party-5"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-5"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-5"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FPST-Party-5"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-5"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-5"])
                .call()
    }

    def "multiple suppliers and minimum stock"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-6", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-6", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FPST-Party-6", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FPST-Party-6"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-6"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-6-1", productId: "FPST-P-6",
                             vendorPartyId: "L1", customerPartyId: "FPST-Party-6",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-6-2", productId: "FPST-P-6",
                             vendorPartyId: "L2", customerPartyId: "FPST-Party-6",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductFacility")
                .parameters([productId: "FPST-P-6",
                             facilityId: "FPST-Party-6", minimumStock: "6"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-6"])
                .call()
                .get("resultList")

        with ((Map) result.sort().get(0)) {
            get("productPriceId") == "FPST-PP-6-1"
            get("productId") == "FPST-P-6"
            get("supplierId") == "L1"
            get("supplierName") == "Linic"
            get("minimumStock").toString() == "6"
        }
        with ((Map) result.sort().get(1)) {
            get("productPriceId") == "FPST-PP-6-2"
            get("productId") == "FPST-P-6"
            get("supplierId") == "L2"
            get("supplierName") == "Colibri"
            get("minimumStock").toString() == "6"
        }

        cleanup:
        ec.service.sync().name("delete#ProductFacility")
                .parameters([productId: "FPST-P-6", facilityId: "FPST-Party-6"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-6-1"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-6-2"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-6"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FPST-Party-6"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-6"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-6"])
                .call()
    }

    def "dont return sale price, only purchase price"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-7", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-7", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FPST-Party-7", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FPST-Party-7"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-7"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-7", productId: "FPST-P-7",
                             vendorPartyId: "L1", customerPartyId: "FPST-Party-7",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-7-2", productId: "FPST-P-7",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductFacility")
                .parameters([productId: "FPST-P-7",
                             facilityId: "FPST-Party-7", minimumStock: "7"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-7"])
                .call()
                .get("resultList")

        result.size() == 1
        with ((Map) result.get(0)) {
            get("productPriceId") == "FPST-PP-7"
            get("productId") == "FPST-P-7"
            get("supplierId") == "L1"
            get("supplierName") == "Linic"
            get("minimumStock").toString() == "7"
        }

        cleanup:
        ec.service.sync().name("delete#ProductFacility")
                .parameters([productId: "FPST-P-7", facilityId: "FPST-Party-7"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-7"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-7-2"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-7"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FPST-Party-7"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-7"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-7"])
                .call()
    }

    def "should return min stock when supplier pricing is for another facility"() {
        given:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "FPST-Party-8", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "FPST-Party-8", organizationName: "Test party"])
                .call()
        ec.service.sync().name("store#Facility")
                .parameters([facilityId: "FPST-Party-8", facilityTypeEnumId: "FcTpWarehouse",
                             ownerPartyId: "FPST-Party-8"])
                .call()

        ec.service.sync().name("store#Product")
                .parameters([productId: "FPST-P-8"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "FPST-PP-8", productId: "FPST-P-8",
                             vendorPartyId: "L2", customerPartyId: "L1",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase"])
                .call()
        ec.service.sync().name("store#ProductFacility")
                .parameters([productId: "FPST-P-8",
                             facilityId: "FPST-Party-8", minimumStock: "8"])
                .call()

        expect:
        List result = ec.service.sync()
                .name("LegacyServices.find#ProductSuppliers")
                .parameters([organizationPartyId: "FPST-Party-8"])
                .call()
                .get("resultList")

        with ((Map) result.get(0)) {
            get("productPriceId") == null
            get("productId") == "FPST-P-8"
            get("supplierId") == null
            get("supplierName") == null
            get("minimumStock").toString() == "8"
        }

        cleanup:
        ec.service.sync().name("delete#ProductFacility")
                .parameters([productId: "FPST-P-8", facilityId: "FPST-Party-8"])
                .call()
        ec.service.sync().name("delete#ProductPrice")
                .parameters([productPriceId: "FPST-PP-8"])
                .call()
        ec.service.sync().name("delete#Product")
                .parameters([productId: "FPST-P-8"])
                .call()
        ec.service.sync().name("delete#Facility")
                .parameters([facilityId: "FPST-Party-8"])
                .call()
        ec.service.sync().name("delete#Organization")
                .parameters([partyId: "FPST-Party-8"])
                .call()
        ec.service.sync().name("delete#Party")
                .parameters([partyId: "FPST-Party-8"])
                .call()
    }
}
