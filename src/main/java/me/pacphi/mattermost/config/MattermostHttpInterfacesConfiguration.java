package me.pacphi.mattermost.config;

import me.pacphi.mattermost.configuration.HttpInterfacesAbstractConfigurator;
import me.pacphi.mattermost.service.AuthenticationStrategy;
import me.pacphi.mattermost.service.MattermostProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Mattermost HTTP Interface clients.
 * Extends the generated abstract configurator and provides a configured WebClient
 * with authentication support.
 */
@Configuration
public class MattermostHttpInterfacesConfiguration extends HttpInterfacesAbstractConfigurator {

    public MattermostHttpInterfacesConfiguration(
            MattermostProperties props,
            AuthenticationStrategy authenticationStrategy) {
        super(createWebClient(props, authenticationStrategy));
    }

    private static WebClient createWebClient(
            MattermostProperties props,
            AuthenticationStrategy authenticationStrategy) {
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .filter(authenticationFilter(authenticationStrategy))
                .build();
    }

    private static ExchangeFilterFunction authenticationFilter(AuthenticationStrategy authenticationStrategy) {
        return (request, next) -> {
            String token = authenticationStrategy.authenticate();
            ClientRequest authenticatedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + token)
                    .build();
            return next.exchange(authenticatedRequest);
        };
    }
}
