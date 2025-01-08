package me.pacphi.mattermost.service;

import feign.RequestInterceptor;
import org.openapitools.configuration.ClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
class FeignRequestOverrides {

    private final AuthenticationService authenticationService;

    FeignRequestOverrides(AuthenticationService authenticationService) {
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
