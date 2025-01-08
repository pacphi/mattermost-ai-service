package me.pacphi.mattermost;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:creds.properties")
class MattermostAiServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
