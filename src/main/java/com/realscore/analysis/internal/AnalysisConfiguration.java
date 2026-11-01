package com.realscore.analysis.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AnalysisProperties.class, ScoringProperties.class})
class AnalysisConfiguration {
}
