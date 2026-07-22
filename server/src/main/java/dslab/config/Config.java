package dslab.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Reads the configuration from a {@code .properties} file.
 */
public final class Config {

    private final ResourceBundle bundle;
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * Creates an instance of Config which reads configuration data form {@code .properties} file with given name found
     * in classpath.
     *
     * @param name the name of the .properties file
     */
    public Config(String name) {
        if (name.endsWith(".properties")) {
            this.bundle = ResourceBundle.getBundle(name.substring(0, name.length() - 11));
        } else {
            this.bundle = ResourceBundle.getBundle(name);
        }
    }

    /**
     * Returns the value as String for the given key.
     *
     * @param key the property's key
     * @return String value of the property
     * @see ResourceBundle#getString(String)
     */
    public String getString(String key) {
        if (!properties.containsKey(key)) {
            properties.put(key, bundle.getString(key));
        }

        return (String) properties.get(key);
    }

    public String[] getStringArr(String key) {
        if (!properties.containsKey(key)) {
            String[] val = bundle.getString(key).split(",");
            properties.put(key, val);
        }

        return (String[]) properties.get(key);
    }

    public int[] getIntArr(String key) {
        if (!properties.containsKey(key)) {
            String[] stringVal = bundle.getString(key).split(",");
            int[] intVal = Arrays.stream(stringVal).mapToInt(Integer::parseInt).toArray();
            properties.put(key, intVal);
        }

        return (int[]) properties.get(key);
    }

    /**
     * Returns the value as {@code int} for the given key.
     *
     * @param key the property's key
     * @return int value of the property
     * @throws NumberFormatException if the String cannot be parsed to an Integer
     */
    public int getInt(String key) {
        if (!properties.containsKey(key)) {
            int val = Integer.parseInt(bundle.getString(key));
            properties.put(key, val);
        }

        return (int) properties.get(key);
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key) || bundle.containsKey(key);
    }

    /**
     * Sets the value for the given key.
     *
     * @param key the property's key
     * @param value the value of the property
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Returns all keys of this configuration.
     *
     * @return the keys
     */
    public Set<String> listKeys() {
        Set<String> keys = bundle.keySet();
        keys.addAll(properties.keySet());
        return keys;
    }
}
