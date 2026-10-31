package com.realscore.scraping.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("realscore.sreality")
public record SrealityProperties(
        @DefaultValue("https://www.sreality.cz") String baseUrl,
        @DefaultValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
        String userAgent) {
}
