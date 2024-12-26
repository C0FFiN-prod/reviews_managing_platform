package com.example.demo.component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Llm llm;
    private LastUpdate lastUpdate;

    @Setter
    @Getter
    public static class Llm {
        // Геттеры и сеттеры
        private String host;
        private String model;

    }

    @Setter
    @Getter
    public static class LastUpdate {
        private String categories;
        private String brands;

    }

}

