package org.misha.authservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация WebMvc для правильной обработки путей.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Отключает автоматическое добавление завершающего слэша к путям.
     * Это позволяет обрабатывать как /excel, так и /excel/ одинаково.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(false);
    }
}

