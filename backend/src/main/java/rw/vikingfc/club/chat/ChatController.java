package rw.vikingfc.club.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> inbox() {
        return ApiResponse.ok("Inbox", chatService.inbox());
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable Long id) {
        return ApiResponse.ok("Messages", chatService.history(id));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> send(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object reply = body.get("replyToId");
        return ApiResponse.ok("Sent", chatService.send(id, String.valueOf(body.get("body")), reply == null ? null : Long.valueOf(reply.toString())));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> read(@PathVariable Long id) {
        chatService.read(id);
        return ApiResponse.ok("Read", null);
    }

    @PostMapping("/direct")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> direct(@RequestBody Map<String, String> body) {
        return ApiResponse.ok("Conversation", chatService.direct(body.get("email")));
    }
}
