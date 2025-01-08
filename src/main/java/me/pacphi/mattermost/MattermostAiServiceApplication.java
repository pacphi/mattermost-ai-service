package me.pacphi.mattermost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients
@EnableConfigurationProperties
public class MattermostAiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MattermostAiServiceApplication.class, args);
	}

}
