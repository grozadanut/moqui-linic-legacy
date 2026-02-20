package ro.colibri.legacy.service.ui

import com.google.common.collect.ImmutableList
import org.moqui.context.ExecutionContext
import org.moqui.service.ServiceException
import ro.colibri.entities.comercial.AccountingDocument
import ro.colibri.entities.comercial.Document
import ro.colibri.entities.comercial.Gestiune
import ro.colibri.entities.comercial.Operatiune
import ro.colibri.legacy.service.LegacySyncServices
import ro.colibri.util.InvocationResult
import ro.colibri.util.NumberUtils

import java.text.MessageFormat
import java.time.LocalDate

ExecutionContext ec = context.ec

if (channel.channelId.equalsIgnoreCase("transfer")) {
    if (!otherGestiuneId)
        throw new ServiceException("otherGestiuneId Missing!")

    Long docId = null
    ImmutableList<Gestiune> allGestiuni = LegacySyncServices.allGestiuni()
    Gestiune gestiune = allGestiuni.stream().filter(gest -> gest.getImportName().equalsIgnoreCase(facilityId))
            .findFirst().orElseThrow {new ServiceException("Gestiunea cu importName ${facilityId} nu a fost gasita")}
    final BigDecimal tvaPercent = new BigDecimal(0.21)
    final BigDecimal tvaExtractDivisor = Operatiune.tvaExtractDivisor(tvaPercent)

    for (req in requirements) {
        final Operatiune op = new Operatiune()
        op.setBarcode(req.barcode)
        op.setCantitate(req.quantityTotal)
        op.setCategorie("MARFA")
        op.setGestiune(gestiune)
        op.setName(req.name)
        op.setPretUnitarAchizitieFaraTVA(req.lastBuyingPriceNoTva)
        op.setPretVanzareUnitarCuTVA(req.pricePerUom)
        Operatiune.updateAmounts(op, tvaPercent, tvaExtractDivisor)
        op.setUom(req.uom)
        op.setTipOp(Operatiune.TipOp.INTRARE)

        if (!docId) {
            String docNr = LegacySyncServices.autoNumber(Document.TipDoc.CUMPARARE, "BON TRANSFER", null)
            InvocationResult legacyResult = LegacySyncServices.addOperationToUnpersistedDoc(
                    Document.TipDoc.CUMPARARE, "BON TRANSFER", docNr, LocalDate.now(), docNr, LocalDate.now(),
                    NumberUtils.parseToLong(System.getProperty("OP_INTERNA_ID")),
                    false, op, otherGestiuneId)
            if (legacyResult.statusCanceled())
                throw new ServiceException(legacyResult.toTextDescription())
            docId = ((AccountingDocument) legacyResult.extra(InvocationResult.ACCT_DOC_KEY)).getId()
            transferId = docId

            ec.service.sync().name("UIServices.delete#Requirement")
                    .parameters([requirementId: "*"])
                    .parameters([facilityId: facilityId])
                    .parameters([requirementTypeEnumId: "RqTpInventory"])
                    .parameters([statusId: "RqmtStCreated"])
                    .parameters([productId: req.get("id")])
                    .call()
        } else {
            if (LegacySyncServices.addOperationToDoc(docId, op, otherGestiuneId).statusOk())
                ec.service.sync().name("UIServices.delete#Requirement")
                        .parameters([requirementId: "*"])
                        .parameters([facilityId: facilityId])
                        .parameters([requirementTypeEnumId: "RqTpInventory"])
                        .parameters([statusId: "RqmtStCreated"])
                        .parameters([productId: req.get("id")])
                        .call()
        }
    }
} else if (channel.channelId.equalsIgnoreCase("whatsapp")) {
    var args = [to: channel.chatId, content:
            requirements.stream()
            .map {MessageFormat.format("{0} \t {1} {2}", it.name, it.quantityTotal, it.uom)}
            .toList()
            .join(System.lineSeparator())]
    var whatsappResult = ec.service.sync().name("UIServices.send#WhatsappMessage")
            .parameters([args: args])
            .call()
    if (whatsappResult.get("success"))
        requirements.forEach {req ->
            ec.service.sync().name("UIServices.statusTo#Requirement")
                    .parameters([facilityId: facilityId])
                    .parameters([requirementTypeEnumId: "RqTpInventory"])
                    .parameters([productId: req.get("id")])
                    .parameters([description: supplier.organizationName])
                    .parameters([statusId: "RqmtStOrdered"])
                    .call()
        }
    else
        ec.message.addError("Eroare la trimiterea mesajului pe WhatsApp: "+whatsappResult.get("response"))
}