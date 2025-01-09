package me.pacphi.mattermost.service.ai;

import me.pacphi.mattermost.service.ai.chat.ChatService;
import me.pacphi.mattermost.service.ai.chat.Inquiry;
import org.apache.commons.collections.CollectionUtils;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<String> chat(@RequestBody Inquiry inquiry) {
        if (CollectionUtils.isNotEmpty(inquiry.filter())) {
            return ResponseEntity.ok(chatService.respondToQuestion(inquiry.question(), inquiry.filter()));
        } else {
            return ResponseEntity.ok(chatService.respondToQuestion(inquiry.question()));
        }
    }

    @PostMapping("/api/stream/chat")
    public ResponseEntity<Flux<String>> streamChat(@RequestBody Inquiry inquiry) {
        if (CollectionUtils.isNotEmpty(inquiry.filter())) {
            return ResponseEntity.ok(chatService.streamResponseToQuestion(inquiry.question(), inquiry.filter()));
        } else {
            return ResponseEntity.ok(chatService.streamResponseToQuestion(inquiry.question()));
        }
    }
}
