package com.stockwatch.project_stock_data.service;

import com.stockwatch.project_stock_data.entity.Stock;
import com.stockwatch.project_stock_data.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 背景排程器：用「輪流（round-robin）一次只查一支股票報價」的方式，
 * 持續、緩慢地更新所有股票的即時報價快取，避免在單一 /api/heatmap
 * 請求中同時對 Finnhub 發送 503 個請求而觸發免費版每分鐘 60 次的限速（429）。
 *
 * - 每 1.2 秒只打一支股票的報價（約 50 次/分鐘，低於 Finnhub 限制）。
 * - 503 支股票全部更新一輪大約需要 503 * 1.2s ≈ 10 分鐘。
 * - getHeatmapData() 直接讀這裡快取好的結果，不再同步呼叫 Finnhub，
 *   因此 API 回應速度不受 Finnhub 速率影響。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveQuoteService {

    private final StockRepository stockRepository;
    private final WebClient webClient;

    @Value("${data-provider.base-url}")
    private String dataProviderBaseUrl;

    private volatile List<String> symbols = List.of();
    private final AtomicInteger cursor = new AtomicInteger(0);
    private final Map<String, Quote> quotes = new ConcurrentHashMap<>();

    public record Quote(Double price, Double change, Double changePct) {
    }

    @PostConstruct
    void init() {
        symbols = stockRepository.findAll().stream()
                .map(Stock::getSymbol)
                .toList();
        log.info("LiveQuoteService 初始化完成，共 {} 支股票待輪流更新報價", symbols.size());
    }

    @Scheduled(fixedDelay = 1200)
    public void refreshNextSymbol() {
        List<String> list = symbols;
        if (list.isEmpty()) {
            return;
        }

        int idx = cursor.getAndUpdate(i -> (i + 1) % list.size());
        String symbol = list.get(idx);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> quote = webClient.get()
                    .uri(dataProviderBaseUrl + "/provider/quote/{symbol}", symbol)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (quote != null) {
                quotes.put(symbol, new Quote(
                        toDouble(quote.get("c")),
                        toDouble(quote.get("d")),
                        toDouble(quote.get("dp"))));
            }
        } catch (Exception e) {
            log.warn("refreshNextSymbol failed for symbol={}, error={}: {}",
                    symbol, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    public Quote getQuote(String symbol) {
        return quotes.get(symbol);
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return null;
    }
}
