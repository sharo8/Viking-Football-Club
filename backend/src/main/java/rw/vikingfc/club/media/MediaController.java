package rw.vikingfc.club.media;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media")
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String tag) {
        return ApiResponse.ok("Media library", mediaService.list(tag));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> upload(@RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String tags,
                                                    @RequestParam(required = false) String caption,
                                                    @RequestParam(required = false) Long playerId,
                                                    @RequestParam(required = false) Long matchId,
                                                    @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Media stored", mediaService.upload(title, tags, caption, playerId, matchId, file));
    }
}
