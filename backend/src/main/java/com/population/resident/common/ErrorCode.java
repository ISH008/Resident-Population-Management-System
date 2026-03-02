package com.population.resident.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    BAD_REQUEST(4001, "参数错误"),
    UNAUTHORIZED(4003, "未授权"),
    NOT_FOUND(4004, "资源不存在"),
    CONFLICT(4009, "数据冲突"),
    INTERNAL_ERROR(5000, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
