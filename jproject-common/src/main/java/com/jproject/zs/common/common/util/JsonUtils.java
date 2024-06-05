package com.jproject.zs.common.common.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON的工具类
 */
public class JsonUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

    private static final Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.enableComplexMapKeySerialization();
        gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.IDENTITY);

        gson = gsonBuilder.create();
    }

    private static final List<Object> emptyList = new ArrayList<Object>();

    public static <T> T fromJson(String json, Class<T> beanClass) {
        return gson.fromJson(json, beanClass);
    }

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    public static String toJson(Map<String, String> params) {
        return gson.toJson(params);
    }

    public static <T> String toJson(T src) {
        return gson.toJson(src);
    }
    
    public static <T> String toString(T src) {
        try {
            return gson.toJson(src);
        } catch (Exception e) {
            LOGGER.error("Object to json string failed", e);
            return null;
        }
    }

    public static String toNotifyJson(Map<String, Object> params) {
        return gson.toJson(params);
    }

    public static Map<String, String> jsonToMap(String json) {
        return gson.fromJson(json, new TypeToken<Map<String, String>>() {
        }.getType());
    }
}
