package com.stockwatch.project_stock_data.controller;

import com.stockwatch.project_stock_data.dto.HeatmapItemDTO;
import com.stockwatch.project_stock_data.dto.StockDetailDTO;
import com.stockwatch.project_stock_data.service.StockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class StockDataController {

    private final StockDataService stockDataService;

    // GET /data/heatmap → 給 project-heatmap-ui 抓全部股票熱圖資料
    @GetMapping("/heatmap")
    public List<HeatmapItemDTO> getHeatmap() {
        return stockDataService.getHeatmapData();
    }

    // GET /data/ohlc?id={stockId} → 給 project-heatmap-ui 抓單一股票的 K 線歷史資料
    @GetMapping("/ohlc")
    public StockDetailDTO getOhlc(@RequestParam Long id) {
        return stockDataService.getStockDetailById(id);
    }
}
