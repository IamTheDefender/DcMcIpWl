package xyz.iamthedefender.dragonmc.yaml;

import java.util.*;

/**
 * Represents a section (node) in a YAML configuration.
 * Supports nested sections, strings, numbers, booleans, and lists.
 */
public class ConfigurationSection {

    private final Map<String, Object> data = new LinkedHashMap<>();
    private final String path;

    public ConfigurationSection(String path) {
        this.path = path;
    }

    public ConfigurationSection() {
        this("");
    }

    // ─────────────────────────────── SET ────────────────────────────────── //

    public void set(String key, Object value) {
        String[] parts = key.split("\\.", 2);
        if (parts.length == 2) {
            ConfigurationSection child = getOrCreateSection(parts[0]);
            child.set(parts[1], value);
        } else {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }
    }

    private ConfigurationSection getOrCreateSection(String key) {
        Object existing = data.get(key);
        if (existing instanceof ConfigurationSection) {
            return (ConfigurationSection) existing;
        }
        String fullPath = path.isEmpty() ? key : path + "." + key;
        ConfigurationSection section = new ConfigurationSection(fullPath);
        data.put(key, section);
        return section;
    }

    // ─────────────────────────────── GET ────────────────────────────────── //

    public Object get(String key) {
        return get(key, null);
    }

    public Object get(String key, Object def) {
        String[] parts = key.split("\\.", 2);
        if (parts.length == 2) {
            Object child = data.get(parts[0]);
            if (child instanceof ConfigurationSection) {
                return ((ConfigurationSection) child).get(parts[1], def);
            }
            return def;
        }
        return data.getOrDefault(key, def);
    }

    // ─── Typed getters ───────────────────────────────────────────────────── //

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String def) {
        Object val = get(key);
        return val != null ? String.valueOf(val) : def;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int def) {
        Object val = get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long def) {
        Object val = get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try {
                return Long.parseLong(val.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double def) {
        Object val = get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean def) {
        Object val = get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return def;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String key) {
        Object val = get(key);
        if (val instanceof List) return (List<Object>) val;
        return new ArrayList<>();
    }

    public List<String> getStringList(String key) {
        List<String> result = new ArrayList<>();
        for (Object o : getList(key)) result.add(String.valueOf(o));
        return result;
    }

    public ConfigurationSection getSection(String key) {
        Object val = get(key);
        if (val instanceof ConfigurationSection) return (ConfigurationSection) val;
        return null;
    }

    public ConfigurationSection getOrCreateSection(String key, boolean create) {
        Object val = get(key);
        if (val instanceof ConfigurationSection) return (ConfigurationSection) val;
        if (!create) return null;
        return getOrCreateSection(key);
    }

    // ─────────────────────────────── QUERY ──────────────────────────────── //

    public boolean contains(String key) {
        return get(key) != null;
    }

    public boolean isSection(String key) {
        return get(key) instanceof ConfigurationSection;
    }

    public Set<String> getKeys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /**
     * Returns ALL dot-notated keys recursively.
     */
    public Set<String> getDeepKeys() {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys("", keys);
        return keys;
    }

    private void collectKeys(String prefix, Set<String> result) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String full = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            result.add(full);
            if (entry.getValue() instanceof ConfigurationSection) {
                ((ConfigurationSection) entry.getValue()).collectKeys(full, result);
            }
        }
    }

    public String getPath() {
        return path;
    }

    /**
     * Raw access to the underlying map (used by writer).
     */
    Map<String, Object> getRawMap() {
        return data;
    }

    @Override
    public String toString() {
        return "ConfigurationSection{path='" + path + "', keys=" + data.keySet() + "}";
    }
}