package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.sequoiacm.s3.core.Part;

import java.util.List;

@JacksonXmlRootElement(localName = "CompleteMultipartUpload")
public class CompleteMultipartUpload {
    @JacksonXmlElementWrapper(localName = "Part", useWrapping = false)
    @JsonProperty("Part")
    private List<CompletePart> part;

    public void setPart(List<CompletePart> part) {
        this.part = part;
    }

    public List<CompletePart> getPart() {
        return part;
    }
}
