package com.example.fruitdetection.dto;

import lombok.Data;

@Data
public class BaseResponse<T> {
    private int code;
    private String message;
    private T data;

    public BaseResponse() {
        this.code = 200;
        this.message = "success";
    }

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(T data) {
        this.code = 200;
        this.message = "success";
        this.data = data;
    }
}