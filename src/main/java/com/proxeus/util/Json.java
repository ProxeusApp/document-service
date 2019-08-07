package com.proxeus.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class Json {
    public static ObjectMapper mapper = new ObjectMapper();

    static{
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static String toJson(Object obj){
        try {
            return mapper.writeValueAsString(obj);
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T fromJson(String json, Class<T> clazz){
        try {
            return mapper.readValue(json, clazz);
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T fromJson(byte[] json, Class<T> clazz){
        try {
            return mapper.readValue(json, clazz);
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T fromJson(InputStream json, Class<T> clazz){
        try {
            return mapper.readValue(json, clazz);
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}