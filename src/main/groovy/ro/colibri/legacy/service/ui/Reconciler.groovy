package ro.colibri.legacy.service.ui

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.prowidesoftware.swift.io.parser.SwiftParser
import com.prowidesoftware.swift.io.writer.FINWriterVisitor
import com.prowidesoftware.swift.model.SwiftMessage
import com.prowidesoftware.swift.model.mt.mt9xx.MT940
import org.moqui.context.ExecutionContext
import ro.colibri.entities.comercial.AccountingDocument
import ro.colibri.entities.comercial.Document
import ro.colibri.entities.comercial.Operatiune
import ro.colibri.legacy.reconciliation.*
import ro.colibri.legacy.reconciliation.receipts.ECRReceiptsNormalizer
import ro.colibri.legacy.reconciliation.receipts.OperatiuneNormalizer
import ro.colibri.legacy.reconciliation.receipts.ReceiptAnalyzer
import ro.colibri.legacy.reconciliation.receipts.ReceiptMatcher
import ro.colibri.legacy.service.LegacySyncServices
import ro.colibri.util.InvocationResult
import ro.flexbiz.util.commons.LocalDateUtils
import ro.flexbiz.util.commons.NumberUtils
import ro.flexbiz.util.commons.reconciliation.ReconciliationEngine
import ro.flexbiz.util.commons.reconciliation.model.Index

import java.math.RoundingMode
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

    static Map<String, Object> reconcileReceipts(ExecutionContext ec) {
        ImmutableList<Operatiune> ops = LegacySyncServices.filteredOperations(Operatiune.TipOp.IESIRE, null,
                ec.context.date, ec.context.date, ImmutableSet.of(AccountingDocument.BON_CASA_NAME), null,
                null, null, null, (Integer) ec.context.gestiuneId, (Integer) null, null,
                10000, ro.colibri.util.LocalDateUtils.POSTGRES_MIN.toLocalDate(),
                ro.colibri.util.LocalDateUtils.POSTGRES_MAX.toLocalDate());

        List<Object> rightData = new ArrayList<>();
        rightData.addAll(ops);
        rightData.add(ops.stream()
                .map { NumberUtils.add(it.getValoareVanzareTVA(), it.getValoareVanzareFaraTVA())}
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO));

        return Map.of("resultList", ReconciliationEngine.defaults()
                .leftNormalizer(new ECRReceiptsNormalizer())
                .rightNormalizer(new OperatiuneNormalizer())
                .indexer {Set.of(new Index("name", it.getFields().get("name")),
                        new Index("nameQuantity", it.getFields().get("name")+
                                it.getFields().getBigDecimal("quantity")
                                        .setScale(4, RoundingMode.HALF_EVEN).toString()))}
                .matcher(new ReceiptMatcher())
                .analyzer(new ReceiptAnalyzer())
                .reconcile(List.of(ec.context.receipts), rightData))
    }
}
