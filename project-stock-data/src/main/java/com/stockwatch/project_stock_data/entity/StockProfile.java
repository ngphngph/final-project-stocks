package com.stockwatch.project_stock_data.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stock_profiles")
public class StockProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String industry;
    private String sector;
    private String description;
    private Double marketCap;
    private Double shareOutstanding;
    private String logo;
    private String ipoDate;
    private String webUrl;

    @OneToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
}
