package rw.vikingfc.club.lineup;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LineupDeadlineJob {

    private final LineupCenterService center;

    @Scheduled(cron = "0 */15 * * * *", zone = "Africa/Kigali")
    public void sweep() {
        center.sweepDeadlines();
    }
}
