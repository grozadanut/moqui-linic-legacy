import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class ExpandAnafInvoiceLinesTests extends Specification {
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
        ec.transaction.begin(null)
    }

    def cleanup() {
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "EAILT_1_427941"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "EAILT_2_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "EAILT_1"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "EAILT_2"])
                .call()

        ec.artifactExecution.enableAuthz()
        ec.transaction.commit()
    }

    def "expand received invoice"() {
        setup:
        var rawXml = Files.readString(Paths.get("src", "test", "resources", "invoice.xml"))
        ec.service.sync().name("AnafServices.consume#AnafInvoice")
                .parameters([systemMessageId: "EAILT_1", messageDate: "2024-01-25",
                             messageText: rawXml])
                .call()

        when:
        ec.service.sync().name("AnafServices.expand#AnafInvoiceLines")
                .parameters([systemMessageId: "EAILT_1"])
                .call()

        def msg = ec.entity.find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "EAILT_1_427941")
                .one()

        then:
        msg.systemMessageTypeId == "ANAFReceivedInvoiceLine"
        msg.statusId == "SmsgConsumed"
        msg.isOutgoing == "N"
        msg.parentMessageId == "EAILT_1"
        msg.messageText.toString().startsWith("<cac:InvoiceLine>")
    }

    def "expand received credit note"() {
        setup:
        var rawXml = Files.readString(Paths.get("src", "test", "resources", "credit_note.xml"))
        ec.service.sync().name("AnafServices.consume#AnafInvoice")
                .parameters([systemMessageId: "EAILT_2", messageDate: "2024-01-31",
                             messageText: rawXml])
                .call()

        when:
        ec.service.sync().name("AnafServices.expand#AnafInvoiceLines")
                .parameters([systemMessageId: "EAILT_2"])
                .call()

        def msg = ec.entity.find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "EAILT_2_1")
                .one()

        then:
        msg.systemMessageTypeId == "ANAFReceivedInvoiceLine"
        msg.statusId == "SmsgConsumed"
        msg.isOutgoing == "N"
        msg.parentMessageId == "EAILT_2"
        msg.messageText.toString().startsWith("<cac:CreditNoteLine>")
    }
}
