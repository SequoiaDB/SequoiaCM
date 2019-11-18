package com.sequoiacm.client.element.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;

/**
 * Scm attribute config.
 */
public class ScmAttributeConf {

    private String name;
    private String displayName = "";
    private String description = "";
    private AttributeType type;
    private ScmAttrRule checkRule;
    private boolean required;

    /**
     * Gets the config name.
     *
     * @return config name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the config name.
     *
     * @param name
     *            config name.
     * @return the current instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmAttributeConf setName(String name) throws ScmException {
        checkArgNotNull(FieldName.Attribute.FIELD_NAME, name);
        this.name = name;
        return this;
    }

    /**
     * Gets the display name.
     *
     * @return display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     *
     * @param displayName
     *            display name.
     * @return the current instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmAttributeConf setDisplayName(String displayName) throws ScmException {
        checkArgNotNull(FieldName.Attribute.FIELD_DISPLAY_NAME, displayName);
        this.displayName = displayName;
        return this;
    }

    /**
     * Gets the description.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description
     *            description.
     * @return the current instance.
     * @throws ScmException
     *             ie error happens.
     */
    public ScmAttributeConf setDescription(String description) throws ScmException {
        checkArgNotNull(FieldName.Attribute.FIELD_DESCRIPTION, description);
        this.description = description;
        return this;
    }

    /**
     * Gets the attribute type.
     *
     * @return attribute type.
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Sets the attribute type.
     *
     * @param type
     *            attribute type.
     * @return the current instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmAttributeConf setType(AttributeType type) throws ScmException {
        checkArgNotNull(FieldName.Attribute.FIELD_TYPE, type);
        this.type = type;
        return this;
    }

    /**
     * Gets the check rule.
     *
     * @return check rule.
     */
    public ScmAttrRule getCheckRule() {
        return checkRule;
    }

    /**
     * Sets the check rule.
     *
     * @param checkRule
     *            check rule.
     * @return the current instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmAttributeConf setCheckRule(ScmAttrRule checkRule) throws ScmException {
        this.checkRule = checkRule;
        return this;
    }

    /**
     * Whether attribute is required.
     *
     * @return true or false.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Sets attribute is required.
     *
     * @param required
     *            true or false.
     * @return the current instance.
     */
    public ScmAttributeConf setRequired(boolean required) {
        this.required = required;
        return this;
    }

    /**
     * Transform the config to a bson object.
     *
     * @return bson object.
     * @throws ScmException
     *             if error happens.
     */
    public BSONObject toBSONObject() throws ScmException {
        BSONObject info = new BasicBSONObject();

        checkArgNotNull(FieldName.Attribute.FIELD_NAME, getName());
        info.put(FieldName.Attribute.FIELD_NAME, getName());

        info.put(FieldName.Attribute.FIELD_DISPLAY_NAME, getDisplayName());

        info.put(FieldName.Attribute.FIELD_DESCRIPTION, getDescription());

        checkArgNotNull(FieldName.Attribute.FIELD_TYPE, getType());
        info.put(FieldName.Attribute.FIELD_TYPE, getType().getName());

        if (getCheckRule() != null) {
            info.put(FieldName.Attribute.FIELD_CHECK_RULE, getCheckRule().toBSONObject());
        }

        info.put(FieldName.Attribute.FIELD_REQUIRED, isRequired());

        return info;
    }

    private void checkArgNotNull(String argName, Object arg) throws ScmException {
        if (arg == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, argName + " cannot be null");
        }
    }
}
