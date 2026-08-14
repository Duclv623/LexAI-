package com.chatboxai.chat_service.dto.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"data", "message"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponse<T> implements Serializable {

    private T data;
    private String message;
    private boolean success;

    public CustomResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public CustomResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CustomResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }
}
