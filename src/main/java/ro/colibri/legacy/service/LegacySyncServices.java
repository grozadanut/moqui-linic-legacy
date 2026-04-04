package ro.colibri.legacy.service;

import com.google.common.collect.ImmutableList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.moqui.context.ExecutionContext;
import org.moqui.entity.EntityValue;
import org.moqui.service.ServiceException;
import ro.colibri.beans.ManagerBean;
import ro.colibri.beans.ManagerBeanRemote;
import ro.colibri.beans.VanzariBean;
import ro.colibri.beans.VanzariBeanRemote;
import ro.colibri.entities.comercial.*;
import ro.colibri.util.InvocationResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.StringUtils.isEmpty;

public class LegacySyncServices {
    public static Map<String, Object> syncAllProducts(ExecutionContext ec) {
        final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);

        for (final Product legacyProd : bean.allProducts()) {
            if (!legacyProd.getCategorie().equalsIgnoreCase("MARFA")) {
                continue;
            }

            EntityValue prod = ec.getEntity().makeValue("Product");
            final String legacyId = legacyProd.getId() + "";
            prod.set("productId", legacyId);
            prod.set("productTypeEnumId", "PtAsset");
            prod.set("productName", legacyProd.getName());
            prod.set("amountUomId", mapUom(legacyProd.getUom()));
            prod.set("assetTypeEnumId", "AstTpInventory");
            prod.set("assetClassEnumId", "AsClsInventoryFin");

            final EntityValue sku = ec.getEntity().makeValue("ProductIdentification");
            sku.set("productIdTypeEnumId", "PidtSku");
            sku.set("productId", legacyId);
            sku.set("idValue", legacyProd.getBarcode());

            // final EntityValue price =
            // ec.getEntity().makeValue("mantle.product.ProductPrice");
            // price.set("productId", legacyId);
            // price.set("productPriceId", "leg"+legacyId);
            // price.set("priceTypeEnumId", "PptList");
            // price.set("pricePurposeEnumId", "PppPurchase");
            // price.set("priceUomId", "RON");
            // price.set("price", legacyProd.getPricePerUom());
            //
            // final EntityValue purchPrice =
            // ec.getEntity().makeValue("mantle.product.ProductPrice");
            // purchPrice.set("productId", legacyId);
            // purchPrice.set("productPriceId", "purch"+legacyId);
            // purchPrice.set("priceTypeEnumId", "PptAverage");
            // purchPrice.set("pricePurposeEnumId", "PppPurchase");
            // purchPrice.set("priceUomId", "RON");
            // purchPrice.set("price", legacyProd.getLastBuyingPriceNoTva());

            prod.createOrUpdate();
            sku.createOrUpdate();
            // price.createOrUpdate();
            // purchPrice.createOrUpdate();
        }

        return Map.of();
    }

    private static String mapUom(final String uom) {
        switch (uom) {
            case "BAX":
            case "CUT":
            case "PAC":
            case "SET":
            case "SUL":
                return "OTH_ea";
            case "BUC":
            case "buc":
            case "LEI":
            case "PAL":
            case "PLACA":
            case "PER":
            case "PRET":
            case "RAND":
            case "SAC":
                return "OTH_ea";
            case "KG":
                return "WT_kg";
            case "KM":
                return "LEN_km";
            case "L":
                return "VLIQ_L";
            case "M":
            case "ML":
                return "LEN_m";
            case "MC":
                return "VDRY_m3";
            case "MP":
            case "mp":
            case "M2":
                return "AREA_m2";
            case "ORE":
                return "TF_hr";
            case "T":
            case "TO":
                return "WT_mt";
            default:
                throw new IllegalArgumentException("Unexpected value: " + uom);
        }
    }

    public static Map<String, Object> syncPartners(ExecutionContext ec) {
        int count = 0;
        final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);

        for (final Partner legacyP : bean.allPartners()) {
            boolean isPerson = isEmpty(legacyP.getCodFiscal());

            final EntityValue party = ec.getEntity().makeValue("Party");
            final String legacyId = legacyP.getId() + "";
            party.set("partyId", legacyId);
            party.set("partyTypeEnumId", isPerson ? "PtyPerson" : "PtyOrganization");
            party.set("disabled", legacyP.isActiv() ? "N" : "Y");
            party.createOrUpdate();

            if (isPerson) {
                String[] nameTokens = legacyP.getName().replaceFirst("^A - ", "")
                        .split(" ");
                String lastName = nameTokens.length > 0 ? nameTokens[0] : ""; // nume de familie
                String firstName = ""; // prenume
                for (int i = 1; i < nameTokens.length; i++) {
                    firstName += nameTokens[i] + " ";
                }
                firstName = firstName.trim();

                ec.getLogger().info("[syncPartners] Person #" + legacyId
                        + " | raw='" + legacyP.getName()
                        + "' -> lastName='" + lastName
                        + "', firstName='" + firstName + "'");

                final EntityValue pg = ec.getEntity().makeValue("Person");
                pg.set("partyId", legacyId);
                pg.set("firstName", firstName);
                pg.set("lastName", lastName);
                pg.set("nickname", legacyP.getName());
                pg.createOrUpdate();
            } else {
                ec.getLogger().info("[syncPartners] Organization #" + legacyId
                        + " | name='" + legacyP.getName() + "'");

                final EntityValue pg = ec.getEntity().makeValue("Organization");
                pg.set("partyId", legacyId);
                pg.set("organizationName", legacyP.getName());
                pg.createOrUpdate();
            }
            count++;

            // Cod Fiscal
            if (!isEmpty(legacyP.getCodFiscal())) {
                EntityValue partyIdentification = ec.getEntity().makeValue("mantle.party.PartyIdentification");
                partyIdentification.set("partyId", legacyId);
                partyIdentification.set("partyIdTypeEnumId", "PtidTaxId");
                partyIdentification.set("idValue", legacyP.getCodFiscal());
                partyIdentification.createOrUpdate();
            }

            // Reg Com
            if (!isEmpty(legacyP.getRegCom())) {
                EntityValue partyIdentification = ec.getEntity().makeValue("mantle.party.PartyIdentification");
                partyIdentification.set("partyId", legacyId);
                partyIdentification.set("partyIdTypeEnumId", "PtidTradeReg");
                partyIdentification.set("idValue", legacyP.getRegCom());
                partyIdentification.createOrUpdate();
            }

            // Phone
            if (!isEmpty(legacyP.getPhone())) {
                String phoneStr = legacyP.getPhone().split("[,;]")[0].trim();
                if (!isEmpty(phoneStr)) {
                    String contactMechId = legacyId + "_PHONE";
                    EntityValue contactMech = ec.getEntity().makeValue("mantle.party.contact.ContactMech");
                    contactMech.set("contactMechId", contactMechId);
                    contactMech.set("contactMechTypeEnumId", "CmtTelecomNumber");
                    contactMech.createOrUpdate();

                    EntityValue telecomNumber = ec.getEntity().makeValue("mantle.party.contact.TelecomNumber");
                    telecomNumber.set("contactMechId", contactMechId);
                    telecomNumber.set("contactNumber", phoneStr);
                    telecomNumber.createOrUpdate();

                    EntityValue partyContactMech = ec.getEntity().makeValue("mantle.party.contact.PartyContactMech");
                    partyContactMech.set("partyId", legacyId);
                    partyContactMech.set("contactMechId", contactMechId);
                    partyContactMech.set("contactMechPurposeId", "PhonePrimary");
                    partyContactMech.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)));
                    partyContactMech.createOrUpdate();
                }
            }

            // Email
            if (!isEmpty(legacyP.getEmail())) {
                String contactMechId = legacyId + "_EMAIL";
                EntityValue contactMech = ec.getEntity().makeValue("mantle.party.contact.ContactMech");
                contactMech.set("contactMechId", contactMechId);
                contactMech.set("contactMechTypeEnumId", "CmtEmailAddress");
                contactMech.set("infoString", legacyP.getEmail());
                contactMech.createOrUpdate();

                EntityValue partyContactMech = ec.getEntity().makeValue("mantle.party.contact.PartyContactMech");
                partyContactMech.set("partyId", legacyId);
                partyContactMech.set("contactMechId", contactMechId);
                partyContactMech.set("contactMechPurposeId", "EmailPrimary");
                partyContactMech.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)));
                partyContactMech.createOrUpdate();
            }

            // Bank Account
            if (!isEmpty(legacyP.getIban())) {
                String pmtMethodId = legacyId + "_BANK";
                EntityValue paymentMethod = ec.getEntity().makeValue("mantle.account.method.PaymentMethod");
                paymentMethod.set("paymentMethodId", pmtMethodId);
                paymentMethod.set("paymentMethodTypeEnumId", "PmtBankAccount");
                paymentMethod.set("ownerPartyId", legacyId);
                paymentMethod.createOrUpdate();

                EntityValue bankAccount = ec.getEntity().makeValue("mantle.account.method.BankAccount");
                bankAccount.set("paymentMethodId", pmtMethodId);
                bankAccount.set("bankName", legacyP.getBanca());
                bankAccount.set("accountNumber", legacyP.getIban());
                bankAccount.createOrUpdate();
            }

            // Address (ro.colibri.embeddable.Address)
            ro.colibri.embeddable.Address legacyAddr = legacyP.getAddress();
            if (legacyAddr != null) {
                String contactMechId = legacyId + "_ADDR";
                EntityValue contactMech = ec.getEntity().makeValue("mantle.party.contact.ContactMech");
                contactMech.set("contactMechId", contactMechId);
                contactMech.set("contactMechTypeEnumId", "CmtPostalAddress");
                contactMech.createOrUpdate();

                EntityValue postalAddress = ec.getEntity().makeValue("mantle.party.contact.PostalAddress");
                postalAddress.set("contactMechId", contactMechId);
                postalAddress.set("countryGeoId", mapCountry(legacyAddr.getCountry()));
                postalAddress.set("countyGeoId", mapCounty(legacyAddr.getJudet()));
                postalAddress.set("city", legacyAddr.getOras());
                postalAddress.set("address1", legacyAddr.getStrada());
                postalAddress.set("postalCode", legacyAddr.getNr());
                postalAddress.createOrUpdate();

                EntityValue partyContactMech = ec.getEntity().makeValue("mantle.party.contact.PartyContactMech");
                partyContactMech.set("partyId", legacyId);
                partyContactMech.set("contactMechId", contactMechId);
                partyContactMech.set("contactMechPurposeId", "PostalPrimary");
                partyContactMech.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)));
                partyContactMech.createOrUpdate();
            }

            // Delivery Address (String)
            if (!isEmpty(legacyP.getDeliveryAddress())) {
                String contactMechId = legacyId + "_DELIV_ADDR";
                EntityValue contactMech = ec.getEntity().makeValue("mantle.party.contact.ContactMech");
                contactMech.set("contactMechId", contactMechId);
                contactMech.set("contactMechTypeEnumId", "CmtPostalAddress");
                contactMech.createOrUpdate();

                EntityValue postalAddress = ec.getEntity().makeValue("mantle.party.contact.PostalAddress");
                postalAddress.set("contactMechId", contactMechId);
                postalAddress.set("address1", legacyP.getDeliveryAddress());
                postalAddress.set("directions", legacyP.getIndicatii());
                postalAddress.createOrUpdate();

                EntityValue partyContactMech = ec.getEntity().makeValue("mantle.party.contact.PartyContactMech");
                partyContactMech.set("partyId", legacyId);
                partyContactMech.set("contactMechId", contactMechId);
                partyContactMech.set("contactMechPurposeId", "PostalShippingDest");
                partyContactMech.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)));
                partyContactMech.createOrUpdate();
            }
        }

        ec.getLogger().info("[syncPartners] Done. Imported " + count + " partners.");
        return Map.of();
    }

    private static String mapCountry(String country) {
        if (isEmpty(country))
            return null;
        switch (country.toUpperCase()) {
            case "CZ":
                return "CZE";
            case "RO":
                return "ROU";
            case "HU":
                return "HUN";
            case "DE":
                return "DEU";
            case "BE":
                return "BEL";
            case "PL":
                return "POL";
            case "AU":
                return "AUS";
            case "IE":
                return "IRL";
            case "FR":
                return "FRA";
        }
        return country;
    }

    private static String mapCounty(String judet) {
        if (isEmpty(judet))
            return null;
        if (judet.trim().length() == 2)
            return "RO-" + judet.trim();
        if (judet.startsWith("RO-"))
            return judet.trim();
        switch (judet.toUpperCase()) {
            case "BIHOR":
                return "RO-BH";
            case "ILFOV":
                return "RO-IF";
            case "ARGES":
                return "RO-AG";
            case "BUCURESTI":
                return "RO-B";
            case "HUNEDOARA":
                return "RO-HD";
            case "IASI":
                return "RO-IS";
            case "SALAJ":
                return "RO-SJ";
            case "DOLJ":
                return "RO-DJ";
            case "TIMIS":
                return "RO-TM";
            case "SATU MARE":
                return "RO-SM";
            case "ARAD":
                return "RO-AR";
            case "BACAU":
                return "RO-BC";
            case "ALBA":
                return "RO-AB";
            case "NEAMT":
                return "RO-NT";
            case "CONSTANTA":
                return "RO-CT";
            case "CLUJ":
                return "RO-CJ";
            case "BRASOV":
                return "RO-BV";
        }
        return null;
    }

    public static Map<String, Object> importProductStatistics(ExecutionContext ec) {
        final DiskFileItem fileBytes = (DiskFileItem) ec.getContext().get("uploadedFile");
        final CSVFormat.Builder csvFormatBuilder = CSVFormat.Builder.create();
        final CSVFormat fmt = csvFormatBuilder.build();

        try (BufferedReader csvReader = new BufferedReader(new InputStreamReader(fileBytes.getInputStream()))) {
            for (final CSVRecord rec : fmt.parse(csvReader)) {
                final String barcode = rec.get(0);
                final String paretoCat = rec.get(4);

                EntityValue productIden = ec.getEntity().find("ProductIdentification")
                        .condition("productIdTypeEnumId", "PidtSku")
                        .condition("idValue", barcode)
                        .one();

                if (productIden == null) {
                    continue;
                }

                final String productId = (String) productIden.get("productId");

                final EntityValue categoryMember = ec.getEntity().makeValue("ProductCategoryMember");
                categoryMember.set("productId", productId);
                categoryMember.set("productCategoryId", "L2" + paretoCat);
                categoryMember.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));
                categoryMember.createOrUpdate();
            }
        } catch (final IOException e) {
            throw new ServiceException("Error at importProductStatistics", e);
        }

        try {
            fileBytes.delete();
        } catch (IOException e) {
            ec.getLogger().error(e.getMessage(), e);
        }
        return Map.of();
    }

    public static Map<String, Object> importProductSupplier(ExecutionContext ec) {
        final DiskFileItem fileBytes = (DiskFileItem) ec.getContext().get("uploadedFile");
        final CSVFormat.Builder csvFormatBuilder = CSVFormat.Builder.create();
        final CSVFormat fmt = csvFormatBuilder.build();

        try (BufferedReader csvReader = new BufferedReader(new InputStreamReader(fileBytes.getInputStream()))) {
            for (final CSVRecord rec : fmt.parse(csvReader)) {
                final String barcode = rec.get(0);
                final String supplierName = rec.get(5);

                if (isEmpty(supplierName))
                    continue;

                EntityValue productIden = ec.getEntity().find("ProductIdentification")
                        .condition("productIdTypeEnumId", "PidtSku")
                        .condition("idValue", barcode)
                        .useCache(true)
                        .one();

                if (productIden == null) {
                    continue;
                }

                String supplierId;

                if ("TRANSFER".equals(supplierName)) {
                    supplierId = "L1";
                } else {
                    EntityValue supplier = ec.getEntity().find("Organization")
                            .condition("organizationName", supplierName)
                            .useCache(true)
                            .one();

                    if (supplier == null) {
                        continue;
                        // throw new ServiceException("Organization not found: "+supplierName);
                        // create supplier on demand
                        // final EntityValue party = ec.getEntity().makeValue("Party");
                        // party.set("partyTypeEnumId", "PtyOrganization");
                        // party.create();
                        // supplier = ec.getEntity().makeValue("Organization");
                        // supplier.set("partyId", party.get("partyId"));
                        // supplier.set("organizationName", supplierName);
                        // supplier.create();
                    }

                    supplierId = (String) supplier.get("partyId");
                }

                final EntityValue supplierRole = ec.getEntity().makeValue("PartyRole");
                supplierRole.set("partyId", supplierId);
                supplierRole.set("roleTypeId", "Supplier");
                supplierRole.createOrUpdate();

                final String productId = (String) productIden.get("productId");

                String agreementId = MessageFormat.format("AgrSuppl_L2_{0}", supplierId);
                final EntityValue agreement = ec.getEntity().makeValue("Agreement");
                agreement.set("agreementId", agreementId);
                agreement.set("agreementTypeEnumId", "AgrProduct");
                agreement.set("organizationPartyId", "L2");
                agreement.set("otherRoleTypeId", "Supplier");
                agreement.set("otherPartyId", supplierId);
                agreement.createOrUpdate();

                final EntityValue agreementItem = ec.getEntity().makeValue("AgreementItem");
                agreementItem.set("agreementId", agreementId);
                agreementItem.set("agreementItemTypeEnumId", "AitPurchase");
                agreementItem.set("productId", productId);
                agreementItem.set("agreementItemSeqId", productId);
                agreementItem.createOrUpdate();
            }
        } catch (final IOException e) {
            throw new ServiceException("Error at importProductSupplier", e);
        }

        try {
            fileBytes.delete();
        } catch (IOException e) {
            ec.getLogger().error(e.getMessage(), e);
        }
        return Map.of();
    }

    public static Product productById(final Integer id) {
        final VanzariBeanRemote commercialBean = ServiceLocator.getBusinessService(VanzariBean.class,
                VanzariBeanRemote.class);
        return commercialBean.productById(id);
    }

    public static ImmutableList<Product> convertToProducts(final ImmutableList<Operatiune> ops) {
        final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
        return bean.convertToProducts(ops);
    }

    public static InvocationResult addOperationToUnpersistedDoc(final Document.TipDoc tipDoc, final String doc,
            final String nrDoc, final LocalDate dataDoc, final String nrRec,
            final LocalDate dataRec, final Long partnerId, final boolean rpz, final Operatiune newOp,
            final Integer otherTransferGestId) {
        final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
        return bean.addOperationToUnpersistedDoc(tipDoc, doc, nrDoc, dataDoc, nrRec, dataRec, partnerId, rpz, newOp,
                otherTransferGestId);
    }

    public static InvocationResult addOperationToDoc(final long docId, final Operatiune op,
            final Integer otherTransferGestId) {
        final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
        return bean.addOperationToDoc(docId, op, otherTransferGestId);
    }

    public static ImmutableList<Gestiune> allGestiuni() {
        final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
        return bean.allGestiuni().stream()
                .sorted(Comparator.comparing(Gestiune::getImportName))
                .collect(toImmutableList());
    }

    public static long autoNumber(final Document.TipDoc tipDoc, final String doc, final Integer gestiuneId) {
        final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
        return bean.autoNumber(tipDoc, doc, gestiuneId);
    }

    public static ImmutableList<Document> filteredDocuments(final Integer gestiuneId,
            final Long partnerId, final Document.TipDoc tipDoc, final LocalDate from, final LocalDate to,
            final AccountingDocument.RPZLoad rpzLoad, final AccountingDocument.CasaLoad casaLoad,
            final AccountingDocument.BancaLoad bancaLoad, final Integer contBancarId,
            final AccountingDocument.DocumentTypesLoad documentTypes,
            final AccountingDocument.CoveredDocsLoad coveredLoad,
            final Boolean shouldTransport, final Integer userId, final AccountingDocument.ContaLoad contaLoad,
            final LocalDate transportFrom,
            final LocalDate transportTo) {
        final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
        final ImmutableList<Document> docs = bean.filteredDocuments(gestiuneId, partnerId, tipDoc, from, to, rpzLoad,
                casaLoad, bancaLoad, contBancarId, documentTypes, coveredLoad, shouldTransport, userId, contaLoad,
                transportFrom, transportTo);
        return docs;
    }

    public static InvocationResult regBanca(final Integer gestiuneId, final Integer contBancarId, final LocalDate from,
            final LocalDate to) {
        final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
        return bean.regBanca(gestiuneId, contBancarId, from, to);
    }
}
