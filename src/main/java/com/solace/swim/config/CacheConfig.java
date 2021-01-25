package com.solace.swim.config;


import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableCaching
@ImportResource({"classpath*:stddsToJsonContext.xml"})
public class CacheConfig  {

}
