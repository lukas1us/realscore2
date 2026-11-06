package com.realscore.scraping.internal;

import com.realscore.scraping.ScrapeRunService;
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

    @Bean
    ScrapeScheduler scrapeScheduler(ScrapeJobProperties properties, SrealityScrapeJob job,
            ScrapeRunService scrapeRunService) {
        Clock clock = Clock.systemUTC();
        return new ScrapeScheduler(new ScrapeSchedule(properties.cron()), job, scrapeRunService, clock,
                clock.instant());
    }
}
