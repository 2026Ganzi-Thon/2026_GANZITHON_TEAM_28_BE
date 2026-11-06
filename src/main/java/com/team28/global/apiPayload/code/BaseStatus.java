package com.team28.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public interface BaseStatus {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
