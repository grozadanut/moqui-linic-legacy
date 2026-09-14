package ro.flexbiz

import groovy.transform.CompileStatic
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.Nats
import io.nats.client.Options
import org.moqui.context.ExecutionContextFactory
import org.moqui.context.ToolFactory
import org.moqui.util.SystemBinding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ro.flexbiz.util.commons.StringUtils

import java.nio.charset.StandardCharsets
import java.time.Duration

@CompileStatic
class NatsToolFactory implements ToolFactory<NatsToolFactory> {
    protected final static Logger logger = LoggerFactory.getLogger(NatsToolFactory.class)
    final static String TOOL_NAME = "NATS"

    private Connection nc

    NatsToolFactory () { }

    @Override
    String getName() { return TOOL_NAME }

    @Override
    void init(ExecutionContextFactory ecf) {
        logger.info("Initializing NATS.")
        final String natsUrl = SystemBinding.getPropOrEnv("natsUrl")

        if (StringUtils.isEmpty(natsUrl)) {
            logger.warn("'natsUrl' is empty, NOT activating NATS messaging service!")
            return
        }

        try {
            if (nc != null)
                nc.close()

            final Options options = new Options.Builder()
                    .server(natsUrl)
                    .maxReconnects(-1) // infinite
                    .noEcho() // don't send message back to me
                    .build()
            nc = Nats.connect(options)
        } catch (final Exception e) {
            logger.error(e.getMessage(), e)
        }
    }

    @Override
    NatsToolFactory getInstance(Object... parameters) {
        return this
    }

    @Override
    void destroy() {
        try {
            if (nc != null)
                nc.close()
        } catch (final InterruptedException e) {
            logger.error(e.getMessage(), e)
        }
    }

    void sendMessage(final String tenantId, final String subject, final String body) {
        if (nc == null)
            return

        try {
            nc.publish(tenantId+"."+subject, body == null ? null : body.getBytes(StandardCharsets.UTF_8))
        } catch (final Exception e) {
            logger.error(e.getMessage(), e)
        }
    }

    Optional<Message> requestReply(final String tenantId, final String subject, final String body, final Duration timeout) {
        if (nc == null)
            return Optional.empty()

        try {
            return Optional.ofNullable(nc.request(tenantId+"."+subject, body == null ? null : body.getBytes(StandardCharsets.UTF_8), timeout))
        } catch (final Exception e) {
            logger.error(e.getMessage(), e)
            return Optional.empty()
        }
    }
}
