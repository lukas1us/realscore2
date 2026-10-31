package com.realscore.scraping.internal;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({SrealityProperties.class, ScrapeJobProperties.class})
class SrealityConfiguration {

    @Bean
    SrealityRequestPolicy srealityRequestPolicy(SrealityProperties properties) {
        return new SrealityRequestPolicy(properties, Thread::sleep, Clock.systemUTC());
    }
}
