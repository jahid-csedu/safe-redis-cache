# safe-redis-cache

A Spring Boot starter that puts guard rails around a Redis cache. You add one dependency, keep writing `@Cacheable` the way you already do, and the starter protects each cache from three classic failures: cache avalanche, cache stampede and cache penetration. Which protections a cache gets is decided in `application.yml`, per cache name.

It's built for apps that run more than one instance. That's usually why people put a cache in Redis in the first place, so every protection here is designed to hold across pods, not only inside one JVM.

> Status: early development. The configuration model is in place; the protections themselves are being built step by step (see the roadmap below). Nothing is published to Maven Central yet.

## The three problems

Each protection comes from a story I wrote with a runnable experiment behind it. If you want the "why" and the numbers, start there.

- [When every cache key expires at the same second](https://medium.com/@jahid.csedu/when-every-cache-key-expires-at-the-same-second-b6bfe144cc1e) (cache avalanche)
- [One hot key, 200 identical queries](https://medium.com/@jahid.csedu/one-hot-key-200-identical-queries-ced8bacca898) (cache stampede)
- [20,000 requests for products that don't exist](https://medium.com/@jahid.csedu/20-000-requests-for-products-that-dont-exist-87234b947f0a) (cache penetration)

## How it protects you

Avalanche happens when many keys were written together with the same TTL and then all expire in the same moment. The starter adds a random amount of extra time (jitter) to every TTL, so expiries spread out. Each instance picks its own random value, which is exactly what you want across pods.

Stampede is many requests missing the same hot key at once and all hitting the database. The starter stops it in two layers. A local single-flight lock first collapses all the threads of one instance into a single caller. That one caller then takes a Redisson distributed lock for the key, so only one instance in the whole cluster loads the value; after getting the lock it checks Redis again, because another instance may have filled it meanwhile. The local layer means a burst of 200 threads costs Redis one lock attempt per pod instead of 200.

Penetration is traffic for keys that don't exist anywhere, so the cache never fills and every request reaches the database. The starter caches "not found" with a short TTL of its own. An optional Bloom filter (Redisson `RBloomFilter`) can reject unknown ids before they touch the cache or the database. Your app supplies the ids, since only it knows them.

## Requirements

Java 17 or newer, Spring Boot 4.1 or newer, and a Redis server. The starter talks to Redis through [Redisson](https://redisson.org/), which also backs Spring Data Redis, so your `RedisTemplate` and the cache share one connection pool.

## Usage

Add the starter (once it's published; for now, build it locally with `mvn install`):

```xml
<dependency>
  <groupId>io.github.jahidcsedu</groupId>
  <artifactId>safe-redis-cache-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Then use Spring's cache abstraction as usual. One rule matters: set `sync = true`.

```java
@Cacheable(cacheNames = "products", key = "#id", sync = true)
public Product getProduct(long id) {
    return repository.findById(id);
}
```

Without `sync = true`, Spring does a plain read followed by a separate write and never hands the cache your loading method. The stampede and penetration protections need that loader, so they can't help on that path. The starter will log a warning when it sees `@Cacheable` without it.

If you'd rather not use annotations, call the cache directly. This is the same code path `sync = true` uses, so you get the same protection:

```java
Cache products = cacheManager.getCache("products");
Product product = products.get(id, () -> repository.findById(id));
```

Plain `RedisTemplate` calls stay untouched. A template never sees the database call behind a value, so there's nothing for the starter to lock around or to cache as "not found".

## Configuration

Everything lives under `safe-redis-cache`. The values below are the built-in defaults.

```yaml
safe-redis-cache:
  enabled: true            # turn the whole starter off with false
  defaults:                # applies to every cache
    ttl: 10m
    avalanche:
      enabled: true
      jitter: 1m           # each entry lives ttl + random(0..jitter)
    stampede:
      enabled: true
      lock-wait: 5s        # how long to wait for another instance's load
    penetration:
      enabled: true
      null-ttl: 30s        # how long a "not found" stays cached
  caches:                  # per-cache settings, keyed by cache name
    products:
      ttl: 30m
      stampede:
        enabled: false
```

A per-cache block only needs the values that differ. Anything it leaves out comes from your `defaults` block, field by field, and from the built-in values when `defaults` doesn't set it either. Invalid values, such as a zero TTL or a negative jitter, stop the app at startup with a message naming the property.

## Project layout

| Module | What it holds |
|---|---|
| `safe-redis-cache-spring-boot-autoconfigure` | All the code: properties, auto-configuration, the cache decorators |
| `safe-redis-cache-spring-boot-starter` | No code; just the dependency list an app adds |
| `safe-redis-cache-sample` | A small Spring Boot app that uses the starter |

This follows Spring Boot's own convention for third-party starters. I'll admit it feels like a lot of modules for a small library, but it keeps the starter you depend on free of anything you didn't ask for.

## Building locally

You need Docker for Redis and JDK 17 or newer.

```bash
docker compose up -d
mvn verify
java -jar safe-redis-cache-sample/target/safe-redis-cache-sample-0.1.0-SNAPSHOT.jar
```

## Roadmap

- [x] Multi-module starter skeleton, auto-configuration and typed properties
- [x] Per-cache settings that fall back to `defaults`, validated at startup
- [ ] A `CacheManager` built from those settings
- [ ] Avalanche: TTL jitter
- [ ] Stampede, layer 1: local single-flight
- [ ] Stampede, layer 2: Redisson lock with a second check, tested across two app instances
- [ ] Penetration: "not found" caching and an optional Bloom filter
- [ ] Warning for `@Cacheable` without `sync = true`
- [ ] Publish to Maven Central

## Why I'm building this

I wrote the three stories above with plain Spring Data Redis code. Copying those fixes into a real project still takes work, so I wanted readers to have something they can add as one dependency.

## License

Licensed under the [Apache License, Version 2.0](LICENSE). It's also my first Spring Boot starter, built in the open while I learn how starters work, so the commit history doubles as a record of that.
