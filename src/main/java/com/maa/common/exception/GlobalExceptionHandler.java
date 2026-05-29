package com.maa.common.exception;

import com.maa.common.dto.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一包装为 ResultMsg
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResultMsg<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ResultMsg.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResultMsg<?> handleMissingParam(MissingServletRequestParameterException e) {
        return ResultMsg.badRequest("缺少请求参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultMsg<?> handleNotReadable(HttpMessageNotReadableException e) {
        return ResultMsg.badRequest("请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ResultMsg<?> handleException(Exception e) {
        log.error("系统异常", e);
        return ResultMsg.internalError("系统繁忙，请稍后重试");
    }

}
