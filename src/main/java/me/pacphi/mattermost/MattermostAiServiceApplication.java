package me.pacphi.mattermost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties
public class MattermostAiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MattermostAiServiceApplication.class, args);
	}

}
