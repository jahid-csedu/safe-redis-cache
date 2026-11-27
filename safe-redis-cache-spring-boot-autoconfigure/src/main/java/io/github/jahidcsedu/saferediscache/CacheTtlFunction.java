package io.github.jahidcsedu.saferediscache;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

record CacheTtlFunction(CacheSpec spec) implements RedisCacheWriter.TtlFunction {

    @Override
    public Duration getTimeToLive(Object key, @Nullable Object value) {
        if (value == null) {
            return spec.penetration().nullTtl();
        }
        if (!spec.avalanche().enabled()) {
            return spec.ttl();
        }
        long extraMillis = ThreadLocalRandom.current().nextLong(spec.avalanche().jitter().toMillis() + 1);
        return spec.ttl().plusMillis(extraMillis);
    }
}
