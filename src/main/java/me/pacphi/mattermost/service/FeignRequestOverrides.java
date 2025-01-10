package me.pacphi.mattermost.service;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FeignRequestOverrides {

    private final AuthenticationStrategy authenticationService;

    FeignRequestOverrides(AuthenticationStrategy authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Bean
    RequestInterceptor authenticationInterceptor() {
        return requestTemplate -> {
            String token = authenticationService.authenticate();
            requestTemplate.header("Authorization", "Bearer " + token);
        };
    }
}
