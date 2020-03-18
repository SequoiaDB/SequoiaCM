package com.sequoiacm.cloud.authentication.controller;

import java.io.IOException;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmGsonTypeAdapter;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;

public class SignatureInfoGsonTypeAdapter extends ScmGsonTypeAdapter<String, SignatureInfo> {

    @Override
    public SignatureInfo convert(String source) {
        BSONObject bson = (BSONObject) JSON.parse(source);
        return new SignatureInfo(bson);
    }

    @Override
    public void write(JsonWriter out, SignatureInfo value) throws IOException {
        throw new RuntimeException("unsupport");
    }

    @Override
    public SignatureInfo read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }

}
