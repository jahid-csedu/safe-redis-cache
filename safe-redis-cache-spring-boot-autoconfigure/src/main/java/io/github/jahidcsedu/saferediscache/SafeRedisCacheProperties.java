package io.github.jahidcsedu.saferediscache;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "safe-redis-cache")
public record SafeRedisCacheProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue CacheSpec defaults,
        Map<String, CacheSpec> caches
) {
}