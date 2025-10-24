package ro.colibri.util;

public class Utils {
    public static String UNECERec20ToUom(final String uomCode) {
        switch (uomCode) {
            case "OTH_ea":
            case "H87":
            case "XPP":
                return "BUC";
            default:
                return uomCode;
        }
    }
}
