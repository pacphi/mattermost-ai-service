package me.pacphi.mattermost.service.ai.chat;

import me.pacphi.mattermost.service.ai.domain.PostLite;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatService(ChatModel model, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(model)
        		.defaultAdvisors(
        				new SimpleLoggerAdvisor())
        		.build();
        this.vectorStore = vectorStore;
    }

    public String respondToQuestion(String question) {
        return respondToQuestion(question, null);
    }

    public String respondToQuestion(String question, List<FilterMetadata> filterMetadata) {
        List<PostLite> posts = constructRequest(question, filterMetadata)
                .call()
                .entity(new ParameterizedTypeReference<List<PostLite>>() {});
        return constructResponse(posts);
    }

    public Flux<String> streamResponseToQuestion(String question) {
        return streamResponseToQuestion(question, null);
    }

    public Flux<String> streamResponseToQuestion(String question, List<FilterMetadata> filterMetadata) {
        List<PostLite> posts = constructRequest(question, filterMetadata)
                .call()
                .entity(new ParameterizedTypeReference<List<PostLite>>() {});
        if (CollectionUtils.isEmpty(posts)) {
            return Flux.empty();
        } else {
            return Flux
                    .fromIterable(posts)
                    .map(post -> String.join(System.lineSeparator(), post.asResponse(), System.lineSeparator(), System.lineSeparator()));
        }
    }

    private ChatClient.ChatClientRequestSpec constructRequest(String question, List<FilterMetadata> filterMetadata) {
        return chatClient
                .prompt()
                .advisors(RetrievalAugmentationAdvisor
                        .builder()
                        .documentRetriever(
                                ChatServiceHelper.constructDocumentRetriever(vectorStore, filterMetadata).build()
                        )
                        .build())
                .user(question);
    }

    private String constructResponse(List<PostLite> posts) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(posts)) {
            for (PostLite post : posts) {
                sb.append(post.asResponse());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
