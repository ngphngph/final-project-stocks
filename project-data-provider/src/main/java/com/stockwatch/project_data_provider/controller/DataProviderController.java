package com.stockwatch.project_data_provider.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockwatch.project_data_provider.dto.CompanyProfileDTO;
import com.stockwatch.project_data_provider.dto.StockQuoteDTO;
import com.stockwatch.project_data_provider.service.FinnhubService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/provider")
@RequiredArgsConstructor
public class DataProviderController {

    private final FinnhubService finnhubService;

    @GetMapping("/quote/{symbol}")
    public StockQuoteDTO getStockQuote(@PathVariable String symbol) {
        return finnhubService.getStockQuote(symbol);
    }

    @GetMapping("/company/{symbol}")
    public CompanyProfileDTO getCompanyProfile(@PathVariable String symbol) {
        return finnhubService.getCompanyProfile(symbol);
    }
    


    
}
