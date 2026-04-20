package ro.colibri.legacy.reconciliation.receipts

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import ro.colibri.entities.comercial.Operatiune
import ro.flexbiz.util.commons.NumberUtils
import ro.flexbiz.util.commons.model.GenericValue
import ro.flexbiz.util.commons.reconciliation.Normalizer
import ro.flexbiz.util.commons.reconciliation.model.NormalizedRecord

import java.time.LocalDate
import java.util.stream.Stream

class OperatiuneNormalizer implements Normalizer {
    @Override
    Stream<NormalizedRecord> normalize(Object o) {
        if (o instanceof BigDecimal)
            return Stream.of(new NormalizedRecord(o, GenericValue.of("total", o, "name", "Z",
                    "type", "Z")));

        Operatiune op = (Operatiune) o;
        GenericValue fields = GenericValue.of();
        fields.put("total", NumberUtils.add(op.getValoareVanzareFaraTVA(), op.getValoareVanzareTVA()));
        fields.put("totalVat", op.getValoareVanzareTVA());
        fields.put("totalNet", op.getValoareVanzareFaraTVA());
        fields.put("quantity", op.getCantitate());
        fields.put("name", op.getName());
        fields.put("uom", op.getUom());
        fields.put("pricePerUom", op.getPretVanzareUnitarCuTVA());
        if (NumberUtils.smallerThan(op.getTotal(), BigDecimal.ZERO))
            fields.put("type", "DISC");
        return Stream.of(new NormalizedRecord(op.getId(), fields));
    }
}