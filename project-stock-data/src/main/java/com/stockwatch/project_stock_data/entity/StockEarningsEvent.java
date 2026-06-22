package com.stockwatch.project_stock_data.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

// 財報發布事件（用於在 K 線圖上標示 "E" 標記）
@Data
@Entity
@Table(name = "stock_earnings_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "report_date"}))
public class StockEarningsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate reportDate;

    private BigDecimal epsActual;
    private BigDecimal epsEstimate;
    private Integer quarter;
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
}
