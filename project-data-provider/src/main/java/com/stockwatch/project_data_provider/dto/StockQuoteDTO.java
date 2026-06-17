package com.stockwatch.project_data_provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class StockQuoteDTO {
    @JsonProperty("c")
    private Double currentPrice;

    @JsonProperty("d")
    private Double change;

    @JsonProperty("dp")
    private Double changePercent;

    @JsonProperty("h")
    private Double high;

    @JsonProperty("l")
    private Double low;

    @JsonProperty("o")
    private Double open;

    @JsonProperty("pc")
    private Double previousClose;

}
