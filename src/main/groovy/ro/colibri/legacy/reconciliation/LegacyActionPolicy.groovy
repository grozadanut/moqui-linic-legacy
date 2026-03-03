package ro.colibri.legacy.reconciliation

import ro.colibri.entities.comercial.Document
import ro.flexbiz.util.commons.model.GenericValue
import ro.flexbiz.util.commons.reconciliation.ActionPolicy
import ro.flexbiz.util.commons.reconciliation.model.Action
import ro.flexbiz.util.commons.reconciliation.model.ActionType
import ro.flexbiz.util.commons.reconciliation.model.IdentityStatus
import ro.flexbiz.util.commons.reconciliation.model.ReconciliationResult
import ro.flexbiz.util.commons.reconciliation.model.ReconciliationStatus

class LegacyActionPolicy implements ActionPolicy {
    @Override
    Action resolve(ReconciliationResult result) {
        if (result.match.status == IdentityStatus.CONFIRMED) {
            // commissions
            // IF line starts with ignore case 'COMIS' THEN doc.gestiune=gestiuneId | Partner='RAIFF gestiune.name.upper' | TipDoc.PLATA | doc=CARD | docNr=NC | date=closingDate | doc.name=COM | doc.total=sum(lines) | banca=contBancarId
            if (result.match.signals[0]?.fieldKey == "commissions" &&
                    result.status == ReconciliationStatus.LEFT_ONLY) {
                return new Action(result, ActionType.CREATE_RECORD, "createAccDoc", GenericValue.of(
                        [gestiuneId: result.match.left[0].fields.gestiuneId,
                         partnerName: result.match.left[0].fields.partnerName,
                         tipDoc: Document.TipDoc.PLATA, doc: "CARD", nrDoc: "NC", name: "COM",
                         dataDoc: result.match.left[0].fields.dataDoc,
                         contBancarId: result.match.left[0].fields.contBancarId,
                         total: result.match.left.stream()
                                 .map {it.fields.getBigDecimal("total")}
                                 .reduce(BigDecimal::add)
                                 .orElse(BigDecimal.ZERO)]))
            }
        }
        else if (result.match.status == IdentityStatus.PROBABLE) {
            // pos incoming payments
            // ELSE IF line starts with 'AK' AND contains 'POS'
            //   THEN doc.gestiune=gestiuneId | Partner='CARD INCASARE' | TipDoc.INCASARE | doc='CARD' | docNr='NC' | date=line.date | doc.name='INC' | doc.total=line.total | banca=contBancarId
            //   OR doc.gestiune=gestiuneId | Partner not 'CARD INCASARE' or 'RAIFF...' | TipDoc.INCASARE | doc='CARD' | date=line.date |  doc.total=line.total | banca=contBancarId
            if (result.match.signals[0]?.fieldKey == "byTotals" && result.status == ReconciliationStatus.LEFT_ONLY &&
                    result.match.left.size() == 1 && result.match.left[0].fields.partnerName == "CARD INCASARE" &&
                    result.match.left[0].fields.tipDoc == Document.TipDoc.INCASARE &&
                    result.match.left[0].fields.doc == "CARD" &&
                    result.match.left[0].fields.nrDoc == "NC" &&
                    result.match.left[0].fields.name == "INC") {
                return new Action(result, ActionType.CREATE_RECORD, "createAccDoc", GenericValue.of(
                        [gestiuneId: result.match.left[0].fields.gestiuneId,
                         partnerName: "CARD INCASARE",
                         tipDoc: Document.TipDoc.INCASARE, doc: "CARD", nrDoc: "NC", name: "INC",
                         dataDoc: result.match.left[0].fields.dataDoc,
                         contBancarId: result.match.left[0].fields.contBancarId,
                         total: result.match.left[0].fields.total]))
            }
        }
        return new Action(result, ActionType.IGNORE, null, null)
    }
}
