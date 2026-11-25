package io.github.jahidcsedu.saferediscache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@ConditionalOnProperty(prefix = "safe-redis-cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SafeRedisCacheProperties.class)
public class SafeRedisCacheAutoConfiguration {
}