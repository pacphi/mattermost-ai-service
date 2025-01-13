package me.pacphi.mattermost.service;

import me.pacphi.mattermost.model.User;
import org.springframework.web.client.RestClient;

abstract class BaseAuthenticationService implements AuthenticationStrategy {

    protected final RestClient authClient;
    protected final MattermostProperties props;

    BaseAuthenticationService(MattermostProperties props) {
        this.authClient =
                RestClient.builder()
                        .baseUrl(props.baseUrl())
                        .build();
        this.props = props;
    }

    public User getCurrentUser() {
        String token = authenticate();
        return
                authClient
                        .get()
                        .uri("/api/v4/users/me")
                        .header("Authorization", String.format("Bearer %s", token))
                        .retrieve()
                        .toEntity(User.class)
                        .getBody();
    }
}
