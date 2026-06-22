package com.stockwatch.project_heatmap_ui.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 前端分離成獨立的靜態網站（部署在 Vercel）後，跟後端 (Railway) 變成不同網域，
// 瀏覽器預設會擋住跨網域的 fetch 請求，所以這裡明確允許 /api/** 給指定來源呼叫。
// allowedOriginPatterns 用「樣式比對」是因為 Vercel 每次部署/分支/預覽都會配新的子網域。
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "https://*.vercel.app"
                )
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}
