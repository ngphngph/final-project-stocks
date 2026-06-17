package com.stockwatch.project_stock_data.repository;

import com.stockwatch.project_stock_data.entity.StockProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface StockProfileRepository extends JpaRepository<StockProfile, Long> {
    @Query("SELECT sp FROM StockProfile sp JOIN FETCH sp.stock")
    List<StockProfile> findAllWithStock();
}