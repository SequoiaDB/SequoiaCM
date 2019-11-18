package com.sequoiacm.client.element;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class of ScmProerties.
 * @since 2.1
 */
@Deprecated
public class ScmProperties {
    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Add properties.
     * @param map Properties.
     * @since 2.1
     */
    public void addProperty(Map<String, Object> map) {
        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            properties.put(key, map.get(key));
        }
    }

    /**
     * Add property.
     * @param name Property key.
     * @param value Property value.
     * @since 2.1
     */
    public void addProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Get property.
     * @param name Property key.
     * @return value.
     * @since 2.1
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Delete property.
     * @param name Property key.
     * @since 2.1
     */
    public void deleteProperty(String name) {
        properties.remove(name);
    }

    /**
     * Get all keys.
     * @return Keys set.
     * @since 2.1
     */
    public Set<String> keySet() {
        return properties.keySet();
    }

    @Override
    public String toString() {
        if (!properties.isEmpty()) {
            StringBuffer strBuff = new StringBuffer("{");
            Set<String> propertiesNameSet = properties.keySet();
            for (String name : propertiesNameSet) {
                strBuff.append("\"").append(name).append("\":\"").append(properties.get(name)).append("\", ");
            }
            String str = strBuff.substring(0, strBuff.length() - 2);
            return str + "}";
        }
        else {
            return "{}";
        }
    }
}
