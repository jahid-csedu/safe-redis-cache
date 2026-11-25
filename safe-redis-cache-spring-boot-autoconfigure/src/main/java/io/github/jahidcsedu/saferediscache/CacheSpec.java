package io.github.jahidcsedu.saferediscache;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

public record CacheSpec(
        @DefaultValue("10m") Duration ttl,
        @DefaultValue Avalanche avalanche,
        @DefaultValue Stampede stampede,
        @DefaultValue Penetration penetration
) {

    public record Avalanche(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("1m") Duration jitter
    ) {
    }

    public record Stampede(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("5s") Duration lockWait
    ) {
    }

    public record Penetration(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30s") Duration nullTtl
    ) {
    }
}
