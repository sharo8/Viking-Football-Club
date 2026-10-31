package rw.vikingfc.club.media;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.match.FootballMatch;
import rw.vikingfc.club.match.FootballMatchRepository;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.user.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {

    private final MediaAssetRepository assetRepository;
    private final FileStorageService storageService;
    private final PlayerRepository playerRepository;
    private final FootballMatchRepository matchRepository;
    private final UserRepository userRepository;

    public Map<String, Object> upload(String title, String tags, String caption, Long playerId, Long matchId, MultipartFile file) {
        StoredFile stored = storageService.store(file, "library");
        MediaAsset asset = new MediaAsset();
        asset.setTitle(title == null || title.isBlank() ? stored.originalName() : title.trim());
        asset.setUrl(stored.url());
        asset.setThumbnailUrl(stored.thumbnailUrl());
        asset.setTags(tags);
        asset.setCaption(caption);
        if (playerId != null) {
            asset.setPlayer(playerRepository.findById(playerId).orElse(null));
        }
        if (matchId != null) {
            asset.setMatch(matchRepository.findById(matchId).orElse(null));
        }
        var principal = SecurityUtils.principal();
        if (principal != null) {
            asset.setOwner(userRepository.findById(principal.getId()).orElse(null));
        }
        return view(assetRepository.save(asset));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String tag) {
        return assetRepository.findAll().stream()
                .filter(asset -> tag == null || tag.isBlank() || (asset.getTags() != null && asset.getTags().toLowerCase().contains(tag.toLowerCase())))
                .map(this::view)
                .toList();
    }

    private Map<String, Object> view(MediaAsset asset) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", asset.getId());
        body.put("title", asset.getTitle());
        body.put("url", asset.getUrl());
        body.put("thumbnailUrl", asset.getThumbnailUrl());
        body.put("tags", asset.getTags());
        body.put("caption", asset.getCaption());
        Player player = asset.getPlayer();
        FootballMatch match = asset.getMatch();
        body.put("playerId", player == null ? null : player.getId());
        body.put("matchId", match == null ? null : match.getId());
        return body;
    }
}
