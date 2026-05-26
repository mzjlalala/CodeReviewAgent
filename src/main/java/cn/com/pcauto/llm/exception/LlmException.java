package cn.com.pcauto.llm.exception;

import cn.com.pcauto.common.exception.BusinessException;

public class LlmException extends BusinessException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

}
