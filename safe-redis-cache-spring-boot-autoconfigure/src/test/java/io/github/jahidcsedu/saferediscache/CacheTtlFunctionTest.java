package io.github.jahidcsedu.saferediscache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Step 3 target: the TTL each cache entry gets.
 * A plain unit test: no Spring context, no Redis. We build a CacheSpec by hand and call the function.
 */
@DisplayName("Cache TTL function")
class CacheTtlFunctionTest {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration JITTER = Duration.ofMinutes(1);
    private static final Duration NULL_TTL = Duration.ofSeconds(30);

    @Test
    @DisplayName("Each real value lives between ttl and ttl + jitter")
    void realValuesGetTtlPlusARandomJitter() {
        CacheTtlFunction ttlFunction = new CacheTtlFunction(spec(true, JITTER, true));

        for (int i = 0; i < 10_000; i++) {
            Duration ttl = ttlFunction.getTimeToLive("product:" + i, "some value");
            assertThat(ttl).isBetween(TTL, TTL.plus(JITTER));
        }
    }

    @Test
    @DisplayName("TTLs are actually spread out, not all the same")
    void ttlsAreSpreadAcrossTheJitterWindow() {
        CacheTtlFunction ttlFunction = new CacheTtlFunction(spec(true, JITTER, true));

        Set<Duration> distinct = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            distinct.add(ttlFunction.getTimeToLive("product:" + i, "some value"));
        }

        // 1,000 draws from a 60,000 ms window: if almost all are the same, the jitter isn't random.
        assertThat(distinct).hasSizeGreaterThan(900);
    }

    @Test
    @DisplayName("With avalanche protection off, every value gets exactly the ttl")
    void noJitterWhenAvalancheIsDisabled() {
        CacheTtlFunction ttlFunction = new CacheTtlFunction(spec(false, JITTER, true));

        assertThat(ttlFunction.getTimeToLive("product:1", "some value")).isEqualTo(TTL);
    }

    @Test
    @DisplayName("A zero jitter means exactly the ttl")
    void zeroJitterMeansExactlyTheTtl() {
        CacheTtlFunction ttlFunction = new CacheTtlFunction(spec(true, Duration.ZERO, true));

        assertThat(ttlFunction.getTimeToLive("product:1", "some value")).isEqualTo(TTL);
    }

    @Test
    @DisplayName("A cached \"not found\" (null) lives for exactly the null-ttl")
    void nullValuesGetTheShortNullTtl() {
        CacheTtlFunction ttlFunction = new CacheTtlFunction(spec(true, JITTER, true));

        assertThat(ttlFunction.getTimeToLive("product:404", null)).isEqualTo(NULL_TTL);
    }

    private static CacheSpec spec(boolean avalancheEnabled, Duration jitter, boolean penetrationEnabled) {
        return new CacheSpec(
                TTL,
                new CacheSpec.Avalanche(avalancheEnabled, jitter),
                new CacheSpec.Stampede(true, Duration.ofSeconds(5)),
                new CacheSpec.Penetration(penetrationEnabled, NULL_TTL));
    }
}
