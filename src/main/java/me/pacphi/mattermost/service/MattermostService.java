package me.pacphi.mattermost.service;

import me.pacphi.mattermost.api.ChannelsApi;
import me.pacphi.mattermost.api.PostsApi;
import me.pacphi.mattermost.api.TeamsApi;
import me.pacphi.mattermost.model.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service class for interacting with Mattermost API using Spring HTTP Interface clients
 */
@Service
public class MattermostService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int RATE_LIMIT_DELAY = 250;
    private static final Logger logger = LoggerFactory.getLogger(MattermostService.class);

    private final ChannelsApi channelsApi;
    private final PostsApi postsApi;
    private final TeamsApi teamsApi;
    private final AuthenticationStrategy authenticationService;

    public MattermostService(
            ChannelsApi channelsApi,
            PostsApi postsApi,
            TeamsApi teamsApi,
            AuthenticationStrategy authenticationService) {
        this.channelsApi = channelsApi;
        this.postsApi = postsApi;
        this.teamsApi = teamsApi;
        this.authenticationService = authenticationService;
    }

    public List<Channel> getChannelsForTeam(String teamName) {
        List<Channel> result = new ArrayList<>();
        try {
            User currentUser = authenticationService.getCurrentUser();
            Team team = teamsApi.getTeamByName(teamName).getBody();
            if (team != null) {
                List<Channel> channels = channelsApi.getChannelsForTeamForUser(currentUser.getId(), team.getId(), null, null).getBody();
                if (CollectionUtils.isNotEmpty(channels)) {
                    result.addAll(channels);
                }
            }
            return result;
        } catch (WebClientResponseException e) {
            logger.error(String.format("Error fetching team %s's channels", teamName), e);
            throw new MattermostApiException(String.format("Failed to fetch team %s's channels", teamName), e);
        }
    }

    public List<Post> getChannelPosts(String channelId, Long since) {
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(since),
                ZoneId.systemDefault()
        );
        logger.debug("Fetching posts for channel {} since {}", channelId, timestamp);
        List<Post> allPosts = new ArrayList<>();
        int page = 0;
        PostList response;
        do {
            try {
                response =
                        postsApi.getPostsForChannel(
                                channelId,
                                page,
                                DEFAULT_PAGE_SIZE,
                                null,
                                null,
                                null,
                                false
                        ).getBody();

                if (response == null || response.getPosts() == null || response.getPosts().isEmpty()) {
                    logger.info("-- No posts found for channel {} since {}", channelId, timestamp);
                    break;
                }
                Collection<Post> posts = response.getPosts().values();
                allPosts.addAll(filterPosts(posts, since));
                page++;
                TimeUnit.MILLISECONDS.sleep(RATE_LIMIT_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Rate limiting delay interrupted", e);
                break;
            } catch (WebClientResponseException e) {
                logger.error("Error fetching channel posts", e);
                throw new MattermostApiException("Failed to fetch channel posts", e);
            }
        } while (!response.getPosts().isEmpty());

        return allPosts;
    }

    private Collection<Post> filterPosts(Collection<Post> posts, Long since) {
        return posts.stream().filter(p -> p.getCreateAt() >= since).toList();
    }

    public List<ChannelWithTeamData> getAllChannels() {
        logger.debug("Fetching all channels");
        List<ChannelWithTeamData> allChannels = new ArrayList<>();
        int page = 0;
        List<ChannelWithTeamData> response;
        do {
            try {
                response =
                        channelsApi.getAllChannels(
                                null,
                                page,
                                DEFAULT_PAGE_SIZE,
                                false,
                                false,
                                false,
                                false
                        ).getBody();

                if (response == null) {
                    break;
                }
                allChannels.addAll(response);
                page++;
                TimeUnit.MILLISECONDS.sleep(RATE_LIMIT_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Rate limiting delay interrupted", e);
                break;
            } catch (WebClientResponseException e) {
                logger.error("Error fetching channels", e);
                throw new MattermostApiException("Failed to fetch channels", e);
            }
        } while (!response.isEmpty());

        return allChannels;
    }

    public List<Team> getTeams() {
        logger.debug("Fetching all teams");
        List<Team> allTeams = new ArrayList<>();
        int page = 0;
        List<Team> response;
        do {
            try {
                response =
                        teamsApi.getAllTeams(
                            page,
                            DEFAULT_PAGE_SIZE,
                            false,
                            false
                        ).getBody();

                if (response == null) {
                    break;
                }
                allTeams.addAll(response);
                page++;
                TimeUnit.MILLISECONDS.sleep(RATE_LIMIT_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Rate limiting delay interrupted", e);
                break;
            } catch (WebClientResponseException e) {
                logger.error("Error fetching teams", e);
                throw new MattermostApiException("Failed to fetch teams", e);
            }
        } while (!response.isEmpty());

        return allTeams;
    }

}
