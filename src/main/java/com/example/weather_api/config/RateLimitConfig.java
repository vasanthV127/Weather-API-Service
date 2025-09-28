package com.example.weather_api.config;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.example.weather_api.exception.RateLimitException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Value("${rate.limit.capacity:10}")
    private long capacity;

    @Value("${rate.limit.refill:10}")
    private long refill;

    @Value("${rate.limit.duration.minutes:1}")
    private long durationMinutes;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor());
    }

    private class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        private final Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(refill, Duration.ofMinutes(durationMinutes))))
                .build();

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (!bucket.tryConsume(1)) {
                throw new RateLimitException("Rate limit exceeded for IP: " + request.getRemoteAddr());
            }
            return true;
        }
    }
}
