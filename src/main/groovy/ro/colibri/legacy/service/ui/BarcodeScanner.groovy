package ro.colibri.legacy.service.ui


import org.moqui.context.ExecutionContext
import ro.colibri.util.GsonUtils
import ro.flexbiz.NatsToolFactory
import ro.flexbiz.util.commons.StringUtils

class BarcodeScanner {
    static Map<String, Object> pushBarcodeToPC(ExecutionContext ec) {
        NatsToolFactory nc = ec.getTool(NatsToolFactory.TOOL_NAME, NatsToolFactory.class)
        String channel = ec.user.getPreference('SCANNER_CHANNEL')
        if (!StringUtils.isEmpty(channel))
            nc.sendMessage(channel, "scanner.barcode",
                    GsonUtils.get().toJson(Map.of("pseudoId", ec.context.pseudoId, "quantity", ec.context.quantity)))
        return Map.of()
    }
}
