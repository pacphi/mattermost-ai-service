package me.pacphi.mattermost.service;

public class PersonalAccessTokenAuthenticationService implements AuthenticationStrategy {

    private final MattermostProperties props;

    public PersonalAccessTokenAuthenticationService(MattermostProperties props) {
        this.props = props;
    }

    public String authenticate() {
        return props.credentials().token();
    }

    public void clearToken() {
        throw new UnsupportedOperationException("Personal access tokens must be deactivated via Mattermost's system console.");
    }
}
