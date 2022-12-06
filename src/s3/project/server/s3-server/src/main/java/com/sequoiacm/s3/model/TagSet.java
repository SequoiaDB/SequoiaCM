package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "Tagging")
public class TagSet {

    @JacksonXmlElementWrapper(localName = "Tag", useWrapping = false)
    @JsonProperty("Tag")
    private List<Tag> tag;

    public TagSet() {
    }

    public TagSet(List<Tag> tag) {
        this.tag = tag;
    }

    public List<Tag> getTag() {
        return tag;
    }

    public void setTag(List<Tag> tags) {
        this.tag = tags;
    }
}
