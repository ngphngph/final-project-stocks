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
    private List<EarningsEventDTO> earningsEvents;
    private List<SplitEventDTO> splitEvents;

    @Data
    public static class OhlcDTO {
        private String date;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
    }

    // 財報發布事件，給前端在 K 線圖上畫 "E" 標記
    @Data
    public static class EarningsEventDTO {
        private String date;
        private Double epsActual;
        private Double epsEstimate;
        private Integer quarter;
        private Integer year;
    }

    // 股票分割事件，給前端在 K 線圖上畫 "S" 標記
    @Data
    public static class SplitEventDTO {
        private String date;
        private Double fromFactor;
        private Double toFactor;
    }
}