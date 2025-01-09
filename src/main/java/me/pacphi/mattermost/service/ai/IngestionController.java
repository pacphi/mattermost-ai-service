package me.pacphi.mattermost.service.ai;

import me.pacphi.mattermost.model.Post;
import me.pacphi.mattermost.service.MattermostApiException;
import me.pacphi.mattermost.service.MattermostAuthenticationException;
import me.pacphi.mattermost.service.MattermostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mattermost")
public class IngestionController {

    private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);

    private final MattermostService mattermostService;
    private final IngestionService ingestionService;

    public IngestionController(MattermostService mattermostService, IngestionService ingestionService) {
        this.mattermostService = mattermostService;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingestPostsFromChannel(@RequestParam("channelId") String channelId) {
        logger.info("Received request for ingesting posts in channel: {}", channelId);
        try {
            List<Post> posts = mattermostService.getChannelPosts(channelId);
            posts.forEach(ingestionService::ingest);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (MattermostAuthenticationException e) {
            logger.error("Authentication error while fetching posts", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (MattermostApiException e) {
            logger.error("API error while fetching posts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
