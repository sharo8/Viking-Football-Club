package rw.vikingfc.club.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslateController {

    private final TranslateService translateService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ApiResponse<Map<String, String>> translate(@RequestBody Map<String, Object> body) {
        String text = body.get("text") == null ? "" : String.valueOf(body.get("text"));
        String from = body.get("from") == null ? "en" : String.valueOf(body.get("from"));
        @SuppressWarnings("unchecked")
        List<String> to = body.get("to") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return ApiResponse.ok("Translations", translateService.translate(text, from, to));
    }
}
