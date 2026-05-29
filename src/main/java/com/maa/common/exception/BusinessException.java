package com.maa.common.exception;

import com.maa.common.dto.ResultMsg;
import lombok.Getter;

/**
 * 业务异常，由全局处理器转换为 ResultMsg
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ResultMsg.FAIL_CODE, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }

}
