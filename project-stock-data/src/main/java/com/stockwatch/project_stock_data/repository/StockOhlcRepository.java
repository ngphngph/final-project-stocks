package com.stockwatch.project_stock_data.repository;

import com.stockwatch.project_stock_data.entity.StockOhlcData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockOhlcRepository extends JpaRepository<StockOhlcData, Long> {
    List<StockOhlcData> findByStockIdOrderByTradeDateAsc(Long stockId);
}