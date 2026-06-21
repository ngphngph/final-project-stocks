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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataService {

    private final StockProfileRepository stockProfileRepository;
    private final StockOhlcRepository stockOhlcRepository;
    private final StockRepository stockRepository;
    private final LiveQuoteService liveQuoteService;

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

    // 不再同步呼叫 Finnhub，改成讀取 LiveQuoteService 背景排程持續更新好的快取，
    // 避免 503 支股票同時打 Finnhub 觸發 429 限速。
    private void enrichWithLiveQuote(HeatmapItemDTO dto) {
        LiveQuoteService.Quote quote = liveQuoteService.getQuote(dto.getSymbol());
        if (quote != null) {
            dto.setPrice(quote.price());
            dto.setMarketPriceChg(quote.change());
            dto.setMarketPriceChgPct(quote.changePct());
        }
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
        dto.setSector(SectorMapper.mapSector(profile.getIndustry()));
        dto.setMarketCap(profile.getMarketCap());
        dto.setShareOutstanding(profile.getShareOutstanding());
        dto.setLogo(profile.getLogo());
        dto.setIpoDate(profile.getIpoDate());
        dto.setWebUrl(profile.getWebUrl());
        return dto;
    }
}
