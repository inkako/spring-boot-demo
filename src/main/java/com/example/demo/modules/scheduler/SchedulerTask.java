package com.example.demo.modules.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
//@Component
@RequiredArgsConstructor
public class SchedulerTask {
    @Scheduled(fixedRateString = "${task.scheduler.fixedRate}")
    public void run() {
        log.info("Scheduler task executed");
    }
}
