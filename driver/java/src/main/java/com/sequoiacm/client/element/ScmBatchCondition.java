package com.sequoiacm.client.element;

/**
 * The condition of query batch.
 */
public class ScmBatchCondition {

    private String id;
    private String name;
    private ScmClassProperties properties;
    private String author;
    private long createTime;
    private String updateUserName;
    private long updateTime;

    /**
     * Returns the id of the condition.
     *
     * @return batch id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of the batch.
     *
     * @param id
     *            batch id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the name of the batch.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the batch
     *
     * @param name
     *            batch name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the properties.
     *
     * @return properties.
     */
    public ScmClassProperties getProperties() {
        return properties;
    }

    /**
     * Sets the properties.
     *
     * @param properties
     *            properties.
     */
    public void setProperties(ScmClassProperties properties) {
        this.properties = properties;
    }

    /**
     * Gets the author.
     *
     * @return Gets the author.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author.
     *
     * @param author
     *            author.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets created time.
     *
     * @return created time.
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * Sets created time.
     *
     * @param createTime
     *            created time.
     */
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    /**
     * Gets updated user.
     *
     * @return user name.
     */
    public String getUpdateUserName() {
        return updateUserName;
    }

    /**
     * Sets updated user.
     *
     * @param updateUserName
     *            user name.
     */
    public void setUpdateUserName(String updateUserName) {
        this.updateUserName = updateUserName;
    }

    /**
     * Gets updated time.
     *
     * @return updated time.
     */
    public long getUpdateTime() {
        return updateTime;
    }

    /**
     * Sets updated time.
     *
     * @param updateTime
     *            updated time.
     */
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
