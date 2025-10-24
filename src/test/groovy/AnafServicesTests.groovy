import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import spock.lang.Shared
import spock.lang.Specification

class AnafServicesTests extends Specification {
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
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_2"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_3"])
                .call()
        ec.service.sync().name("delete#mantle.account.invoice.InvoiceSystemMessage")
                .parameters([systemMessageId: "AST_4", "invoiceId": "AST_INV_1"])
                .call()
        ec.service.sync().name("delete#mantle.account.invoice.Invoice")
                .parameters(["invoiceId": "AST_INV_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_4"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_1_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_2_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_3_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "AST_4_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "ASTL_5_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "ASTL_5"])
                .call()

        ec.artifactExecution.enableAuthz()
    }

    def "receive#AnafInvoice error when statusId not SmsgConsumed"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_1")
        msg.set("statusId", "SmsgReceived")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoice")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: "AST_1"])
                .call()

        then:
        ec.message.getErrorsString() == "System message [AST_1] has status [SmsgReceived] and must be SmsgConsumed, not receiving.\n"
        ec.message.clearErrors()
    }

    def "receive#AnafInvoice error when systemMessageTypeId not ANAFReceivedInvoice"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_2")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: "AST_2"])
                .call()

        then:
        ec.message.getErrorsString() == "System message [AST_2] has message type [ANAFReceivedInvoiceLine] and must be ANAFReceivedInvoice, not receiving.\n"
        ec.message.clearErrors()
    }

    def "receive#AnafInvoice status to SmsgConfirmed when success"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_3")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoice")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: "AST_3"])
                .call()

        then:
        ec.message.errors.size() == 0
        ec.getEntity().find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "AST_3")
                .one()
                .get("statusId") == "SmsgConfirmed"
    }

    def "receive#AnafInvoice connect to invoice id"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_4")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoice")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: "AST_4"])
                .parameters([invoiceId: "AST_INV_1"])
                .call()

        then:
        ec.message.errors.size() == 0
        ec.getEntity().find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "AST_4")
                .one()
                .get("statusId") == "SmsgConfirmed"
        ec.getEntity().find("mantle.account.invoice.InvoiceSystemMessage")
                .condition("systemMessageId", "AST_4")
                .condition("invoiceId", "AST_INV_1")
                .one() != null
    }

    def "receive#AnafInvoiceLine error when statusId not SmsgConsumed"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_1_1")
        msg.set("statusId", "SmsgReceived")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoiceLine")
                .parameters([systemMessageId: "AST_1_1"])
                .call()

        then:
        ec.message.getErrorsString() == "System message [AST_1_1] has status [SmsgReceived] and must be SmsgConsumed, not receiving.\n"
        ec.message.clearErrors()
    }

    def "receive#AnafInvoiceLine error when systemMessageTypeId not ANAFReceivedInvoiceLine"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_2_1")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoice")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoiceLine")
                .parameters([systemMessageId: "AST_2_1"])
                .call()

        then:
        ec.message.getErrorsString() == "System message [AST_2_1] has message type [ANAFReceivedInvoice] and must be ANAFReceivedInvoiceLine, not receiving.\n"
        ec.message.clearErrors()
    }

    def "receive#AnafInvoiceLine status to SmsgConfirmed when success"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_3_1")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoiceLine")
                .parameters([systemMessageId: "AST_3_1"])
                .call()

        then:
        ec.message.errors.size() == 0
        ec.getEntity().find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "AST_3_1")
                .one()
                .get("statusId") == "SmsgConfirmed"
    }

    def "receive#AnafInvoiceLine update product supplier id"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "AST_4_1")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoiceLine")
                .parameters([systemMessageId: "AST_4_1"])
                .parameters([supplierId: "supplier1"])
                .parameters([productId: "59"])
                .parameters([facilityPartyId: "L2"])
                .parameters([supplierProductId: "HOL59320"])
                .parameters([supplierProductName: "HOLCIM CIMENT 40KG"])
                .call()

        then:
        ec.message.errors.size() == 0
        ec.getEntity().find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "AST_4_1")
                .one()
                .get("statusId") == "SmsgConfirmed"
        var price = ec.getEntity().find("mantle.product.ProductPrice")
                .condition("productPriceId", "supplier1_59_L2")
                .one()
        price != null
        price.get("otherPartyItemId") == "HOL59320"
        price.get("otherPartyItemName") == "HOLCIM CIMENT 40KG"
    }

    def "receive#AnafInvoiceLine mark invoice as received if last line"() {
        setup:
        EntityValue msg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        msg.set("systemMessageId", "ASTL_5")
        msg.set("statusId", "SmsgConsumed")
        msg.set("systemMessageTypeId", "ANAFReceivedInvoice")
        msg.set("isOutgoing", "N")
        msg.createOrUpdate()

        EntityValue lineMsg = ec.getEntity().makeValue("moqui.service.message.SystemMessage")
        lineMsg.set("systemMessageId", "ASTL_5_1")
        lineMsg.set("statusId", "SmsgConsumed")
        lineMsg.set("systemMessageTypeId", "ANAFReceivedInvoiceLine")
        lineMsg.set("isOutgoing", "N")
        lineMsg.set("parentMessageId", "ASTL_5")
        lineMsg.createOrUpdate()

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoiceLine")
                .parameters([systemMessageId: "ASTL_5_1"])
                .call()

        then:
        ec.message.errors.size() == 0
        ec.getEntity().find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "ASTL_5_1")
                .one()
                .get("statusId") == "SmsgConfirmed"
        ec.getEntity().find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "ASTL_5")
                .one()
                .get("statusId") == "SmsgConfirmed"
    }
}
