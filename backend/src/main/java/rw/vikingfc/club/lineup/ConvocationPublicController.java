package rw.vikingfc.club.lineup;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ConvocationPublicController {

    private final LineupCenterService center;

    @GetMapping("/convocation/{token}")
    public ApiResponse<Map<String, Object>> read(@PathVariable String token) {
        return ApiResponse.ok("Convocation", center.publicCall(token));
    }

    @PostMapping("/convocation/{token}")
    public ApiResponse<Map<String, Object>> answer(@PathVariable String token, @RequestBody Answer body) {
        return ApiResponse.ok("Answer saved", center.answerCall(token, body.response()));
    }

    @GetMapping("/lineups/next")
    public ApiResponse<Map<String, Object>> next() {
        return ApiResponse.ok("Lineup", center.publicNext());
    }

    public record Answer(String response) {
    }
}
