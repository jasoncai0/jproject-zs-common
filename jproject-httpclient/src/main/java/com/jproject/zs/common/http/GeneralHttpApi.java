package com.jproject.zs.common.http;

import com.jproject.zs.common.http.exception.JProjectHttpException;
import com.jproject.zs.common.http.model.JProjectResponse;
import com.jproject.zs.common.http.util.ResponseUtil;
import io.vavr.control.Try;
import java.util.function.Predicate;
import okio.Options;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/7
 */
public interface GeneralHttpApi {

    default <T> T call(Call<T> call) {
        return call(call, null);
    }


    default <T> T call(Call<T> call, Options options) {
        try {
            Response<T> response = options == null ? call.execute() : call.execute(options); //这里触发执行请求

            if (!response.isSuccessful()) {
                throw new JProjectHttpException(response.code(),
                        ResponseUtil.buildErrorMsg(call, Try.of(() -> response.errorBody().string()).getOrElse("")));

            }

            return response.body();

        } catch (JProjectHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new JProjectHttpException(e, -1, ResponseUtil.buildErrorMsg(call, e.getMessage()));
        }
    }

    default <T> T callWithDataResponse(Call<JProjectResponse<T>> call) {
        return callWithDataResponse(call,
                null,
                response -> !response.isSuccess());
    }

    default <T> T callWithDataResponse(Call<JProjectResponse<T>> call, Options options) {
        return callWithDataResponse(call,
                options,
                response -> !response.isSuccess());
    }

    default <T> T callWithDataResponse(Call<JProjectResponse<T>> call,
            Predicate<JProjectResponse<T>> dataResponseFailurePredicate) {
        return callWithDataResponse(call,
                null,
                dataResponseFailurePredicate);
    }

    default <T> T callWithDataResponse(
            Call<JProjectResponse<T>> call, Options options,
            Predicate<JProjectResponse<T>> dataResponseFailurePredicate) {

        JProjectResponse<T> response = call(call, options);

        if (response == null) {
            throw new JProjectHttpException(-1, ResponseUtil.buildErrorMsg(call, "empty response"));
        }

        if (dataResponseFailurePredicate.test(response)) {
            throw new JProjectHttpException(response.getCode(), ResponseUtil.buildErrorMsg(call, response.getMessage()));
        }

        return response.getData();
    }


}
