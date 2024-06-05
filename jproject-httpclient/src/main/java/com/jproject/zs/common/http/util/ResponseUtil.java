package com.jproject.zs.common.http.util;


import retrofit2.Call;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/9
 */
public class ResponseUtil {

    public static String buildErrorMsg(Call<?> call, String cause) {
        return String.format("Fail to call %s, cause: %s", call.request().url(), cause);
    }

}
