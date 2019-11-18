package com.sequoiacm.client.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;

/**
 * Use for sets node configurations.
 */
public class ScmConfigProperties {
    enum TargetType {
        ALL,
        SERVICE,
        INSTANCE;
    }

    private TargetType targetType;
    private List<String> targets = new ArrayList<String>();;
    private Map<String, String> updateProps = new HashMap<String, String>();
    private List<String> deleteProps = new ArrayList<String>();
    private boolean acceptUnknownProps = false;

    private ScmConfigProperties() {
    }

    /**
     * Returns a builder for create ScmConfigProperties instance.
     *
     * @return builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the targets of type.
     *
     * @return targets type.
     */
    public String getTargetType() {
        return targetType.toString();
    }

    /**
     * Gets the targets.
     *
     * @return list of targets.
     */
    public List<String> getTargets() {
        return targets;
    }

    /**
     * Gets the properties for update.
     *
     * @return properties.
     */
    public Map<String, String> getUpdateProps() {
        return updateProps;
    }

    /**
     * Is accept unrecognized properties.
     *
     * @return true or false.
     */
    public boolean isAcceptUnknownProps() {
        return acceptUnknownProps;
    }

    /**
     * Gets the properties for delete.
     *
     * @return key list.
     */
    public List<String> getDeleteProps() {
        return deleteProps;
    }

    @Override
    public String toString() {
        return "ScmPropConfig [targetType=" + targetType + ", targets=" + targets + ", props="
                + updateProps + "]";
    }

    /**
     * Builder for create ScmConfigProperties instance.
     */
    public static class Builder {

        private ScmConfigProperties config = new ScmConfigProperties();

        private Builder() {

        }

        /**
         * Specifies the list of services name.
         *
         * @param services
         *            List of service name.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder services(List<String> services) throws ScmException {
            if (config.targetType != null && config.targetType != TargetType.SERVICE) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + config.targetType + " targets");
            }
            if (null == services || services.isEmpty()) {
                throw new ScmInvalidArgumentException("services is null or empty");
            }

            config.targets.addAll(services);
            config.targetType = TargetType.SERVICE;
            return this;
        }

        /**
         * Specifies same services name.
         *
         * @param services
         *            Service name.
         * @return This.
         * @throws ScmException
         *             If error happens;
         */
        public Builder service(String... services) throws ScmException {
            if (config.targetType != null && config.targetType != TargetType.SERVICE) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + config.targetType + " targets");
            }

            if (services == null || services.length <= 0) {
                throw new ScmInvalidArgumentException(
                        "array of service is null or empty:" + Arrays.toString(services));
            }

            for (String service : services) {
                if (service == null || service.trim().isEmpty()) {
                    throw new ScmException(ScmError.INVALID_ARGUMENT,
                            "service can not be null or empty");
                }
                config.targets.add(service);
            }

            config.targetType = TargetType.SERVICE;
            return this;
        }

        /**
         * Specifies the list of instance url.
         *
         * @param instances
         *            List of instance url.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder instances(List<String> instances) throws ScmException {
            if (config.targetType != null && config.targetType != TargetType.INSTANCE) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + config.targetType + " targets");
            }

            if (null == instances || instances.isEmpty()) {
                throw new ScmInvalidArgumentException("instances is null or empty");
            }

            config.targets.addAll(instances);
            config.targetType = TargetType.INSTANCE;
            return this;
        }

        /**
         * Specifies some instance url.
         *
         * @param instances
         *            Instance url.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder instance(String... instances) throws ScmException {
            if (config.targetType != null && config.targetType != TargetType.INSTANCE) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + config.targetType + " targets");
            }

            if (instances == null || instances.length <= 0) {
                throw new ScmInvalidArgumentException(
                        "array of instance is null or empty:" + Arrays.toString(instances));
            }

            for (String instance : instances) {
                if (instance == null || instance.isEmpty()) {
                    throw new ScmException(ScmError.INVALID_ARGUMENT,
                            "instance url can not be null or empty");
                }
                config.targets.add(instance);
            }

            config.targetType = TargetType.INSTANCE;
            return this;
        }

        /**
         * Specifies all instances.
         *
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder allInstance() throws ScmException {
            if (config.targetType != null && config.targetType != TargetType.ALL) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + config.targetType + " targets");
            }
            config.targetType = TargetType.ALL;
            return this;
        }

        /**
         * Specifies a new property for the targets.
         *
         * @param key
         *            Property key.
         * @param value
         *            property value.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder updateProperty(String key, String value) throws ScmException {
            if (key == null || value == null) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "key or value can not be null");
            }
            config.updateProps.put(key, value);
            return this;
        }

        /**
         * Specifies the new properties for the targets.
         *
         * @param properties
         *            A map of properties.
         * @return This.
         * @throws ScmInvalidArgumentException
         *             If error happens.
         */
        public Builder updateProperties(Map<String, String> properties)
                throws ScmInvalidArgumentException {
            if (properties == null) {
                throw new ScmInvalidArgumentException("properties can not be null");
            }
            config.updateProps.putAll(properties);
            return this;
        }

        /**
         * Unset the Specifies property key list for the targets
         *
         * @param keyList
         *            key name list.
         * @return This.
         * @throws ScmInvalidArgumentException
         *             If error happen.
         */
        public Builder deleteProperties(List<String> keyList) throws ScmInvalidArgumentException {
            if (keyList == null) {
                throw new ScmInvalidArgumentException("key list can not be null");
            }
            config.deleteProps.addAll(keyList);
            return this;
        }

        /**
         * Unset the Specifies property key name for the targets
         *
         * @param keyName
         *            Key name.
         * @return This.
         * @throws ScmInvalidArgumentException
         *             If error happens.
         */
        public Builder deleteProperty(String keyName) throws ScmInvalidArgumentException {
            if (keyName == null) {
                throw new ScmInvalidArgumentException("key can not be null");
            }
            config.deleteProps.add(keyName);
            return this;
        }

        /**
         * Whether the targets should accept unrecognized properties.
         *
         * @param isAcceptUnknownProps
         *            true or false.
         * @return this.
         */
        public Builder acceptUnknownProperties(boolean isAcceptUnknownProps) {
            config.acceptUnknownProps = isAcceptUnknownProps;
            return this;
        }

        /**
         * Creates the instance of ScmConfigProperties.
         *
         * @return Instance of ScmConfigProperties.
         * @throws ScmException
         *             If error happens.
         */
        public ScmConfigProperties build() throws ScmException {
            if (config.targetType == null) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "targets is empty");
            }

            if (config.updateProps.size() == 0 && config.deleteProps.size() == 0) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "properties for modify is empty");
            }

            return config;
        }
    }
}
