package com.stockwatch.project_stock_data.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

// 股票分割事件（用於在 K 線圖上標示 "S" 標記）
@Data
@Entity
@Table(name = "stock_split_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "split_date"}))
public class StockSplitEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate splitDate;

    // 例如 4-for-1 分割：fromFactor=1, toFactor=4
    private BigDecimal fromFactor;
    private BigDecimal toFactor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
}
