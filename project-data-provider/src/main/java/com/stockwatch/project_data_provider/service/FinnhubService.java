package com.stockwatch.project_data_provider.service;

import org.springframework.stereotype.Service;

import com.stockwatch.project_data_provider.dto.CompanyProfileDTO;
import com.stockwatch.project_data_provider.dto.StockQuoteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class FinnhubService {

    private final WebClient webClient;

    @Value("${finnhub.api.base-url}")
    private String baseUrl;

    @Value("${finnhub.api.key}")
    private String apiKey;

    public StockQuoteDTO getStockQuote(String symbol) {
        return webClient.get()
                .uri(baseUrl + "/quote?symbol={symbol}&token={token}", symbol, apiKey)
                .retrieve()
                .bodyToMono(StockQuoteDTO.class)
                .block();
    }

    public CompanyProfileDTO getCompanyProfile(String symbol) {
        return webClient.get()
                .uri(baseUrl + "/stock/profile2?symbol=" + symbol + "&token=" + apiKey)
                .retrieve()
                .bodyToMono(CompanyProfileDTO.class)
                .block();
    }
    
}
