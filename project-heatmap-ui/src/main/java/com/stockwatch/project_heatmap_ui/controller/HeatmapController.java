package com.stockwatch.project_heatmap_ui.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 前端已經拆成獨立的靜態網站部署在 Vercel，這個 class 不再負責渲染網頁，
// 只保留下面的 REST API 給 Vercel 前端（以及任何其他 client）呼叫。
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatmapController {

    private final WebClient webClient;

    @Value("${stock-data.base-url}")
    private String stockDataBaseUrl;

    // REST API → 給前端 JS 呼叫，取 heatmap 數據
    @RestController
    @RequestMapping("/api")
    class ApiController {

        @GetMapping("/heatmap")
        public ResponseEntity<Object> getHeatmap() {
            return proxyGet(stockDataBaseUrl + "/data/heatmap");
        }

        @GetMapping("/ohlc")
        public ResponseEntity<Object> getOhlc(@RequestParam Long id) {
            return proxyGet(stockDataBaseUrl + "/data/ohlc?id=" + id);
        }

        // 把下游 project-stock-data 回傳的真實狀態碼跟錯誤內容原封不動轉給前端，
        // 不要讓 WebClientResponseException 被吞掉變成沒有內容的泛用 500，
        // 這樣前端 / 直接開瀏覽器看這個 API 網址時就能看到真正的錯誤訊息方便除錯。
        private ResponseEntity<Object> proxyGet(String uri) {
            try {
                Object body = webClient.get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .block();
                return ResponseEntity.ok(body);
            } catch (WebClientResponseException ex) {
                HttpStatusCode status = ex.getStatusCode();
                String responseBody = ex.getResponseBodyAsString();
                log.error("Downstream call to {} failed: status={}, body={}", uri, status, responseBody, ex);
                return ResponseEntity.status(status).body(responseBody);
            } catch (Exception ex) {
                log.error("Downstream call to {} failed unexpectedly", uri, ex);
                return ResponseEntity.internalServerError()
                        .body("Proxy error calling " + uri + ": " + ex.getMessage());
            }
        }
    }
}