package rw.vikingfc.club.marketing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.security.UserPrincipal;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public void notifyRoles(List<Role> roles, String type, String title, String body, String link) {
        for (User user : userRepository.findByRoleInAndActiveTrueAndDeletedAtIsNull(roles)) {
            notify(user, type, title, body, link);
        }
    }

    public void notify(User user, String type, String title, String body, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setReadFlag(false);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> mineView() {
        return mine().stream().map(notification -> {
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("id", notification.getId());
            body.put("title", notification.getTitle());
            body.put("body", notification.getBody());
            body.put("type", notification.getType());
            body.put("link", notification.getLink());
            body.put("read", notification.isReadFlag());
            body.put("createdAt", notification.getCreatedAt());
            return body;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Notification> mine() {
        UserPrincipal principal = SecurityUtils.principal();
        if (principal == null) {
            return List.of();
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId());
    }

    public Notification markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Notification", id));
        UserPrincipal principal = SecurityUtils.principal();
        if (principal == null || !notification.getUser().getId().equals(principal.getId())) {
            throw NotFoundException.of("Notification", id);
        }
        notification.setReadFlag(true);
        notification.setReadAt(Instant.now(clock));
        return notification;
    }
}
