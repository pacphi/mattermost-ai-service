package me.pacphi.mattermost.service;

import feign.FeignException;
import me.pacphi.mattermost.api.ChannelsApiClient;
import me.pacphi.mattermost.api.PostsApiClient;
import me.pacphi.mattermost.api.TeamsApiClient;
import me.pacphi.mattermost.model.ChannelWithTeamData;
import me.pacphi.mattermost.model.Post;
import me.pacphi.mattermost.model.PostList;
import me.pacphi.mattermost.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service class for interacting with Mattermost API using FeignClients
 */
@Service
public class MattermostService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int RATE_LIMIT_DELAY = 250;
    private static final Logger logger = LoggerFactory.getLogger(MattermostService.class);

    private final PostsApiClient postsApiClient;
    private final TeamsApiClient teamsApiClient;
    private final ChannelsApiClient channelsApiClient;

    public MattermostService(
            PostsApiClient postsApiClient,
            TeamsApiClient teamsApiClient,
            ChannelsApiClient channelsApiClient) {
        this.postsApiClient = postsApiClient;
        this.teamsApiClient = teamsApiClient;
        this.channelsApiClient = channelsApiClient;
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
                        postsApiClient.getPostsForChannel(
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
            } catch (FeignException e) {
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
                        channelsApiClient.getAllChannels(
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
            } catch (FeignException e) {
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
                        teamsApiClient.getAllTeams(
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
            } catch (FeignException e) {
                logger.error("Error fetching teams", e);
                throw new MattermostApiException("Failed to fetch teams", e);
            }
        } while (!response.isEmpty());

        return allTeams;
    }

}
