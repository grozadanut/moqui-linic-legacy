package ro.colibri.util;

import com.google.common.collect.Lists;
import org.apache.commons.collections.ListUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    /**
     * Adds the target data values to this holder, matching by <code>primaryKey</code>.
     * If GenericValues already exist with the <code>primaryKey</code>, they will be updated, if not, new GenericValues will be added.
     * Only the keys that are found in <code>targetToHolderKey</code> will be taken from <code>data</code>,
     * if the map is empty or null takes all keys from target and maps them to the same name in source.
     *
     * @param targetData target data to add to this holder
     * @param targetPrimaryKey key by which it will match the target data
     * @param sourcePrimaryKey key by which it will match the source data
     * @param targetToHolderKey maps the key in the target data with this key in the holder data
     */
    public static void addOrUpdate(List<Map> data, List<Map> targetData, String targetPrimaryKey, String sourcePrimaryKey, Map<String, String> targetToHolderKey) {
        final Map<Object, List<Map>> dataById = data.stream()
                .collect(Collectors.toMap(gv -> gv.get(sourcePrimaryKey), Lists::newArrayList, ListUtils::union));

        for (final Map target : targetData) {
            final Object targetId = target.get(targetPrimaryKey);
            if (dataById.containsKey(targetId)) {
                final List<Map> source = dataById.get(targetId);
                source.forEach(s -> update(s, target, targetToHolderKey));
            }
            else
                data.add(clone(target, targetToHolderKey));
        }
    }
    /**
     * Only the keys that are found in <code>targetToSourceKey</code> will be updated,
     * if the map is empty or null updates all keys.
     *
     * @param targetToSourceKey maps the key in the target data with this key in the source data
     * @return this
     */
    private static void update(Map source, final Map target, final Map<String, String> targetToSourceKey) {
        if (targetToSourceKey == null || targetToSourceKey.isEmpty())
            source.putAll(target);
        else
            source.putAll(targetToSourceKey.entrySet().stream().collect(toMapOfNullables(Map.Entry::getValue, e -> target.get(e.getKey()))));
    }

    /**
     * Only the keys that are found in <code>targetToCloneKey</code> will be cloned,
     * if the map is empty or null clones all keys.
     *
     * @param targetToCloneKey maps the key in the target data with this key in the cloned data
     */
    private static Map clone(Map value, final Map<String, String> targetToCloneKey) {
        if (targetToCloneKey == null || targetToCloneKey.isEmpty()) {
            Map clone = new HashMap();
            clone.putAll(value);
            return clone;
        } else {
            Map clone = new HashMap();
            clone.putAll(targetToCloneKey.entrySet().stream().collect(toMapOfNullables(Map.Entry::getValue, e -> value.get(e.getKey()))));
            return clone;
        }
    }

    public static void addOrUpdate(List<Map> data, final List<Map> targetData, final String primaryKey) {
        addOrUpdate(data, targetData, primaryKey, primaryKey, null);
    }

    public static void addOrUpdate(List<Map> data, final List<Map> targetData, final String targetPrimaryKey, final String sourcePrimaryKey) {
        addOrUpdate(data, targetData, targetPrimaryKey, sourcePrimaryKey, null);
    }

    /**
     * This version:
     *
     * <ul>
     * <li>Allows null keys</li>
     * <li>Allows null values</li>
     * <li>Detects duplicate keys (even if they are null) and throws
     * IllegalStateException as in the original JDK implementation</li>
     * <li>Detects duplicate keys also when the key already mapped to the null
     * value. In other words, separates a mapping with null-value from
     * no-mapping</li>
     * </ul>
     *
     * @param <T>
     * @param <K>
     * @param <U>
     * @param keyMapper
     * @param valueMapper
     * @return
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toMapOfNullables(Function<? super T, ? extends K> keyMapper,
                                                                        Function<? super T, ? extends U> valueMapper) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            final Map<K, U> map = new HashMap<>();
            list.forEach(item -> {
                final K key = keyMapper.apply(item);
                final U value = valueMapper.apply(item);
                if (map.containsKey(key)) {
                    throw new IllegalStateException(String
                            .format("Duplicate key %s (attempted merging values %s and %s)", key, map.get(key), value));
                }
                map.put(key, value);
            });
            return map;
        });
    }

    public static String extractTileName(String productName) {
        if (productName == null)
            return null;
        Pattern pattern = Pattern.compile("^\\S+\\s+([^0-9]+)");
        Matcher matcher = pattern.matcher(productName);
        if (matcher.find())
            return matcher.group(1).trim();
        return productName.trim();
    }
}
