package com.jproject.zs.common.cache.exception;

import lombok.Data;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/20
 */

@Data
public class JProjectCommonException extends RuntimeException {

    private int errorCode;

    private String errorMessage;


    public JProjectCommonException(int errorCode, String errorMessage) {
        this(errorMessage, errorCode, errorMessage);
    }

    public JProjectCommonException(int errorCode, String errorMessage, Throwable throwable) {
        this(errorMessage, throwable, errorCode, errorMessage);
    }

    public JProjectCommonException(String message, int errorCode, String errorMessage) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public JProjectCommonException(String message, Throwable cause, int errorCode, String errorMessage) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public JProjectCommonException(Throwable cause, int errorCode, String errorMessage) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
