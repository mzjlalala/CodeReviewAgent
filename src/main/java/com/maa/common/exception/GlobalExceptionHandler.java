package com.maa.common.exception;

import com.maa.common.dto.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultMsg<?> handleNoResourceFound(NoResourceFoundException e) {
        // 浏览器请求 favicon.ico 等不存在的静态资源，无需打印 ERROR 日志
        return ResultMsg.notFound("资源不存在: " + e.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResultMsg<?> handleException(Exception e) {
        log.error("系统异常", e);
        return ResultMsg.internalError("系统繁忙，请稍后重试");
    }

}
