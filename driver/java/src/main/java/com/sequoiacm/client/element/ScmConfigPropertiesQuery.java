package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScmConfigPropertiesQuery {

    enum TargetType {
        ALL,
        SERVICE,
        INSTANCE;
    }

    private TargetType targetType;
    private List<String> targets = new ArrayList<String>();;
    private List<String> props = new ArrayList<String>();

    private ScmConfigPropertiesQuery() {
    }

    /**
     * Returns a builder for create ScmConfigPropertiesQuery instance.
     *
     * @return builder.
     */
    public static Builder builder() {
        return new ScmConfigPropertiesQuery.Builder();
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
     * Gets the properties for query.
     *
     * @return key list.
     */
    public List<String> getProps() {
        return props;
    }

    @Override
    public String toString() {
        return "ScmPropConfig [targetType=" + targetType + ", targets=" + targets + ", props="
                + props + "]";
    }

    /**
     * Builder for create ScmConfigPropertiesQuery instance.
     */
    public static class Builder {

        private ScmConfigPropertiesQuery query = new ScmConfigPropertiesQuery();

        private Builder() {

        }

        /**
         * Specifies the list of services name. The supported service name can be
         * queried by ScmSystem.ServiceCenter#getServiceList(ScmSession).
         *
         * @param services
         *            List of service name.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder services(List<String> services) throws ScmException {
            if (query.targetType != null
                    && query.targetType != ScmConfigPropertiesQuery.TargetType.SERVICE) {
                throw new ScmInvalidArgumentException(
                        "already set " + query.targetType + " targets");
            }
            if (null == services || services.isEmpty()) {
                throw new ScmInvalidArgumentException("services is null or empty");
            }
            for (String service : services) {
                if (null == service || service.isEmpty()) {
                    throw new ScmInvalidArgumentException(
                            "serviceName is null or empty: services=" + services);
                }
                query.targets.add(service);
            }

            query.targetType = TargetType.SERVICE;
            return this;
        }

        /**
         * Specifies same services name. The supported service names can be queried by
         * ScmSystem.ServiceCenter#getServiceList(ScmSession).
         *
         * @param services
         *            Service name.
         * @return This.
         * @throws ScmException
         *             If error happens;
         */
        public Builder service(String... services) throws ScmException {
            if (services == null || services.length == 0) {
                throw new ScmInvalidArgumentException("services is null or empty");
            }
            return this.services(Arrays.asList(services));
        }

        /**
         * Specifies the list of instance url(the url format is server1:8080). The
         * supported instance can be queried by
         * ScmSystem.ServiceCenter#getServiceInstanceList(ScmSession, String)
         *
         * @param instances
         *            List of instance url.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder instances(List<String> instances) throws ScmException {
            if (query.targetType != null && query.targetType != TargetType.INSTANCE) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + query.targetType + " targets");
            }

            if (null == instances || instances.isEmpty()) {
                throw new ScmInvalidArgumentException("instances is null or empty");
            }
            for (String instance : instances) {
                if (null == instance || instance.isEmpty()) {
                    throw new ScmInvalidArgumentException(
                            "instance is null or empty: instances=" + instances);
                }
                query.targets.add(instance);
            }

            query.targetType = TargetType.INSTANCE;
            return this;
        }

        /**
         * Specifies some instance url(the url format is server1:8080). The supported instance can
         * be queried by ScmSystem.ServiceCenter#getServiceInstanceList(ScmSession,
         * String)
         *
         * @param instances
         *            Instance url.
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder instance(String... instances) throws ScmException {
            if (instances == null || instances.length == 0) {
                throw new ScmInvalidArgumentException("instances is null or empty");
            }
            return this.instances(Arrays.asList(instances));
        }

        /**
         * Specifies all instances.
         *
         * @return This.
         * @throws ScmException
         *             If error happens.
         */
        public Builder allInstance() throws ScmException {
            if (query.targetType != null && query.targetType != TargetType.ALL) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "already set " + query.targetType + " targets");
            }
            query.targetType = TargetType.ALL;
            return this;
        }

        /**
         * Add list of property key name that need to be queried.
         *
         * @param keyList
         *            key name list.
         * @return This.
         * @throws ScmInvalidArgumentException
         *             If error happen.
         */
        public Builder addProperties(List<String> keyList) throws ScmInvalidArgumentException {
            if (keyList == null) {
                throw new ScmInvalidArgumentException("key list can not be null");
            }
            query.props.addAll(keyList);
            return this;
        }

        /**
         * Add property key name that need to be queried.
         *
         * @param keyName
         *            Key name.
         * @return This.
         * @throws ScmInvalidArgumentException
         *             If error happens.
         */
        public Builder addProperty(String keyName) throws ScmInvalidArgumentException {
            if (keyName == null || keyName.isEmpty()) {
                throw new ScmInvalidArgumentException("key can not be empty");
            }
            query.props.add(keyName);
            return this;
        }

        /**
         * Creates the instance of ScmConfigPropertiesQuery.
         *
         * @return Instance of ScmConfigPropertiesQuery.
         * @throws ScmException
         *             If error happens.
         */
        public ScmConfigPropertiesQuery build() throws ScmException {
            if (query.targetType == null) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "targets is empty");
            }

            if (query.props.size() == 0) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "properties is empty");
            }

            return query;
        }
    }
}
