package com.jproject.zs.common.http.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import retrofit2.Response;


/**
 * @author caizhensheng
 * @desc
 * @date 2022/2/23
 */
@Setter
@Getter
public class JProjectHttpException extends RuntimeException {

    private Integer code = -1;

    private String errorMsg;

    @Setter
    @Accessors(chain = true)
    private Response<?> response;

    public JProjectHttpException(Integer code, String errorBody) {
        super(errorBody);
        this.code = code;
        this.errorMsg = errorBody;
    }


    public JProjectHttpException(Throwable cause, Integer code, String errorMsg) {
        super(errorMsg, cause);
        this.code = code;
        this.errorMsg = errorMsg;
    }

}
