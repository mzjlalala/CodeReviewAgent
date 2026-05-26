package cn.com.pcauto.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 返回结构
 * <p>
 * code=0 表示成功；非 0 表示失败，详见 msg。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultMsg<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功 */
    public static final int CODE_SUCCESS = 0;

    /** 通用失败 */
    public static final int FAIL_CODE = -1;


    /** 参数错误 */
    public static final int BAD_REQUEST_CODE = 400;

    /** 未授权 */
    public static final int UNAUTHORIZED_CODE = 401;

    /** 禁止访问 */
    public static final int FORBIDDEN_CODE = 403;

    /** 资源不存在 */
    public static final int NOT_FOUND_CODE = 404;

    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR_CODE = 500;

    /**
     * 响应编码，0 正常，非 0 异常
     */
    private int code;

    /**
     * 响应说明
     */
    private String msg;

    /**
     * 业务数据
     */
    private T data;

    public boolean isSuccess() {
        return code == CODE_SUCCESS;
    }

    // ---------- 成功 ----------

    public static <T> ResultMsg<T> ok() {
        return ok("success", null);
    }

    public static <T> ResultMsg<T> ok(T data) {
        return ok("success", data);
    }

    public static <T> ResultMsg<T> ok(String msg) {
        return ok(msg, null);
    }

    public static <T> ResultMsg<T> ok(String msg, T data) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setCode(CODE_SUCCESS);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // ---------- 失败（通用） ----------

    public static <T> ResultMsg<T> fail(String msg) {
        return fail(FAIL_CODE, msg);
    }

    public static <T> ResultMsg<T> fail(int code, String msg) {
        return fail(code, msg, null);
    }

    public static <T> ResultMsg<T> fail(int code, String msg, T data) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // ---------- 失败（常用 HTTP 语义） ----------

    public static <T> ResultMsg<T> badRequest(String msg) {
        return fail(BAD_REQUEST_CODE, msg);
    }

    public static <T> ResultMsg<T> unauthorized(String msg) {
        return fail(UNAUTHORIZED_CODE, msg);
    }

    public static <T> ResultMsg<T> forbidden(String msg) {
        return fail(FORBIDDEN_CODE, msg);
    }

    public static <T> ResultMsg<T> notFound(String msg) {
        return fail(NOT_FOUND_CODE, msg);
    }

    public static <T> ResultMsg<T> internalError(String msg) {
        return fail(INTERNAL_ERROR_CODE, msg);
    }

    /**
     * 根据已有 ResultMsg 复制并替换 data
     */
    public static <T> ResultMsg<T> of(ResultMsg<?> source, T data) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setCode(source.getCode());
        result.setMsg(source.getMsg());
        result.setData(data);
        return result;
    }

}
