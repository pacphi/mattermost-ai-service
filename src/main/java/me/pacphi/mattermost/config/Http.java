package me.pacphi.mattermost.config;

import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.web.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class Http {

    @Bean
    public RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMinutes(10).toMillis());
        factory.setReadTimeout((int) Duration.ofMinutes(10).toMillis());
        RestClient.Builder builder = RestClient.builder();
        builder.requestFactory(factory);
        return configurer.configure(builder);
    }
}
