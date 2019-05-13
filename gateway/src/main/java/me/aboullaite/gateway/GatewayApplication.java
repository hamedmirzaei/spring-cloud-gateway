package me.aboullaite.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableEurekaClient
@EnableHystrix
@Configuration
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    // Not
    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> Mono.just("fero");
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("book-store", r -> r.path("/api/books/**")
                        .filters(f ->
                                f.rewritePath("/api/(?<books>.*)", "/${books}")
                                        .hystrix(c -> c.setName("booksFallbackCommand").setFallbackUri("forward:/fallback/books"))
                                        .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver()))
                        ).uri("lb://book-store"))
                .route("movie-store", r -> r.path("/api/movies/**")
                        .filters(f ->
                                        f.rewritePath("/api/(?<movies>.*)", "/${movies}")
                                                .addResponseHeader("X-Some-Header","aboullaite.me")
                                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver()))
                                //).uri("lb://movie-store"))
                        ).uri("http://localhost:8890"))

                .build();
    }

    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(2, 2);
    }
}

