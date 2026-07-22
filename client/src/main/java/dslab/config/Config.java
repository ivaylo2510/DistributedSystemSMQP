package dslab.config;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Reads the configuration from a {@code .properties} file.
 * <p>
 * DO NOT CHANGE ANY EXISTING METHODS OF THIS CLASS.
 * <p>
 * YOU ARE ALLOWED TO ADD ADDITIONAL METHODS.
 */
public final class Config {

    private final ResourceBundle bundle;
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * Creates an instance of {@link Config} which reads configuration data from a  {@code .properties} file with given name found in classpath resources.
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
     * Checks if an key is present in the configuration.
     * First in the properties map, then in the bundle.
     *
     * @param key the key to check
     * @return true if the key is present, false otherwise
     */
    public boolean containsKey(String key) {
        return properties.containsKey(key) || bundle.containsKey(key);
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
}
