package com.stockwatch.project_stock_data.service;

import com.stockwatch.project_stock_data.dto.HeatmapItemDTO;
import com.stockwatch.project_stock_data.dto.StockDetailDTO;
import com.stockwatch.project_stock_data.entity.Stock;
import com.stockwatch.project_stock_data.entity.StockOhlcData;
import com.stockwatch.project_stock_data.entity.StockProfile;
import com.stockwatch.project_stock_data.repository.StockOhlcRepository;
import com.stockwatch.project_stock_data.repository.StockProfileRepository;
import com.stockwatch.project_stock_data.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataService {

    private final StockProfileRepository stockProfileRepository;
    private final StockOhlcRepository stockOhlcRepository;
    private final StockRepository stockRepository;
    private final WebClient webClient;

    @Value("${data-provider.base-url}")
    private String dataProviderBaseUrl;

    @Cacheable(value = "heatmap-data")
    public List<HeatmapItemDTO> getHeatmapData() {
        return stockProfileRepository.findAllWithStock()
                .stream()
                .map(this::toHeatmapItemDTO)
                .peek(this::enrichWithLiveQuote)
                .toList();
    }

    @Cacheable(value = "stock-detail", key = "#symbol")
    public StockDetailDTO getStockDetail(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + symbol));
        return toStockDetailDTO(stock);
    }

    @Cacheable(value = "stock-detail-by-id", key = "#stockId")
    public StockDetailDTO getStockDetailById(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: id=" + stockId));
        return toStockDetailDTO(stock);
    }

    private StockDetailDTO toStockDetailDTO(Stock stock) {
        StockProfile profile = stock.getStockProfile();
        List<StockOhlcData> ohlcList = stockOhlcRepository.findByStockIdOrderByTradeDateAsc(stock.getId());

        StockDetailDTO dto = new StockDetailDTO();
        dto.setStockId(stock.getId());
        dto.setSymbol(stock.getSymbol());
        if (profile != null) {
            dto.setCompanyName(profile.getCompanyName());
            dto.setMarketCap(profile.getMarketCap());
            dto.setIndustry(profile.getIndustry());
            dto.setShareOutstanding(profile.getShareOutstanding());
            dto.setLogo(profile.getLogo());
        }
        dto.setOhlcs(ohlcList.stream().map(this::toOhlcDTO).toList());
        return dto;
    }

    // 呼叫 project-data-provider 取得即時報價（current price / change / change%），
    // 並填入 HeatmapItemDTO，讓 D3 Treemap 能依漲跌幅上色。
    @SuppressWarnings("unchecked")
    private void enrichWithLiveQuote(HeatmapItemDTO dto) {
        try {
            Map<String, Object> quote = webClient.get()
                    .uri(dataProviderBaseUrl + "/provider/quote/{symbol}", dto.getSymbol())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (quote != null) {
                // 注意：StockQuoteDTO 欄位上有 @JsonProperty("c"/"d"/"dp")，
                // Jackson 序列化輸出時會用這些短 key（沿用 Finnhub 原始格式），
                // 不是 Java 欄位名稱 currentPrice/change/changePercent。
                dto.setPrice(toDouble(quote.get("c")));
                dto.setMarketPriceChg(toDouble(quote.get("d")));
                dto.setMarketPriceChgPct(toDouble(quote.get("dp")));
            }
        } catch (Exception e) {
            // 暫時加上錯誤 log 以診斷為何 price 一直是 null
            log.warn("enrichWithLiveQuote failed for symbol={}, url={}, error={}: {}",
                    dto.getSymbol(), dataProviderBaseUrl, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return null;
    }

    private StockDetailDTO.OhlcDTO toOhlcDTO(StockOhlcData data) {
        StockDetailDTO.OhlcDTO dto = new StockDetailDTO.OhlcDTO();
        dto.setDate(data.getTradeDate().toString());
        dto.setOpen(data.getOpen() != null ? data.getOpen().doubleValue() : null);
        dto.setHigh(data.getHigh() != null ? data.getHigh().doubleValue() : null);
        dto.setLow(data.getLow() != null ? data.getLow().doubleValue() : null);
        dto.setClose(data.getClose() != null ? data.getClose().doubleValue() : null);
        dto.setVolume(data.getVolume());
        return dto;
    }

    private HeatmapItemDTO toHeatmapItemDTO(StockProfile profile) {
        HeatmapItemDTO dto = new HeatmapItemDTO();
        dto.setStockId(profile.getStock().getId());
        dto.setSymbol(profile.getStock().getSymbol());
        dto.setName(profile.getCompanyName());
        dto.setIndustry(profile.getIndustry());
        dto.setMarketCap(profile.getMarketCap());
        dto.setShareOutstanding(profile.getShareOutstanding());
        dto.setLogo(profile.getLogo());
        dto.setIpoDate(profile.getIpoDate());
        dto.setWebUrl(profile.getWebUrl());
        return dto;
    }
}
