package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "Tag")
public class Tag {

    @JacksonXmlElementWrapper(localName = "Key", useWrapping = false)
    @JsonProperty("Key")
    private String key;

    @JacksonXmlElementWrapper(localName = "Value", useWrapping = false)
    @JsonProperty("Value")
    private String value;

    public Tag() {
    }

    public Tag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
