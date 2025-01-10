package me.pacphi.mattermost.service;

public interface AuthenticationStrategy {
    String authenticate();
    void clearToken();
}

