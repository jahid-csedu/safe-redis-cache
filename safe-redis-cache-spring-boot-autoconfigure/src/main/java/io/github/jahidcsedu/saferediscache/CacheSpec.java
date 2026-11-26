package io.github.jahidcsedu.saferediscache;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

public record CacheSpec(
        @DefaultValue("10m") Duration ttl,
        @DefaultValue Avalanche avalanche,
        @DefaultValue Stampede stampede,
        @DefaultValue Penetration penetration
) {

    public CacheSpec {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive, was "+ ttl);
        }
    }

    public record Avalanche(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("1m") Duration jitter
    ) {
        public Avalanche {
            if (jitter.isNegative()) {
                throw new IllegalArgumentException("avalanche.jitter must not be negative, was " + jitter);
            }
        }
    }

    public record Stampede(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("5s") Duration lockWait
    ) {
        public Stampede {
            if (lockWait.isNegative() || lockWait.isZero()) {
                throw new IllegalArgumentException("stampede.lock-wait must be positive, was " + lockWait);
            }
        }
    }

    public record Penetration(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30s") Duration nullTtl
    ) {
        public Penetration {
            if (nullTtl.isNegative() || nullTtl.isZero()) {
                throw new IllegalArgumentException("penetration.null-ttl must be positive, was " + nullTtl);
            }
        }
    }
}
