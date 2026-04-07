package com.myproject.service_ai_assistant.exception;

import com.myproject.service_ai_assistant.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;
    private final String message;

    /**
     * 使用默认业务错误码
     */
    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    /**
     * 自定义错误码和消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 使用枚举错误码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 使用枚举错误码 + 动态参数（支持 String.format）
     * 例如：ResultCode.ACCOUNT_LOCKED + 5 → "账号已被锁定，请 5 分钟后再试"
     */
    public BusinessException(ResultCode resultCode, Object... args) {
        super(String.format(resultCode.getMessage(), args));
        this.code = resultCode.getCode();
        this.message = String.format(resultCode.getMessage(), args);
    }
}
