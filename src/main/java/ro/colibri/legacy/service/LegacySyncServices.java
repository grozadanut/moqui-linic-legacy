package ro.colibri.legacy.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.moqui.context.ExecutionContext;
import org.moqui.entity.EntityValue;
import org.moqui.service.ServiceException;
import ro.colibri.beans.VanzariBean;
import ro.colibri.beans.VanzariBeanRemote;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.Product;
import ro.colibri.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Map;

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
            prod.set("productTypeEnumId", "PtAsset");
            prod.set("assetTypeEnumId", "AstTpInventory");
            prod.set("assetClassEnumId", "AsClsInventoryFin");

            final EntityValue sku = ec.getEntity().makeValue("ProductIdentification");
            sku.set("productIdTypeEnumId", "PidtSku");
            sku.set("productId", legacyId);
            sku.set("idValue", legacyProd.getBarcode());

            prod.createOrUpdate();
            sku.createOrUpdate();
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
        final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);

        for (final Partner legacyP : bean.allPartners()) {
            boolean isPerson = StringUtils.isEmpty(legacyP.getCodFiscal());

            final EntityValue party = ec.getEntity().makeValue("Party");
            final String legacyId = legacyP.getId() + "";
            party.set("partyId", legacyId);
            party.set("partyTypeEnumId", isPerson ? "PtyPerson" : "PtyOrganization");
            party.createOrUpdate();

            if (isPerson) {
                String[] nameTokens = legacyP.getName().split(" ");
                String lastName = nameTokens.length > 0 ? nameTokens[0] : ""; // nume de familie
                String firstName = ""; // prenume
                for (int i = 1; i < nameTokens.length; i++) {
                    firstName += nameTokens[i] + " ";
                }

                final EntityValue pg = ec.getEntity().makeValue("Person");
                pg.set("partyId", legacyId);
                pg.set("firstName", firstName);
                pg.set("lastName", lastName);
                pg.createOrUpdate();
            } else {
                final EntityValue pg = ec.getEntity().makeValue("Organization");
                pg.set("partyId", legacyId);
                pg.set("organizationName", legacyP.getName());
                pg.createOrUpdate();
            }
        }

        return Map.of();
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
                categoryMember.set("fromDate", Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0)));
                categoryMember.createOrUpdate();
            }
        } catch (final IOException e) {
            throw new ServiceException("Error at importProductStatistics", e);
        }

        fileBytes.delete();
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

                if (StringUtils.isEmpty(supplierName))
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
//                        throw new ServiceException("Organization not found: "+supplierName);
                        // create supplier on demand
//                    final EntityValue party = ec.getEntity().makeValue("Party");
//                    party.set("partyTypeEnumId", "PtyOrganization");
//                    party.create();
//                    supplier = ec.getEntity().makeValue("Organization");
//                    supplier.set("partyId", party.get("partyId"));
//                    supplier.set("organizationName", supplierName);
//                    supplier.create();
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

        fileBytes.delete();
        return Map.of();
    }
}
