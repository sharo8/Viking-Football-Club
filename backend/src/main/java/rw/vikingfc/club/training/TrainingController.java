package rw.vikingfc.club.training;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Tag(name = "Training")
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','DOCTOR')")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Training sessions", trainingService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody TrainingService.SessionWrite request) {
        return ApiResponse.ok("Training session created", trainingService.create(request));
    }
}
