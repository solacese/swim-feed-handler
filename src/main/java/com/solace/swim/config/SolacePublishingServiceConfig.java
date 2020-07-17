package com.solace.swim.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ConditionalOnProperty(prefix = "service.solace-publishing", value = "enabled", havingValue = "true")
@ImportResource({"classpath*:solacePublishingServiceContext.xml"})
public class SolacePublishingServiceConfig {
}
