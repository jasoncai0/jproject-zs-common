package com.jproject.zs.common.metrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/26
 */
public class JsonUtils {


    private static Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.enableComplexMapKeySerialization();
        gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        gson = gsonBuilder.create();

    }


    public static Map<String, Object> readToMap(String str) {
        Map<String, Object> emptyMap = new HashMap<String, Object>();
        if (str == null || str.length() == 0) {
            return emptyMap;
        }
        return gson.fromJson(str, new TypeToken<Map<String, Object>>() {
        }.getType());

    }

    public static <T> T read(String json, TypeToken<T> typeToken) {
        if (json == null) {
            return null;
        }

        try {

            return gson.fromJson(json, typeToken.getType());
        } catch (JsonSyntaxException e) {
            return null;
        }
    }


}
