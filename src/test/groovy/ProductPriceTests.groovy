import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification

class ProductPriceTests extends Specification {

    @Shared
    ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def setup() {
        ec.artifactExecution.disableAuthz()
        ec.transaction.begin(null)
    }

    def cleanup() {
        ec.artifactExecution.enableAuthz()
        ec.transaction.commit()
    }

    def "base price for standard product"() {
        setup:
        ec.service.sync().name("store#Product")
                .parameters([productId: "TEST-PROD-1", productName: "Test Product"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-1", productId: "TEST-PROD-1", priceTypeEnumId: "PptList",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 29.99])
                .call()

        when:
        def price = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameter("productId", "TEST-PROD-1")
                .call()

        then:
        price.price == 29.99
    }

    def "promotional price"() {
        setup:
        ec.service.sync().name("store#Product")
                .parameters([productId: "TEST-PROD-2", productName: "Promotional Product"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-2-1", productId: "TEST-PROD-2", priceTypeEnumId: "PptList",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 100.00])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-2-2", productId: "TEST-PROD-2", priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 80.00])
                .call()

        when:
        def price = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-2"])
                .call()

        then:
        price.price == 80.00
        price.listPrice == 100.00
    }

    def "tiered pricing based on quantity"() {
        setup:
        ec.service.sync().name("store#Product")
                .parameters([productId: "TEST-PROD-3", productName: "Bulk Product"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-3-1", productId: "TEST-PROD-3", priceTypeEnumId: "PptList",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 10.00])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-3-2", productId: "TEST-PROD-3", priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 8.00,
                             minQuantity: 10])
                .call()

        when:
        def price1 = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-3", quantity: 5])
                .call()
        def price2 = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-3", quantity: 15])
                .call()

        then:
        price1.price == 10.00
        price2.price == 8.00
    }

    def "customer specific pricing"() {
        setup:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "VIP-CUST-1", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "VIP-CUST-1", organizationName: "VIP Customer"])
                .call()
        ec.service.sync().name("store#PartyRole")
                .parameters([partyId: "VIP-CUST-1", roleTypeId: "Customer"])
                .call()
        ec.service.sync().name("store#Product")
                .parameters([productId: "TEST-PROD-4", productName: "VIP Product"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-4-1", productId: "TEST-PROD-4", priceTypeEnumId: "PptList",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 100.00])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-4-2", productId: "TEST-PROD-4", priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 80.00,
                             customerPartyId: "VIP-CUST-1"])
                .call()

        when:
        def regularPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameter("productId", "TEST-PROD-4")
                .call()
        def vipPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-4", customerPartyId: "VIP-CUST-1"])
                .call()

        then:
        regularPrice.price == 100.00
        vipPrice.price == 80.00
    }

    def "both customer specific pricing and quantity promotion"() {
        setup:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "VIP-CUST-2", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "VIP-CUST-2", organizationName: "VIP Customer"])
                .call()
        ec.service.sync().name("store#PartyRole")
                .parameters([partyId: "VIP-CUST-2", roleTypeId: "Customer"])
                .call()
        ec.service.sync().name("store#Product")
                .parameters([productId: "TEST-PROD-5", productName: "VIP Product Quant"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-5-1", productId: "TEST-PROD-5", priceTypeEnumId: "PptList",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 100.00])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-5-2", productId: "TEST-PROD-5", priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 80.00,
                             customerPartyId: "VIP-CUST-2"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-5-3", productId: "TEST-PROD-5", priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 90.00,
                             minQuantity: 10])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "TEST-PROD-5-4", productId: "TEST-PROD-5", priceTypeEnumId: "PptCurrent",
                             pricePurposeEnumId: "PppPurchase", priceUomId: "RON", price: 70.00,
                             minQuantity: 20, customerPartyId: "VIP-CUST-2"])
                .call()

        when:
        def regularPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameter("productId", "TEST-PROD-5")
                .call()
        def quantityPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-5", quantity: 10])
                .call()
        def vipPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-5", quantity: 10, customerPartyId: "VIP-CUST-2"])
                .call()
        def vipQuantPrice = ec.service.sync().name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-5", quantity: 20, customerPartyId: "VIP-CUST-2"])
                .call()

        then:
        regularPrice.price == 100.00
        quantityPrice.price == 90.00
        vipPrice.price == 80.00
        vipQuantPrice.price == 70.00
    }

    def "base supplier purchase price"() {
        setup:
        ec.service.sync().name("store#Party")
                .parameters([partyId: "SUPPL-1", partyTypeEnumId: "PtyOrganization"])
                .call()
        ec.service.sync().name("store#Organization")
                .parameters([partyId: "SUPPL-1", organizationName: "Supplier"])
                .call()
        ec.service.sync().name("store#PartyRole")
                .parameters([partyId: "SUPPL-1", roleTypeId: "Supplier"])
                .call()
        ec.service.sync().name("store#Product")
                .parameters([productId: "TEST-PROD-6", productName: "Test Product to purchase"])
                .call()
        ec.service.sync().name("store#ProductPrice")
                .parameters([productPriceId: "SUPPL-1_TEST-PROD-6", productId: "TEST-PROD-6",
                             vendorPartyId: "SUPPL-1", customerPartyId: "L2",
                             priceTypeEnumId: "PptCurrent", pricePurposeEnumId: "PppPurchase",
                             price: 32.9, priceUomId: "RON"])
                .call()

        when:
        def price = ec.service.sync()
                .name("mantle.product.PriceServices.get#ProductPrice")
                .parameters([productId: "TEST-PROD-6", vendorPartyId: "SUPPL-1", customerPartyId: "L2"])
                .call()

        then:
        price.price == 32.9
    }
}