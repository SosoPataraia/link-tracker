package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import backend.academy.linktracker.scrapper.ratelimit.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfiguration {

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitProperties rateLimitProperties) {
        return new RateLimitFilter(rateLimitProperties);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        var registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.addUrlPatterns("/links/*", "/links", "/tg-chat/*");
        registration.setOrder(1);
        return registration;
    }
}
