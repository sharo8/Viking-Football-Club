package rw.vikingfc.club.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;

    public Map<String, Object> inbox() {
        User me = me();
        ensureGroups();
        List<Map<String, Object>> rows = new ArrayList<>();
        int unread = 0;
        for (ConversationMember membership : members.findByUserId(me.getId())) {
            Conversation conversation = conversations.findById(membership.getConversationId()).orElse(null);
            if (conversation == null) {
                continue;
            }
            List<ChatMessage> latest = messages.findTop50ByConversationIdOrderByCreatedAtDesc(conversation.getId());
            ChatMessage last = latest.isEmpty() ? null : latest.get(0);
            long badge = membership.getLastReadAt() == null
                    ? latest.size()
                    : messages.countByConversationIdAndCreatedAtAfter(conversation.getId(), membership.getLastReadAt());
            unread += (int) badge;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", conversation.getId());
            row.put("kind", conversation.getKind());
            row.put("title", displayTitle(conversation, me));
            row.put("pinned", membership.isPinned());
            row.put("muted", membership.isMuted());
            row.put("unread", badge);
            row.put("preview", last == null ? "" : last.getBody());
            row.put("time", last == null ? conversation.getCreatedAt() : last.getCreatedAt());
            rows.add(row);
        }
        return Map.of("unread", unread, "conversations", rows);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(Long conversationId) {
        requireMember(conversationId);
        return messages.findTop50ByConversationIdOrderByCreatedAtDesc(conversationId).stream()
                .sorted(java.util.Comparator.comparing(ChatMessage::getCreatedAt))
                .map(this::bubble)
                .toList();
    }

    public Map<String, Object> send(Long conversationId, String body, Long replyToId) {
        User me = requireMember(conversationId);
        if (body == null || body.isBlank()) {
            throw BusinessException.badRequest("Write a message first");
        }
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(me.getId());
        message.setBody(body.trim());
        message.setReplyToId(replyToId);
        message.setStatus("SENT");
        messages.save(message);
        Map<String, Object> bubble = bubble(message);
        messaging.convertAndSend("/topic/chat." + conversationId, bubble);
        return bubble;
    }

    public void read(Long conversationId) {
        User me = requireMember(conversationId);
        members.findByConversationIdAndUserId(conversationId, me.getId()).ifPresent(membership -> {
            membership.setLastReadAt(Instant.now());
        });
    }

    public Long direct(String email) {
        User me = me();
        User other = users.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() -> BusinessException.badRequest("That staff member was not found"));
        for (ConversationMember membership : members.findByUserId(me.getId())) {
            Conversation existing = conversations.findById(membership.getConversationId()).orElse(null);
            if (existing != null && "DIRECT".equals(existing.getKind())
                    && members.findByConversationIdAndUserId(existing.getId(), other.getId()).isPresent()) {
                return existing.getId();
            }
        }
        Conversation created = new Conversation();
        created.setKind("DIRECT");
        created.setTitle("Direct");
        conversations.save(created);
        add(created.getId(), me.getId());
        add(created.getId(), other.getId());
        return created.getId();
    }

    private String displayTitle(Conversation conversation, User me) {
        if (!"DIRECT".equals(conversation.getKind())) {
            return conversation.getTitle();
        }
        return members.findByConversationId(conversation.getId()).stream()
                .filter(member -> !member.getUserId().equals(me.getId()))
                .map(member -> users.findById(member.getUserId()).map(User::getFullName).orElse(conversation.getTitle()))
                .findFirst()
                .orElse(conversation.getTitle());
    }

    private void ensureGroups() {
        group("Management", Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER, Role.ASSISTANT_MANAGER);
        group("Technical staff", Role.HEAD_COACH, Role.ASSISTANT_COACH, Role.GOALKEEPER_COACH, Role.DOCTOR);
        group("Media", Role.SOCIAL_MEDIA_MANAGER, Role.MARKETING_MANAGER);
        group("All staff", Role.values());
    }

    private void group(String title, Role... roles) {
        Conversation conversation = conversations.findByTitleAndKind(title, "GROUP").orElseGet(() -> {
            Conversation created = new Conversation();
            created.setKind("GROUP");
            created.setTitle(title);
            return conversations.save(created);
        });
        for (User user : users.findByRoleInAndActiveTrueAndDeletedAtIsNull(List.of(roles))) {
            if (members.findByConversationIdAndUserId(conversation.getId(), user.getId()).isEmpty()) {
                add(conversation.getId(), user.getId());
            }
        }
    }

    private void add(Long conversationId, Long userId) {
        ConversationMember membership = new ConversationMember();
        membership.setConversationId(conversationId);
        membership.setUserId(userId);
        members.save(membership);
    }

    private User requireMember(Long conversationId) {
        User me = me();
        if (members.findByConversationIdAndUserId(conversationId, me.getId()).isEmpty()) {
            throw BusinessException.forbidden("You are not in this conversation");
        }
        return me;
    }

    private User me() {
        return users.findByEmailIgnoreCaseAndDeletedAtIsNull(SecurityUtils.email())
                .orElseThrow(() -> BusinessException.badRequest("Sign in first"));
    }

    private Map<String, Object> bubble(ChatMessage message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", message.getId());
        row.put("conversationId", message.getConversationId());
        row.put("senderId", message.getSenderId());
        row.put("body", message.getBody());
        row.put("replyToId", message.getReplyToId());
        row.put("status", message.getStatus());
        row.put("mine", message.getSenderId().equals(me().getId()));
        row.put("createdAt", message.getCreatedAt());
        return row;
    }
}
