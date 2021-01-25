package com.solace.swim.cache.stdds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.cache.CacheManager;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MINUTES;

@Service
@CacheDefaults(cacheName="cache.stdds.smes")
public class SMESManager {
    private static final Logger logger = LoggerFactory.getLogger(SMESManager.class);

    //create cache
    @Component
    public static class CachingSetup implements JCacheManagerCustomizer
    {
        @Override
        public void customize(CacheManager cacheManager)
        {
            cacheManager.createCache("cache.stdds.smes", new MutableConfiguration<>()
                    .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(MINUTES, 10)))
                    .setStoreByValue(true)
                    .setStatisticsEnabled(true));
        }
    }

    //@Cacheable(key = "#id", unless = "#result == null")
    @CacheResult ()
    public ConcurrentHashMap<String, String> getObjectByTrackId(String id) {
        return null;
    }

    @CachePut(cacheNames = "cache.stdds.smes", key = "#id")
    public ConcurrentHashMap<String, String> insert(String id, ConcurrentHashMap<String, String> object) {
        return object;
    }
}
