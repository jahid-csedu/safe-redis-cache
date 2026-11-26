package io.github.jahidcsedu.saferediscache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Lesson 1 target. This test does not compile until you write
 * SafeRedisCacheAutoConfiguration and SafeRedisCacheProperties.
 *
 * ApplicationContextRunner starts a tiny Spring context with only the auto-configuration
 * you give it. No Redis, no web server, it runs in milliseconds. It's the standard way
 * to test a starter.
 */
@DisplayName("Safe Redis Cache properties")
class SafeRedisCachePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SafeRedisCacheAutoConfiguration.class));

    @Test
    @DisplayName("Built-in defaults apply when nothing is configured")
    void builtInDefaultsApplyWhenNothingIsConfigured() {
        runner.run(context -> {
            SafeRedisCacheProperties props = context.getBean(SafeRedisCacheProperties.class);

            assertThat(props.enabled()).isTrue();
            assertThat(props.defaults().ttl()).isEqualTo(Duration.ofMinutes(10));

            assertThat(props.defaults().avalanche().enabled()).isTrue();
            assertThat(props.defaults().avalanche().jitter()).isEqualTo(Duration.ofMinutes(1));

            assertThat(props.defaults().stampede().enabled()).isTrue();
            assertThat(props.defaults().stampede().lockWait()).isEqualTo(Duration.ofSeconds(5));

            assertThat(props.defaults().penetration().enabled()).isTrue();
            assertThat(props.defaults().penetration().nullTtl()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    @DisplayName("Defaults can be changed from properties")
    void defaultsCanBeChangedFromProperties() {
        runner.withPropertyValues(
                        "safe-redis-cache.defaults.ttl=1h",
                        "safe-redis-cache.defaults.avalanche.jitter=5m",
                        "safe-redis-cache.defaults.penetration.enabled=false")
                .run(context -> {
                    SafeRedisCacheProperties props = context.getBean(SafeRedisCacheProperties.class);

                    assertThat(props.defaults().ttl()).isEqualTo(Duration.ofHours(1));
                    assertThat(props.defaults().avalanche().jitter()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(props.defaults().penetration().enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("Each cache name can have its own settings")
    void eachCacheNameCanHaveItsOwnSettings() {
        runner.withPropertyValues(
                        "safe-redis-cache.caches.products.ttl=30m",
                        "safe-redis-cache.caches.products.stampede.enabled=false")
                .run(context -> {
                    SafeRedisCacheProperties props = context.getBean(SafeRedisCacheProperties.class);

                    assertThat(props.caches()).containsKey("products");
                    assertThat(props.caches().get("products").ttl()).isEqualTo(Duration.ofMinutes(30));
                    assertThat(props.caches().get("products").stampede().enabled()).isFalse();
                });
    }

    @Test
    @DisplayName("The whole starter switches off with one property")
    void theWholeStarterSwitchesOffWithOneProperty() {
        runner.withPropertyValues("safe-redis-cache.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SafeRedisCacheProperties.class));
    }
}
