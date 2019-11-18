package com.sequoiacm.client.element;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class of ScmProerties.
 *
 * @since 2.1
 */
public class ScmClassProperties {
    private String classId;

    /**
     * Create scm class propertis with specified class id.
     *
     * @param classId
     *            class id.
     */
    public ScmClassProperties(String classId) {
        this.classId = classId;
    }

    private Map<String, Object> classProperties = new HashMap<String, Object>();

    /**
     * Add classProperties.
     *
     * @param properties
     *            classProperties.
     * @since 2.1
     */
    public void addProperties(Map<String, Object> properties) {
        if (null != properties) {
            Set<String> keySet = properties.keySet();
            for (String key : keySet) {
                classProperties.put(key, properties.get(key));
            }
        }
    }

    /**
     * Add property.
     *
     * @param name
     *            Property key.
     * @param value
     *            Property value.
     * @since 2.1
     */
    public void addProperty(String name, Object value) {
        classProperties.put(name, value);
    }

    /**
     * Get property.
     *
     * @param name
     *            Property key.
     * @return value.
     * @since 2.1
     */
    public Object getProperty(String name) {
        return classProperties.get(name);
    }

    /**
     * Delete property.
     *
     * @param name
     *            Property key.
     * @since 2.1
     */
    public void deleteProperty(String name) {
        classProperties.remove(name);
    }

    public boolean contains(String name) {
        return classProperties.containsKey(name);
    }

    /**
     * Get all keys.
     *
     * @return Keys set.
     * @since 2.1
     */
    public Set<String> keySet() {
        return classProperties.keySet();
    }

    /**
     * Convert the current properties value to Map
     *
     * @return Map
     */
    public Map<String, Object> toMap() {
        return new HashMap<String, Object>(classProperties);
    }

    @Override
    public String toString() {
        StringBuffer strBuff = new StringBuffer("{");
        Set<String> propertiesNameSet = classProperties.keySet();
        for (String name : propertiesNameSet) {
            strBuff.append(name + ":" + classProperties.get(name) + ", ");
        }
        String str = strBuff.substring(0, strBuff.length() - 2);
        return str + "}";
    }

    /**
     * Gets the ClassId
     *
     * @return classId
     */
    public String getClassId() {
        return classId;
    }

    /***
     * Sets the ClassID
     *
     * @param classId
     *            set classId
     */
    public void setClassId(String classId) {
        this.classId = classId;
    }
}
