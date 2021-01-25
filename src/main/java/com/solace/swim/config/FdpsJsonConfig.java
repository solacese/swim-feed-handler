package com.solace.swim.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableCaching
@ImportResource({"classpath*:fdpsToJsonContext.xml"})
public class FdpsJsonConfig {
}
