package com.stockwatch.project_heatmap_ui.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HeatmapController {

    private final WebClient webClient;

    @Value("${stock-data.base-url}")
    private String stockDataBaseUrl;

    // GET / → 返回 index.html（熱圖頁面）
    @GetMapping("/")
    public String index(Model model) {
        // 伺服器端初次渲染，之後 JS setInterval 自動刷新
        return "index";
    }

    // GET /stock/{stockId} → 返回 stock.html（K線圖頁面）
    @GetMapping("/stock/{stockId}")
    public String stockPage(@PathVariable Long stockId, Model model) {
        model.addAttribute("stockId", stockId);
        return "stock";
    }

    // REST API → 給前端 JS 呼叫，取 heatmap 數據
    @RestController
    @RequestMapping("/api")
    class ApiController {

        @GetMapping("/heatmap")
        public Object getHeatmap() {
            return webClient.get()
                    .uri(stockDataBaseUrl + "/data/heatmap")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        }

        @GetMapping("/ohlc")
        public Object getOhlc(@RequestParam Long id) {
            return webClient.get()
                    .uri(stockDataBaseUrl + "/data/ohlc?id=" + id)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        }
    }
}