package com.sequoiacm.client.element.privilege;

/**
 * SCM resource
 */
public interface ScmResource {
    /**
     * Get the type of the resource
     *
     * @return type string.
     */
    String getType();

    /**
     * Format the resource to a string.
     *
     * @return string.
     */
    String toStringFormat();
}
