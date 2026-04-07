package com.myproject.service_ai_assistant.common;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    
    // ========== 客户端错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "拒绝访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // ========== 服务端错误 5xx ==========
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    // ========== 认证相关 (1xxx) ==========
    AUTH_FAILED(1001, "用户名或密码错误"),
    ACCOUNT_LOCKED(1002, "账号已被锁定，请 %d 分钟后再试"),
    ACCOUNT_DISABLED(1003, "账号已被禁用，请联系管理员"),
    TOKEN_EXPIRED(1004, "登录已过期，请重新登录"),
    TOKEN_INVALID(1005, "无效的 Token"),
    MULTI_LOGIN(1006, "您的账号已在其他设备登录，请重新登录"),
    TENANT_CODE_REQUIRED(1007, "请输入租户编码"),
    
    // ========== 租户相关 (2xxx) ==========
    TENANT_NOT_FOUND(2001, "租户不存在"),
    TENANT_DISABLED(2002, "租户已禁用"),
    TENANT_EXPIRED(2003, "租户已过期"),
    TENANT_ID_REQUIRED(2004, "租户 ID 不能为空"),
    TENANT_CODE_EXISTS(2005, "租户编码已存在"),
    TENANT_DELETE_FAILED(2006, "只能删除已禁用的租户，请先禁用该租户"),
    TENANT_UPDATE_FAILED(2007, "更新租户信息失败"),
    
    // ========== 业务通用 (3xxx) ==========
    BUSINESS_ERROR(3001, "业务异常"),
    DATA_NOT_FOUND(3002, "数据不存在"),
    DATA_ALREADY_EXISTS(3003, "数据已存在"),
    PARAM_VALIDATION_ERROR(3004, "参数校验失败"),
    OPERATION_FAILED(3005, "操作失败"),
    
    // ========== 用户相关 (31xx) ==========
    USER_NOT_FOUND(3101, "用户不存在"),
    USERNAME_EXISTS(3102, "用户名已存在"),
    PHONE_EXISTS(3103, "手机号已存在"),
    OLD_PASSWORD_WRONG(3104, "原密码错误"),
    PASSWORD_CHANGE_FAILED(3105, "修改密码失败"),
    PASSWORD_RESET_FAILED(3106, "重置密码失败"),
    USER_CREATE_FAILED(3107, "创建用户失败"),
    USER_UPDATE_FAILED(3108, "更新用户状态失败"),
    USER_DELETE_FAILED(3109, "删除用户失败"),
    DELETE_SUPER_ADMIN_FORBIDDEN(3110, "不允许删除超级管理员账号"),
    PERMISSION_DENIED(3111, "您没有权限访问用户管理"),
    
    // ========== 知识库相关 (4xxx) ==========
    KNOWLEDGE_NOT_FOUND(4001, "知识条目不存在"),
    CATEGORY_NOT_FOUND(4002, "分类不存在"),
    
    // ========== 咨询反馈相关 (5xxx) ==========
    FEEDBACK_NOT_FOUND(5001, "反馈记录不存在"),
    CONSULTATION_NOT_FOUND(5002, "咨询记录不存在"),
    FEEDBACK_FORMAT_ERROR(5003, "反馈原因格式错误"),
    
    // ========== 文件相关 (6xxx) ==========
    FILE_UPLOAD_ERROR(6001, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORTED(6002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(6003, "文件大小超出限制"),
    
    // ========== AI 服务相关 (7xxx) ==========
    AI_SERVICE_ERROR(7001, "AI 服务调用失败"),
    AI_SERVICE_TIMEOUT(7002, "AI 服务响应超时");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
