package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Use for sets check connect target instance.
 */
public class ScmCheckConnTarget {

    private boolean isAll;

    private List<String> instances = new ArrayList<String>();

    private List<String> services = new ArrayList<String>();

    private ScmCheckConnTarget() {

    }

    /**
     * Returns a builder for create ScmCheckConnTarget instance.
     *
     * @return builder.
     */
    public static Builder builder() {
        return new ScmCheckConnTarget.Builder();
    }

    /**
     * Return the target instances.
     *
     * @return instances.
     * @since 3.2.2
     */
    public List<String> getInstances() {
        return instances;
    }

    /**
     * Return the target services.
     *
     * @return services.
     * @since 3.2.2
     */
    public List<String> getServices() {
        return services;
    }

    /**
     * Return the ScmCheckConnTarget  instance is all or not
     *
     * @return return true if is all.
     * @since 3.2.2
     */
    public boolean isAll() {
        return isAll;
    }

    public static class Builder {

        private ScmCheckConnTarget target = new ScmCheckConnTarget();

        private Builder() {

        }

        /**
         * Specifies the list of service.
         *
         * @param services
         *            List of service.
         * @return This.
         * @since 3.2.2
         */
        public Builder service(String... services) throws ScmException {
            if (services == null || services.length <= 0) {
                throw new ScmInvalidArgumentException(
                        "array of service is null or empty:" + Arrays.toString(services));
            }
            target.services.addAll(Arrays.asList(services));
            return this;
        }

        /**
         * Specifies the list of instance.
         *
         * @param instances
         *            List of instance.
         * @return This.
         * @since 3.2.2
         */
        public Builder instance(String... instances) throws ScmException {

            if (instances == null || instances.length <= 0) {
                throw new ScmInvalidArgumentException(
                        "array of instance is null or empty:" + Arrays.toString(instances));
            }
            target.instances.addAll(Arrays.asList(instances));
            return this;
        }

        /**
         * Whether specifies all instances.
         *
         * @return This.
         * @since 3.2.2
         */
        public Builder allInstance() throws ScmException {
            target.isAll = true;
            return this;
        }

        /**
         * Creates the instance of ScmCheckConnTarget.
         *
         * @return Instance of ScmCheckConnTarget.
         * @since 3.2.2
         */
        public ScmCheckConnTarget build() throws ScmException {
            if (!target.isAll && target.instances == null && target.services == null) {
                throw new ScmException(ScmError.INVALID_ARGUMENT, "targets is empty");
            }
            return target;
        }
    }
}
