package me.pacphi.mattermost.config;

import me.pacphi.mattermost.service.*;
import org.apache.commons.lang3.StringUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Auth {

    @Bean
    public AuthenticationStrategy authenticationStrategy(MattermostProperties props) {
        if (StringUtils.isNotBlank(props.credentials().token())) {
            return new PersonalAccessTokenAuthenticationService(props);
        } else {
            if (StringUtils.isNotBlank(props.credentials().username()) && StringUtils.isNotBlank(props.credentials().password())) {
                return new UsernameAndPasswordAuthenticationService(props);
            }
            throw new MattermostAuthenticationException("Invalid authentication credentials provided");
        }
    }
}
