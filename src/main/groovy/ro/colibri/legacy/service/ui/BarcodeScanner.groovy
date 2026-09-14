package ro.colibri.legacy.service.ui

import io.nats.client.Message
import org.moqui.context.ExecutionContext
import ro.colibri.util.GsonUtils
import ro.flexbiz.NatsToolFactory
import ro.flexbiz.util.commons.StringUtils

import java.nio.charset.StandardCharsets
import java.time.Duration

class BarcodeScanner {
    static Map<String, Object> pushBarcodeToPC(ExecutionContext ec) {
        NatsToolFactory nc = ec.getTool(NatsToolFactory.TOOL_NAME, NatsToolFactory.class)
        String channel = ec.user.getPreference('SCANNER_CHANNEL')
        if (StringUtils.isEmpty(channel))
            return Map.of()

        Optional<Message> response = nc.requestReply(channel, "scanner.barcode",
                GsonUtils.get().toJson(Map.of("pseudoId", ec.context.pseudoId, "quantity", ec.context.quantity)),
                Duration.ofSeconds(30))
        if (response.isEmpty())
            return Map.of()

        Map result = GsonUtils.get().fromJson(new String(response.get().getData(), StandardCharsets.UTF_8), Map.class)
        if (result.error != null && !StringUtils.isEmpty(result.error))
            ec.message.addError(result.error)
        return result
    }
}
