package com.chatboxai.api_gateway.configs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private List<Rule> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class Rule {
        private String name;
        private String pathPrefix;
        private int limit;
        private int windowSeconds;
    }
}
