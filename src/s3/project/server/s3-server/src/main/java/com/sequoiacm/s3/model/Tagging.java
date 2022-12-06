package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JacksonXmlRootElement(localName = "Tagging")
public class Tagging {

    @JacksonXmlElementWrapper(localName = "TagSet", useWrapping = false)
    @JsonProperty("TagSet")
    private List<TagSet> tagList;

    public Tagging() {
    }

    public Tagging(Map<String, String> map) {
        List<Tag> tagList = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            tagList.add(new Tag(entry.getKey(), entry.getValue()));
        }
        TagSet tagSet = new TagSet(tagList);
        List<TagSet> list = new ArrayList<>();
        list.add(tagSet);
        this.tagList = list;
    }

    public Tagging(List<TagSet> tagList) {
        this.tagList = tagList;
    }

    public List<TagSet> getTagList() {
        return tagList;
    }

    public void setTagList(List<TagSet> tagList) {
        this.tagList = tagList;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        List<TagSet> tagSets = this.tagList;
        for (TagSet tagSet : tagSets) {
            List<Tag> tagList = tagSet.getTag();
            if (tagList != null && tagList.size() > 0) {
                for (Tag tag : tagList) {
                    map.put(tag.getKey(), tag.getValue());
                }
            }
        }
        return map;
    }
}
