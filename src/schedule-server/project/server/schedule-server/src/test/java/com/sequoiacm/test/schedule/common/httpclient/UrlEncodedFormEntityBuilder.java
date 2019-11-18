package com.sequoiacm.test.schedule.common.httpclient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

public class UrlEncodedFormEntityBuilder {
    List<NameValuePair> parmList;

    public UrlEncodedFormEntityBuilder() {
        parmList = new ArrayList<NameValuePair>();
    }

    public UrlEncodedFormEntityBuilder addNameValue(String name, String value) {
        parmList.add(new BasicNameValuePair(name, value));
        return this;
    }

    public UrlEncodedFormEntity build() throws UnsupportedEncodingException {
        return new UrlEncodedFormEntity(parmList);
    }
}
