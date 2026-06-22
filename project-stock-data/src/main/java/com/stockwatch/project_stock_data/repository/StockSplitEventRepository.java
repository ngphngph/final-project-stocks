package com.stockwatch.project_stock_data.repository;

import com.stockwatch.project_stock_data.entity.StockSplitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockSplitEventRepository extends JpaRepository<StockSplitEvent, Long> {
    List<StockSplitEvent> findByStockIdOrderBySplitDateAsc(Long stockId);
}
