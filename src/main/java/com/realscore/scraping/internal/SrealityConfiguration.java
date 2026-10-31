package com.realscore.scraping.internal;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SrealityProperties.class)
class SrealityConfiguration {

    @Bean
    SrealityRequestPolicy srealityRequestPolicy(SrealityProperties properties) {
        return new SrealityRequestPolicy(properties, Thread::sleep, Clock.systemUTC());
    }
}
