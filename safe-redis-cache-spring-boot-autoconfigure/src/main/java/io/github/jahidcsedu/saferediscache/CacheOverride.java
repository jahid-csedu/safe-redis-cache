package io.github.jahidcsedu.saferediscache;

import java.time.Duration;

public record CacheOverride(
        Duration ttl,
        AvalancheOverride avalanche,
        StampedeOverride stampede,
        PenetrationOverride penetration
) {

    public CacheSpec applyTo(CacheSpec defaults) {
        return new CacheSpec(
                ttl != null ? ttl : defaults.ttl(),
                avalanche != null ? avalanche.applyTo(defaults.avalanche()) : defaults.avalanche(),
                stampede != null ? stampede.applyTo(defaults.stampede()) : defaults.stampede(),
                penetration != null ? penetration.applyTo(defaults.penetration()) : defaults.penetration()
        );
    }

    public record AvalancheOverride(Boolean enabled, Duration jitter) {
        CacheSpec.Avalanche applyTo(CacheSpec.Avalanche base) {
            return new CacheSpec.Avalanche(
                    enabled != null ? enabled : base.enabled(),
                    jitter != null ? jitter : base.jitter()
            );
        }
    }

    public record StampedeOverride(Boolean enabled, Duration lockWait) {
        CacheSpec.Stampede applyTo(CacheSpec.Stampede base) {
            return new CacheSpec.Stampede(
                    enabled != null ? enabled : base.enabled(),
                    lockWait != null ? lockWait : base.lockWait()
            );
        }
    }

    public record PenetrationOverride(Boolean enabled, Duration nullTtl) {
        CacheSpec.Penetration applyTo(CacheSpec.Penetration base) {
            return new CacheSpec.Penetration(
                    enabled != null ? enabled : base.enabled(),
                    nullTtl != null ? nullTtl : base.nullTtl()
            );
        }
    }
}
