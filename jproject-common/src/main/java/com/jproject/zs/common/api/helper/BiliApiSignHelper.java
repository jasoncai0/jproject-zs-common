package com.jproject.zs.common.api.helper;

/**
 *
 */

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: zhangjing
 * Date: 14-8-13
 * Time: 下午2:05
 * To change this template use File | Settings | File Templates.
 *
 * --------
 *
 * 仅做兼容迁移使用， 不会提供后续支持
 *
 * @author caizhensheng
 */
public class ApiSignHelper {


    public static boolean checkSign(Map<String, String[]> params, String appSecret, String sign) throws UnsupportedEncodingException {
        return sign.equals(getSign(params, appSecret));
    }

    private static String getSign(Map<String, String[]> params, String appSecret) throws UnsupportedEncodingException {
        TreeMap<String, String> paramsTreeMap = new TreeMap<String, String>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (!"sign".equals(entry.getKey()) && entry.getValue().length > 0) {
                paramsTreeMap.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        return getSign(paramsTreeMap, appSecret);
    }

    public static String getSign(List<NameValuePair> nvps, String appSecret) throws UnsupportedEncodingException {
        TreeMap<String, String> paramsTreeMap = new TreeMap<String, String>();
        for (NameValuePair nameValuePair : nvps) {
            paramsTreeMap.put(nameValuePair.getName(), nameValuePair.getValue());
        }
        return getSign(paramsTreeMap, appSecret);
    }

    public static String getBigFunSign(List<NameValuePair> nvps, String appSecret) {
        TreeMap<String, String> paramsTreeMap = new TreeMap<>();
        for (NameValuePair nameValuePair : nvps) {
            paramsTreeMap.put(nameValuePair.getName(), nameValuePair.getValue());
        }
        return getSignWithoutEncodeValue(paramsTreeMap, appSecret);
    }

    /**
     * 根据Map中的key按字母排序后，将value直接拼接，最后拼上appSecret。再MDS加密。
     *
     * @param paramsTreeMap
     * @param appSecret
     * @return
     */
    public static String getSignByJoinValues(TreeMap<String, String> paramsTreeMap, String appSecret) {
        String signCalc = String.join("", paramsTreeMap.values());
        return DigestUtils.md5Hex(String.format("%s%s", signCalc, appSecret));
    }

    public static String getSign(TreeMap<String, String> paramsTreeMap, String appSecret) throws UnsupportedEncodingException {
        String signCalc = "";
        for (Map.Entry<String, String> entry : paramsTreeMap.entrySet()) {
            String key = entry.getKey(); // map中的key
            String value = "";
            value = String.valueOf(entry.getValue());
            signCalc = String.format("%s%s=%s&", signCalc, key, encodedFix(URLEncoder.encode(value, "UTF-8")));
        }
        if (signCalc.length() > 0) {
            signCalc = signCalc.substring(0, signCalc.length() - 1);
        }
        signCalc = DigestUtils.md5Hex(String.format("%s%s", signCalc, appSecret));
        return signCalc;
    }

    public static String getSignWithKey(List<NameValuePair> nvps, String appSecret) throws UnsupportedEncodingException {
        TreeMap<String, String> paramsTreeMap = new TreeMap<String, String>();
        for (NameValuePair nameValuePair : nvps) {
            paramsTreeMap.put(nameValuePair.getName(), nameValuePair.getValue());
        }
        return getSignWithKey(paramsTreeMap, appSecret);
    }

    public static String getSignWithKey(TreeMap<String, String> paramsTreeMap, String appSecret) throws UnsupportedEncodingException {
        String signCalc = "";
        for (Map.Entry<String, String> entry : paramsTreeMap.entrySet()) {
            String key = entry.getKey(); // map中的key
            String value = "";
            value = String.valueOf(entry.getValue());
            signCalc = String.format("%s%s=%s&", signCalc, key, encodedFix(URLEncoder.encode(value, "UTF-8")));
        }
        if (signCalc.length() > 0) {
            signCalc = signCalc.substring(0, signCalc.length() - 1);
        }
        String str = String.format("%s%s", signCalc, "&key=" + appSecret);
        signCalc = DigestUtils.md5Hex(str.toLowerCase());
        return signCalc;
    }

    /**
     * 不对value进行UrlEncode
     */
    public static String getSignWithoutEncodeValue(TreeMap<String, String> paramsTreeMap, String appSecret) {
        String signCalc = "";
        for (Map.Entry<String, String> entry : paramsTreeMap.entrySet()) {
            String key = entry.getKey(); // map中的key
            String value = String.valueOf(entry.getValue());
            signCalc = String.format("%s%s=%s&", signCalc, key, value);
        }
        if (signCalc.length() > 0) {
            signCalc = signCalc.substring(0, signCalc.length() - 1);
        }
        signCalc = DigestUtils.md5Hex(String.format("%s%s", signCalc, appSecret));
        return signCalc;
    }


    private static String encodedFix(String encoded) {
        // required
        encoded = encoded.replace("+", "%20");
        encoded = encoded.replace("*", "%2A");
        encoded = encoded.replace("%7E", "~");

        // optional
        encoded = encoded.replace("!", "%21");
        encoded = encoded.replace("(", "%28");
        encoded = encoded.replace(")", "%29");
        encoded = encoded.replace("'", "%27");
        return encoded;
    }
}
