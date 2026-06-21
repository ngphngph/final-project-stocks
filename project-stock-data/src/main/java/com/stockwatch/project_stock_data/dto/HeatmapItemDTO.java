package com.stockwatch.project_stock_data.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapItemDTO {
    private Long stockId;
    private String symbol;
    private String name;
    private Double price;
    private Double marketPriceChg;      // 漲跌金額
    private Double marketPriceChgPct;   // 漲跌幅 %
    private Double marketCap;
    private String industry;
    private String sector;
    private String ipoDate;
    private String webUrl;
    private Double shareOutstanding;
    private String logo;
}
