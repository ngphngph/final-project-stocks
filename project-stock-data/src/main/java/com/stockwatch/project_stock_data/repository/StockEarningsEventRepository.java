package com.stockwatch.project_stock_data.repository;

import com.stockwatch.project_stock_data.entity.StockEarningsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockEarningsEventRepository extends JpaRepository<StockEarningsEvent, Long> {
    List<StockEarningsEvent> findByStockIdOrderByReportDateAsc(Long stockId);
}
