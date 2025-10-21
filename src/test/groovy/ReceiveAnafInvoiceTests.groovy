import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class ReceiveAnafInvoiceTests extends Specification {
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
                .parameters([systemMessageId: "12345"])
                .call()
        ec.service.sync().name("delete#moqui.service.message.SystemMessage")
                .parameters([systemMessageId: "12346"])
                .call()

        ec.artifactExecution.enableAuthz()
        ec.transaction.commit()
    }

    def "received invoice"() {
        setup:
        var rawXml = Files.readString(Paths.get("src", "test", "resources", "invoice.xml"))

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: "12345", messageDate: "2024-01-25",
                             messageText: rawXml])
                .call()

        def msg = ec.entity.find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "12345")
                .one()

        then:
        msg.systemMessageTypeId == "ANAFReceivedInvoice"
        msg.statusId == "SmsgConsumed"
        msg.isOutgoing == "N"
        msg.messageText == rawXml
        msg.senderId == "RO14998343"
        msg.receiverId == "RO14998343"
        msg.messageId == "LINDL2-623"
        msg.messageDate.toString() == "2024-01-25 00:00:00.0"
        msg.docType == "380"
        msg.docSubType == "Invoice"
        msg.docControl == "1.99"
    }

    def "received credit note"() {
        setup:
        var rawXml = Files.readString(Paths.get("src", "test", "resources", "credit_note.xml"))

        when:
        ec.service.sync().name("AnafServices.receive#AnafInvoice")
                .parameters([systemMessageId: "12346", messageDate: "2024-01-31",
                             messageText: rawXml])
                .call()

        def msg = ec.entity.find("moqui.service.message.SystemMessage")
                .condition("systemMessageId", "12346")
                .one()

        then:
        msg.systemMessageTypeId == "ANAFReceivedInvoice"
        msg.statusId == "SmsgConsumed"
        msg.isOutgoing == "N"
        msg.messageText == rawXml
        msg.senderId == "RO7568475"
        msg.receiverId == "RO14998343"
        msg.messageId == "FBON01"
        msg.messageDate.toString() == "2024-01-31 00:00:00.0"
        msg.docType == "381"
        msg.docSubType == "CreditNote"
        msg.docControl == "-526.91"
    }
}
