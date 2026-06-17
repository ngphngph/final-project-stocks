package com.stockwatch.project_stock_data.dto;

import lombok.Data;
import java.util.List;

@Data
public class StockDetailDTO {
    private Long stockId;
    private String symbol;
    private String companyName;
    private Double marketCap;
    private String industry;
    private Double shareOutstanding;
    private String logo;
    private List<OhlcDTO> ohlcs;

    @Data
    public static class OhlcDTO {
        private String date;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
    }
}