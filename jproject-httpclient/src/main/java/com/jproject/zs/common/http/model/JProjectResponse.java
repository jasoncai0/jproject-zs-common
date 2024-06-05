package com.jproject.zs.common.http.model;

import lombok.Data;


/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/7
 */
@Data
public class JProjectResponse<T>  {

    private String requestId;

    private Long ts;

    private Long ttl;

    private T data;

    private Integer code;

    private String message;


}
