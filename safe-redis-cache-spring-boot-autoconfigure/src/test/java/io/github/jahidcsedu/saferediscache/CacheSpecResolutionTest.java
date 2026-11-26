package io.github.jahidcsedu.saferediscache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Step 2 target: per-cache settings fall back to the user's "defaults" block, field by field,
 * and bad values stop the app at startup instead of failing later at runtime.
 */
@DisplayName("Per-cache settings resolution")
class CacheSpecResolutionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SafeRedisCacheAutoConfiguration.class));

    @Test
    @DisplayName("A cache with no settings of its own uses the defaults block")
    void aCacheWithNoSettingsOfItsOwnUsesTheDefaultsBlock() {
        runner.withPropertyValues("safe-redis-cache.defaults.ttl=1h")
                .run(context -> {
                    SafeRedisCacheProperties props = context.getBean(SafeRedisCacheProperties.class);

                    assertThat(props.specFor("orders")).isEqualTo(props.defaults());
                    assertThat(props.specFor("orders").ttl()).isEqualTo(Duration.ofHours(1));
                });
    }

    @Test
    @DisplayName("Unset per-cache values come from the defaults block, not from built-ins")
    void unsetPerCacheValuesComeFromTheDefaultsBlockNotFromBuiltIns() {
        runner.withPropertyValues(
                        "safe-redis-cache.defaults.avalanche.jitter=5m",
                        "safe-redis-cache.defaults.penetration.null-ttl=10s",
                        "safe-redis-cache.caches.products.ttl=30m")
                .run(context -> {
                    CacheSpec products = context.getBean(SafeRedisCacheProperties.class).specFor("products");

                    assertThat(products.ttl()).isEqualTo(Duration.ofMinutes(30));               // set on the cache
                    assertThat(products.avalanche().jitter()).isEqualTo(Duration.ofMinutes(5));  // from the user's defaults
                    assertThat(products.penetration().nullTtl()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(products.stampede().lockWait()).isEqualTo(Duration.ofSeconds(5)); // nobody set it: built-in
                });
    }

    @Test
    @DisplayName("Nested blocks merge field by field")
    void nestedBlocksMergeFieldByField() {
        runner.withPropertyValues(
                        "safe-redis-cache.defaults.avalanche.jitter=5m",
                        "safe-redis-cache.caches.products.avalanche.enabled=false")
                .run(context -> {
                    CacheSpec products = context.getBean(SafeRedisCacheProperties.class).specFor("products");

                    assertThat(products.avalanche().enabled()).isFalse();                         // set on the cache
                    assertThat(products.avalanche().jitter()).isEqualTo(Duration.ofMinutes(5));   // still from defaults
                });
    }

    @Test
    @DisplayName("A cache can switch on what the defaults switched off")
    void aCacheCanSwitchOnWhatTheDefaultsSwitchedOff() {
        runner.withPropertyValues(
                        "safe-redis-cache.defaults.stampede.enabled=false",
                        "safe-redis-cache.caches.products.stampede.enabled=true")
                .run(context -> {
                    SafeRedisCacheProperties props = context.getBean(SafeRedisCacheProperties.class);

                    assertThat(props.specFor("products").stampede().enabled()).isTrue();
                    assertThat(props.specFor("orders").stampede().enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("An invalid default stops the app at startup")
    void anInvalidDefaultStopsTheAppAtStartup() {
        runner.withPropertyValues("safe-redis-cache.defaults.ttl=0s")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("ttl");
                });
    }

    @Test
    @DisplayName("An invalid per-cache value stops the app at startup too")
    void anInvalidPerCacheValueStopsTheAppAtStartupToo() {
        runner.withPropertyValues("safe-redis-cache.caches.products.avalanche.jitter=-1s")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("jitter");
                });
    }
}
