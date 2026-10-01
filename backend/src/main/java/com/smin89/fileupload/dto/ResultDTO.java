package com.smin89.fileupload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** API의 공통 성공·실패 응답. HTTP 상태 코드는 Controller 또는 전역 예외 처리기에서 지정한다. */
@Getter
@AllArgsConstructor
@Builder
public class ResultDTO<T> {
    private String resultCode;
    private String message;
    // 성공 데이터 또는 오류 부가 정보. 데이터가 없으면 null로 반환한다.
    private T data;

    /** 응답 데이터가 없는 결과를 생성한다. */
    public static <T> ResultDTO<T> res(final String resultCode, final String message) {
        return res(resultCode, message, null);
    }

    /** API별 데이터를 공통 형식으로 감싼다. 이 메서드 자체는 HTTP 상태를 변경하지 않는다. */
    public static <T> ResultDTO<T> res(final String resultCode, final String message, final T t) {
        return ResultDTO.<T>builder()
                .data(t)
                .resultCode(resultCode)
                .message(message)
                .build();
    }
}
