package me.pacphi.mattermost.service;

import me.pacphi.mattermost.model.User;

public interface AuthenticationStrategy {
    String authenticate();
    User getCurrentUser();
    void clearToken();
}

