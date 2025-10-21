import org.junit.jupiter.api.AfterAll
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.moqui.Moqui

@Suite
@SelectClasses([ ProductPriceTests.class, ReceiveAnafInvoiceTests.class ])
class LegacySuite {
    @AfterAll
    static void destroyMoqui() {
        Moqui.destroyActiveExecutionContextFactory()
    }
}
