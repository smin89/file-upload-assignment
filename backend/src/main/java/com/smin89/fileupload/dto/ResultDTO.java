package com.smin89.fileupload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** API의 공통 성공·실패 응답. HTTP 상태 코드는 Controller에서 지정한다. */
@Getter
@AllArgsConstructor
@Builder
public class ResultDTO<T> {
    private String resultCode;
    private String message;
    private T data;

    public static <T> ResultDTO<T> res(final String resultCode, final String message) {
        return res(resultCode, message, null);
    }

    public static <T> ResultDTO<T> res(final String resultCode, final String message, final T t) {
        return ResultDTO.<T>builder()
                .data(t)
                .resultCode(resultCode)
                .message(message)
                .build();
    }
}
