package ro.colibri.legacy.service;

import org.moqui.context.ExecutionContext;
import org.moqui.entity.EntityValue;
import ro.colibri.beans.VanzariBean;
import ro.colibri.beans.VanzariBeanRemote;
import ro.colibri.entities.comercial.Product;

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
}
