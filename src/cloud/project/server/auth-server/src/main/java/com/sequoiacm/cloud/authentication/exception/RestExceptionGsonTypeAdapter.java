// package com.sequoiacm.cloud.authentication.exception;
//
//import java.io.IOException;
//
//import com.google.gson.TypeAdapter;
//import com.google.gson.stream.JsonReader;
//import com.google.gson.stream.JsonWriter;
//
//public class RestExceptionGsonTypeAdapter extends TypeAdapter<RestException> {
//
//    @Override
//    public void write(JsonWriter out, RestException value) throws IOException {
//        out.beginObject();
//        out.name("timestamp").value(value.getTimestamp());
//        out.name("status").value(value.getStatus());
//        out.name("error").value(value.getError());
//        out.name("exception").value(value.getException());
//        out.name("message").value(value.getMessage());
//        out.name("path").value(value.getPath());
//        out.endObject();
//    }
//
//    @Override
//    public RestException read(JsonReader in) throws IOException {
//        throw new RuntimeException("do not support yet");
//    }
//}