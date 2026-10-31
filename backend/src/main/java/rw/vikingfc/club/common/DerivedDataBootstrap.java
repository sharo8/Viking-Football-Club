package rw.vikingfc.club.common;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.finance.PayrollScheduleService;
import rw.vikingfc.club.sanction.FineService;

import java.time.Clock;
import java.time.LocalDate;

@Component
@Order(20)
@RequiredArgsConstructor
public class DerivedDataBootstrap implements ApplicationRunner {

    private final AppProperties properties;
    private final PayrollScheduleService payrollScheduleService;
    private final FineService fineService;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.bootstrapDerivedData()) {
            return;
        }
        payrollScheduleService.ensureAll();
        fineService.ensureFromAttendance();
        payrollScheduleService.refreshOverdue(LocalDate.now(clock));
    }
}
