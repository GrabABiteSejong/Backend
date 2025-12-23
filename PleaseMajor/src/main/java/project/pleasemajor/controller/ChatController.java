package project.pleasemajor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.pleasemajor.service.CareerChatService;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

  private final CareerChatService chatService;

  @PostMapping("/chat")
  public ChatResponse chat(@RequestBody ChatRequest req) {
    var res = chatService.reply(req.sessionId(), req.message());
    return new ChatResponse(res.sessionId(), res.answer());
  }

//  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//  public Flux<String> stream(@RequestBody ChatRequest req) {
//    return chatService.replyStream(req.sessionId(), req.message());
//  }

  public record ChatRequest(String sessionId, String message) {}
  public record ChatResponse(String sessionId, String answer) {}

}
