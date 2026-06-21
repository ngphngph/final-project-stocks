package com.stockwatch.project_stock_data.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

// 把所有未被攔截的例外轉成有內容的 JSON 錯誤回應（exception class + message），
// 而不是預設的空白 500。這樣前端 / 直接用瀏覽器開 /data/ohlc?id=xx 就能直接看到
// 真正失敗的原因（例如 LazyInitializationException、Stock not found 等），方便除錯。
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("API call failed", ex);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getClass().getName());
        body.put("message", String.valueOf(ex.getMessage()));

        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status).body(body);
    }
}
