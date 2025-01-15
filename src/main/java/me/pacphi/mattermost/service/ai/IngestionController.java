package me.pacphi.mattermost.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
    public ResponseEntity<Void> ingestPostsFromChannel(@RequestParam("channelId") String channelId, @RequestParam("since") Long since) {
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(since),
                ZoneId.systemDefault()
        );
        logger.info("Received request for posts in channel {} since {}", channelId, timestamp);
        List<Post> posts = new ArrayList<>();
        try {
            posts.addAll(mattermostService.getChannelPosts(channelId, since));
        } catch (MattermostAuthenticationException e) {
            logger.error("Authentication error while fetching posts", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (MattermostApiException e) {
            logger.error("API error while fetching posts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        int successes = 0;
        int failures = 0;
        for (Post p : posts) {
            logger.info(
                    "-- Ingesting post [ id: {}, created: {}, message (truncated): {} ]",
                    p.getId(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getCreateAt()), ZoneId.systemDefault()),
                    p.getMessage().length() > 10 ? p.getMessage().substring(0,10): p.getMessage()
            );
            try {
                ingestionService.ingest(p);
                logger.info("---- Ingested");
                successes++;
            } catch (JsonProcessingException | UnsupportedEncodingException | RuntimeException e) {
                logger.warn("---- Failed", e);
            }
        };
        logger.info("Successfully ingested: {} post(s). Failed to ingest: {} post(s).", successes, failures);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
