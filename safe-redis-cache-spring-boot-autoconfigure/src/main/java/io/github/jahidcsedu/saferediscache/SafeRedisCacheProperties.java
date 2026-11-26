package io.github.jahidcsedu.saferediscache;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "safe-redis-cache")
public record SafeRedisCacheProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue CacheSpec defaults,
        @DefaultValue Map<String, CacheOverride> caches
) {

    public SafeRedisCacheProperties {
        for(CacheOverride override: caches.values()) {
            override.applyTo(defaults);
        }
    }

    public CacheSpec specFor(String cacheName) {
        CacheOverride present = caches.get(cacheName);
        if (present != null) {
            return present.applyTo(defaults);
        }
        return defaults;
    }
}