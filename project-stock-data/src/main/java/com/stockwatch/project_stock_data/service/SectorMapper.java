package com.stockwatch.project_stock_data.service;

import java.util.Map;

/**
 * 把 Finnhub 的 industry（細分產業，例如 "Semiconductors"）對應到較大的
 * sector 分類（例如 "Information Technology"），讓前端能像 Finviz 一樣
 * 做 Sector > Industry 兩層 treemap。
 *
 * 這份對照表是依照 stock_profiles 資料表裡實際出現的 40 種 industry 值
 * （SELECT DISTINCT industry FROM stock_profiles）手動對應到 11 個
 * GICS 風格的大分類，沒有出現在表內的 industry 會 fallback 到 "Other"。
 */
public final class SectorMapper {

    private static final Map<String, String> INDUSTRY_TO_SECTOR = Map.ofEntries(
            Map.entry("Aerospace & Defense", "Industrials"),
            Map.entry("Airlines", "Industrials"),
            Map.entry("Auto Components", "Consumer Discretionary"),
            Map.entry("Automobiles", "Consumer Discretionary"),
            Map.entry("Banking", "Financials"),
            Map.entry("Beverages", "Consumer Staples"),
            Map.entry("Biotechnology", "Health Care"),
            Map.entry("Building", "Industrials"),
            Map.entry("Chemicals", "Materials"),
            Map.entry("Commercial Services & Supplies", "Industrials"),
            Map.entry("Communications", "Communication Services"),
            Map.entry("Construction", "Industrials"),
            Map.entry("Consumer products", "Consumer Discretionary"),
            Map.entry("Distributors", "Consumer Discretionary"),
            Map.entry("Electrical Equipment", "Industrials"),
            Map.entry("Energy", "Energy"),
            Map.entry("Financial Services", "Financials"),
            Map.entry("Food Products", "Consumer Staples"),
            Map.entry("Health Care", "Health Care"),
            Map.entry("Hotels, Restaurants & Leisure", "Consumer Discretionary"),
            Map.entry("Industrial Conglomerates", "Industrials"),
            Map.entry("Insurance", "Financials"),
            Map.entry("Leisure Products", "Consumer Discretionary"),
            Map.entry("Life Sciences Tools & Services", "Health Care"),
            Map.entry("Logistics & Transportation", "Industrials"),
            Map.entry("Machinery", "Industrials"),
            Map.entry("Media", "Communication Services"),
            Map.entry("Metals & Mining", "Materials"),
            Map.entry("Packaging", "Materials"),
            Map.entry("Pharmaceuticals", "Health Care"),
            Map.entry("Professional Services", "Industrials"),
            Map.entry("Real Estate", "Real Estate"),
            Map.entry("Retail", "Consumer Discretionary"),
            Map.entry("Road & Rail", "Industrials"),
            Map.entry("Semiconductors", "Information Technology"),
            Map.entry("Technology", "Information Technology"),
            Map.entry("Telecommunication", "Communication Services"),
            Map.entry("Textiles, Apparel & Luxury Goods", "Consumer Discretionary"),
            Map.entry("Tobacco", "Consumer Staples"),
            Map.entry("Trading Companies & Distributors", "Industrials"),
            Map.entry("Utilities", "Utilities")
    );

    private SectorMapper() {
    }

    public static String mapSector(String industry) {
        if (industry == null) {
            return "Other";
        }
        return INDUSTRY_TO_SECTOR.getOrDefault(industry, "Other");
    }
}
