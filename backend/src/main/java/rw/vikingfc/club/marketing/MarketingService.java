package rw.vikingfc.club.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.Money;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketingService {

    private final ContentPostRepository postRepository;
    private final CampaignRepository campaignRepository;
    private final MatchdayTaskRepository taskRepository;
    private final AnnouncementRepository announcementRepository;
    private final FootballMatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public Map<String, Object> createPost(PostWrite request) {
        ContentPost post = new ContentPost();
        post.setPlatform(request.platform());
        post.setCaption(request.caption());
        post.setScheduledAt(request.scheduledAt());
        post.setStatus(request.scheduledAt() == null ? PostStatus.DRAFT : PostStatus.SCHEDULED);
        var principal = SecurityUtils.principal();
        if (principal != null) {
            post.setOwner(userRepository.findById(principal.getId()).orElse(null));
        }
        if (request.matchId() != null) {
            post.setMatch(matchRepository.findById(request.matchId()).orElse(null));
        }
        if (request.playerId() != null) {
            post.setPlayer(playerRepository.findById(request.playerId()).orElse(null));
        }
        return postView(postRepository.save(post));
    }

    public Map<String, Object> publishPost(Long id) {
        ContentPost post = postRepository.findById(id).orElseThrow(() -> NotFoundException.of("Content post", id));
        post.setStatus(PostStatus.PUBLISHED);
        return postView(post);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calendar() {
        return postRepository.findAll().stream()
                .sorted((left, right) -> {
                    Instant a = left.getScheduledAt();
                    Instant b = right.getScheduledAt();
                    if (a == null && b == null) {
                        return 0;
                    }
                    if (a == null) {
                        return 1;
                    }
                    if (b == null) {
                        return -1;
                    }
                    return a.compareTo(b);
                })
                .map(this::postView)
                .toList();
    }

    public Map<String, Object> createCampaign(CampaignWrite request) {
        Campaign campaign = new Campaign();
        campaign.setName(request.name());
        campaign.setBudget(Money.nz(request.budget()));
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setGoal(request.goal());
        campaign.setStatus(CampaignStatus.DRAFT);
        return campaignView(campaignRepository.save(campaign));
    }

    public Map<String, Object> updateCampaign(Long id, CampaignStatus status, String results, Long reach, Long engagement) {
        Campaign campaign = campaignRepository.findById(id).orElseThrow(() -> NotFoundException.of("Campaign", id));
        if (status != null) {
            campaign.setStatus(status);
        }
        campaign.setResultsNote(results);
        campaign.setReach(reach);
        campaign.setEngagement(engagement);
        return campaignView(campaign);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> campaigns() {
        return campaignRepository.findAll().stream().map(this::campaignView).toList();
    }

    public Map<String, Object> addTask(Long matchId, String label, String ownerRole) {
        var match = matchRepository.findById(matchId).orElseThrow(() -> NotFoundException.of("Match", matchId));
        MatchdayTask task = new MatchdayTask();
        task.setMatch(match);
        task.setLabel(label);
        task.setOwnerRole(ownerRole);
        task.setDone(false);
        taskRepository.save(task);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", task.getId());
        body.put("matchId", matchId);
        body.put("label", label);
        body.put("done", false);
        body.put("ownerRole", ownerRole);
        return body;
    }

    public Map<String, Object> toggleTask(Long id) {
        MatchdayTask task = taskRepository.findById(id).orElseThrow(() -> NotFoundException.of("Match-day task", id));
        task.setDone(!task.isDone());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", task.getId());
        body.put("done", task.isDone());
        body.put("label", task.getLabel());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tasks(Long matchId) {
        return taskRepository.findAll().stream()
                .filter(task -> matchId == null || task.getMatch().getId().equals(matchId))
                .map(task -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("id", task.getId());
                    body.put("matchId", task.getMatch().getId());
                    body.put("label", task.getLabel());
                    body.put("done", task.isDone());
                    body.put("ownerRole", task.getOwnerRole());
                    return body;
                }).toList();
    }

    public Map<String, Object> announce(String title, String body, Set<Role> roles) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setBody(body);
        announcement.setAudiences(roles == null || roles.isEmpty() ? EnumSet.allOf(Role.class) : EnumSet.copyOf(roles));
        announcementRepository.save(announcement);
        notificationService.notifyRoles(announcement.getAudiences().stream().toList(), "ANNOUNCEMENT", title, body, "/announcements");
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", announcement.getId());
        view.put("title", announcement.getTitle());
        view.put("body", announcement.getBody());
        view.put("audiences", announcement.getAudiences());
        return view;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> announcements() {
        var principal = SecurityUtils.principal();
        Role role = principal == null ? null : principal.getRole();
        return announcementRepository.findAll().stream()
                .filter(item -> role != null && (item.getAudiences().isEmpty() || item.getAudiences().contains(role) || role.isExecutive()))
                .map(item -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", item.getId());
                    view.put("title", item.getTitle());
                    view.put("body", item.getBody());
                    view.put("audiences", item.getAudiences());
                    return view;
                }).toList();
    }

    private Map<String, Object> postView(ContentPost post) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", post.getId());
        body.put("platform", post.getPlatform());
        body.put("caption", post.getCaption());
        body.put("scheduledAt", post.getScheduledAt());
        body.put("status", post.getStatus());
        body.put("matchId", post.getMatch() == null ? null : post.getMatch().getId());
        body.put("playerId", post.getPlayer() == null ? null : post.getPlayer().getId());
        return body;
    }

    private Map<String, Object> campaignView(Campaign campaign) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", campaign.getId());
        body.put("name", campaign.getName());
        body.put("budget", campaign.getBudget());
        body.put("startDate", campaign.getStartDate());
        body.put("endDate", campaign.getEndDate());
        body.put("goal", campaign.getGoal());
        body.put("status", campaign.getStatus());
        body.put("results", campaign.getResultsNote());
        body.put("reach", campaign.getReach());
        body.put("engagement", campaign.getEngagement());
        return body;
    }

    public record PostWrite(@NotNull Platform platform, @NotBlank String caption, Instant scheduledAt, Long matchId, Long playerId) {
    }

    public record CampaignWrite(@NotBlank String name, BigDecimal budget, LocalDate startDate, LocalDate endDate, String goal) {
    }
}
