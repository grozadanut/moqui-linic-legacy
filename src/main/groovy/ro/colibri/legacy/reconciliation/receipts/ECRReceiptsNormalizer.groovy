package ro.colibri.legacy.reconciliation.receipts

import ro.flexbiz.util.commons.NumberUtils
import ro.flexbiz.util.commons.model.GenericValue
import ro.flexbiz.util.commons.reconciliation.Normalizer
import ro.flexbiz.util.commons.reconciliation.model.NormalizedRecord

import java.util.stream.Stream

class ECRReceiptsNormalizer implements Normalizer {
    @Override
    Stream<NormalizedRecord> normalize(Object o) {
        String receipts = (String) o

        List<NormalizedRecord> records = []
        String location = ""
        String operation = ""
        BigDecimal zTotal = BigDecimal.ZERO

        for(String line : receipts.lines()) {
            if (line.contains("JUDET BIHOR"))
                location += "start"
            if (line.contains("~ ~")) {
                location += "line"
                continue;
            }
            if (line.contains("SUBTOTAL"))
                location += "SUBTOTAL"
            if (line.contains("REDUCERE"))
                location += "REDUCERE"
            if (line.contains("CASIER 1"))
                location = ""

            if (location.equals("startline")) {
                operation += " "+line
                if (line.endsWith(" A")) { // this is the end of line
                    GenericValue fields = GenericValue.of()
                    String nameQuantityUom = operation.substring(0, operation.lastIndexOf("X")).trim()
                    String uom = nameQuantityUom.substring(nameQuantityUom.lastIndexOf(" ")).trim()
                    String nameQuantity = nameQuantityUom.substring(0, nameQuantityUom.lastIndexOf(uom)).trim()
                    String quantity = nameQuantity.substring(nameQuantity.lastIndexOf(" ")).trim()
                    String name = nameQuantity.substring(0, nameQuantity.lastIndexOf(quantity)).trim()
                    BigDecimal total = NumberUtils.parse(operation.substring(operation.indexOf("=")+1, operation.length()-1).trim())
                    BigDecimal pricePerUom = NumberUtils.parse(operation.substring(operation.lastIndexOf("X")+1, operation.indexOf("=")))

                    fields.put("pricePerUom", pricePerUom)
                    fields.put("total", total)
                    fields.put("uom", uom)
                    fields.put("quantity", NumberUtils.parse(quantity))
                    fields.put("name", name)
                    records.add(new NormalizedRecord(operation, fields))
                    operation = ""
                }
            }

            if (location.equals("startlineSUBTOTALREDUCERE")) {
                BigDecimal discountTotal = NumberUtils.parse(line.replace("REDUCERE", "").trim())
                records.add(new NormalizedRecord(discountTotal, GenericValue.of("total", discountTotal,
                        "name", "REDUCERE", "type", "DISC")))
            }

            if (line.contains("TOTAL LEI")) {
                BigDecimal receiptTotal = NumberUtils.parse(line.replace("TOTAL LEI", "").trim())
                zTotal = NumberUtils.add(zTotal, receiptTotal)
            }
        }
        records.add(new NormalizedRecord(o, GenericValue.of("total", zTotal, "name", "Z",
                "type", "Z")))
        return records.stream()
    }
}