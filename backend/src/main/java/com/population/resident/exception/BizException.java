package com.population.resident.exception;

import com.population.resident.common.ErrorCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
