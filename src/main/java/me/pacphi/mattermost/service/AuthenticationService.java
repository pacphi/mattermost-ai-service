package me.pacphi.mattermost.service;

import me.pacphi.mattermost.model.LoginRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class AuthenticationService {

    private final RestClient authClient;
    private final MattermostProperties props;
    private String authToken;

    AuthenticationService(MattermostProperties props) {
        this.authClient =
                RestClient.builder()
                    .baseUrl(props.baseUrl())
                    .build();
        this.props = props;
    }

    String authenticate() {
        if (authToken == null) {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setLoginId(props.credentials().username());
            loginRequest.setPassword(props.credentials().password());
            authToken =
                    authClient
                        .post()
                        .uri("/api/v4/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(loginRequest)
                        .retrieve()
                        .toBodilessEntity()
                        .getHeaders()
                        .getFirst("Token");
        }
        return authToken;
    }

    void clearToken() {
        authToken = null;
    }
}
