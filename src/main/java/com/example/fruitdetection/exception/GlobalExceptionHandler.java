package com.example.fruitdetection.exception;

import com.example.fruitdetection.dto.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<String> handleException(Exception e) {
        return new BaseResponse<>(500, "系统异常: " + e.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return new BaseResponse<>(400, "参数错误: " + e.getMessage(), null);
    }
}