package me.pacphi.mattermost.service;

import me.pacphi.mattermost.model.LoginRequest;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class UsernameAndPasswordAuthenticationService extends BaseAuthenticationService {

    private String authToken;

    public UsernameAndPasswordAuthenticationService(MattermostProperties props) {
        super(props);
    }

    @Override
    public String authenticate() {
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

    @Override
    public void clearToken() {
        authToken = null;
    }
}
