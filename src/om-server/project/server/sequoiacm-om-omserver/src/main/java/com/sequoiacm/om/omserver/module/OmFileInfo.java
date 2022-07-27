package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import java.util.*;

public class OmFileInfo {

    @NotNull(message = "file name is required")
    @JsonProperty("name")
    private String name;

    @JsonProperty("directory_id")
    private String directoryId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("author")
    private String author;

    @JsonProperty("tags")
    private Set<String> tags;

    @JsonProperty("custom_metadata")
    private Map<String, String> customMetadata;

    @JsonProperty("class_id")
    private String classId;

    @JsonProperty("class_properties")
    private Map<String, Object> classProperties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectoryId() {
        return directoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getCustomMetadata() {
        return customMetadata;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public Map<String, Object> getClassProperties() {
        return classProperties;
    }
}
