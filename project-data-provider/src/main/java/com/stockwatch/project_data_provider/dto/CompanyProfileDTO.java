package com.stockwatch.project_data_provider.dto;

import lombok.Data;

@Data
public class CompanyProfileDTO {
    private String ticker;
    private String name;
    private String finnhubIndustry;
    private Long marketCapitalization;
    private String logo;
    private Double shareOutstanding;
    private String weburl;
    private String exchange;

    
}
