package me.pacphi.mattermost.service;

import me.pacphi.mattermost.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for exposing Mattermost API endpoints.
 */
@RestController
@RequestMapping("/api/mattermost")
public class MattermostController {
    private static final Logger logger = LoggerFactory.getLogger(MattermostController.class);

    private final MattermostService mattermostService;

    public MattermostController(MattermostService mattermostService) {
        this.mattermostService = mattermostService;
    }

    @GetMapping("/channels/{channelId}/posts")
    public ResponseEntity<List<Post>> getChannelPosts(
            @PathVariable String channelId) {
        logger.info("Received request for posts in channel: {}", channelId);
        try {
            List<Post> posts = mattermostService.getChannelPosts(channelId);
            return ResponseEntity.ok(posts);
        } catch (MattermostAuthenticationException e) {
            logger.error("Authentication error while fetching posts", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (MattermostApiException e) {
            logger.error("API error while fetching posts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/channels")
    public ResponseEntity<List<ChannelWithTeamData>> getAllChannels() {
        logger.info("Received request for all channels");
        try {
            List<ChannelWithTeamData> channels = mattermostService.getAllChannels();
            return ResponseEntity.ok(channels);
        } catch (MattermostAuthenticationException e) {
            logger.error("Authentication error while fetching channels", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (MattermostApiException e) {
            logger.error("API error while fetching channels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Team>> getAllTeams() {
        logger.info("Received request for all teams");
        try {
            List<Team> teams = mattermostService.getTeams();
            return ResponseEntity.ok(teams);
        } catch (MattermostAuthenticationException e) {
            logger.error("Authentication error while fetching teams", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (MattermostApiException e) {
            logger.error("API error while fetching teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
