package ro.colibri.legacy.service.ui

import com.prowidesoftware.swift.io.parser.SwiftParser
import com.prowidesoftware.swift.io.writer.FINWriterVisitor
import com.prowidesoftware.swift.model.SwiftMessage
import com.prowidesoftware.swift.model.mt.mt9xx.MT940
import org.moqui.context.ExecutionContext
import ro.colibri.legacy.reconciliation.*
import ro.colibri.legacy.service.LegacySyncServices
import ro.colibri.util.InvocationResult
import ro.flexbiz.util.commons.LocalDateUtils
import ro.flexbiz.util.commons.reconciliation.ReconciliationEngine

import java.time.LocalDate

class Reconciler {
    static Map<String, Object> reconcile(ExecutionContext ec) {
        SwiftMessage sm = new SwiftMessage()
        sm.setBlock4(SwiftParser.parseBlock4("{4:" + ec.context.mt940 + FINWriterVisitor.SWIFT_EOL + "-}"))
        MT940 mt = new MT940(sm)

        if (mt.field61.empty)
            return Map.of()

        LocalDate openingDate = LocalDateUtils.toLocalDate(mt.field61[0].date()).withDayOfMonth(1)
        LocalDate closingDate = LocalDateUtils.toLocalDate(mt.field61.last().date())

        InvocationResult result = LegacySyncServices.regBanca(null, ec.context.contBancarId as Integer,
                openingDate, closingDate);

        return Map.of("resultList", ReconciliationEngine.defaults()
                .leftNormalizer(new MT940Normalizer(ec.context.gestiuneId as int, ec.context.contBancarId as int))
                .rightNormalizer(new AccDocNormalizer())
                .indexer(new ColibriIndexer())
                .matcher(new RowMatcher())
                .analyzer(new ColibriAnalyzer())
                .actionPolicy(new LegacyActionPolicy())
                .reconcile(List.of(mt), List.of(result)))
    }
}
