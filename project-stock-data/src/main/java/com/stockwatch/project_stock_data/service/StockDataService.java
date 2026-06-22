package com.stockwatch.project_stock_data.service;

import com.stockwatch.project_stock_data.dto.HeatmapItemDTO;
import com.stockwatch.project_stock_data.dto.StockDetailDTO;
import com.stockwatch.project_stock_data.entity.Stock;
import com.stockwatch.project_stock_data.entity.StockEarningsEvent;
import com.stockwatch.project_stock_data.entity.StockOhlcData;
import com.stockwatch.project_stock_data.entity.StockProfile;
import com.stockwatch.project_stock_data.entity.StockSplitEvent;
import com.stockwatch.project_stock_data.repository.StockEarningsEventRepository;
import com.stockwatch.project_stock_data.repository.StockOhlcRepository;
import com.stockwatch.project_stock_data.repository.StockProfileRepository;
import com.stockwatch.project_stock_data.repository.StockRepository;
import com.stockwatch.project_stock_data.repository.StockSplitEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataService {

    private final StockProfileRepository stockProfileRepository;
    private final StockOhlcRepository stockOhlcRepository;
    private final StockRepository stockRepository;
    private final StockEarningsEventRepository stockEarningsEventRepository;
    private final StockSplitEventRepository stockSplitEventRepository;
    private final LiveQuoteService liveQuoteService;

    @Cacheable(value = "heatmap-data")
    public List<HeatmapItemDTO> getHeatmapData() {
        return stockProfileRepository.findAllWithStock()
                .stream()
                .map(this::toHeatmapItemDTO)
                .peek(this::enrichWithLiveQuote)
                .toList();
    }

    // 加 @Transactional 是因為 stock.getStockProfile() 是 LAZY @OneToOne，
    // findById/findBySymbol 本身的 transaction 結束後 session 就關了，
    // 之後才存取 getStockProfile() 會丟 LazyInitializationException，
    // 在 controller 端變成沒被攔截的 500（前端顯示「Failed to load stock data.」）。
    // 把整個方法包進同一個 transaction，讓 lazy load 能在 session 還開著時完成。
    @Transactional(readOnly = true)
    @Cacheable(value = "stock-detail", key = "#symbol")
    public StockDetailDTO getStockDetail(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + symbol));
        return toStockDetailDTO(stock);
    }

    @Transactional(readOnly = true)
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

        List<StockEarningsEvent> earningsList =
                stockEarningsEventRepository.findByStockIdOrderByReportDateAsc(stock.getId());
        dto.setEarningsEvents(earningsList.stream().map(this::toEarningsEventDTO).toList());

        List<StockSplitEvent> splitList =
                stockSplitEventRepository.findByStockIdOrderBySplitDateAsc(stock.getId());
        dto.setSplitEvents(splitList.stream().map(this::toSplitEventDTO).toList());

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

    private StockDetailDTO.EarningsEventDTO toEarningsEventDTO(StockEarningsEvent event) {
        StockDetailDTO.EarningsEventDTO dto = new StockDetailDTO.EarningsEventDTO();
        dto.setDate(event.getReportDate().toString());
        dto.setEpsActual(event.getEpsActual() != null ? event.getEpsActual().doubleValue() : null);
        dto.setEpsEstimate(event.getEpsEstimate() != null ? event.getEpsEstimate().doubleValue() : null);
        dto.setQuarter(event.getQuarter());
        dto.setYear(event.getYear());
        return dto;
    }

    private StockDetailDTO.SplitEventDTO toSplitEventDTO(StockSplitEvent event) {
        StockDetailDTO.SplitEventDTO dto = new StockDetailDTO.SplitEventDTO();
        dto.setDate(event.getSplitDate().toString());
        dto.setFromFactor(event.getFromFactor() != null ? event.getFromFactor().doubleValue() : null);
        dto.setToFactor(event.getToFactor() != null ? event.getToFactor().doubleValue() : null);
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
