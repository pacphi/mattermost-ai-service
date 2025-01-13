package me.pacphi.mattermost.service;

public class PersonalAccessTokenAuthenticationService extends BaseAuthenticationService {

    public PersonalAccessTokenAuthenticationService(MattermostProperties props) {
        super(props);
    }

    @Override
    public String authenticate() {
        return props.credentials().token();
    }

    @Override
    public void clearToken() {
        throw new UnsupportedOperationException("Personal access tokens must be deactivated via Mattermost's system console.");
    }
}
