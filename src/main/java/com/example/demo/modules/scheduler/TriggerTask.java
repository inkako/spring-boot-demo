package com.example.demo.modules.scheduler;

import com.example.demo.config.TaskScheduleConfigProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
//@RefreshScope
@RequiredArgsConstructor
public class TriggerTask implements SchedulingConfigurer {
    private final TaskScheduleConfigProps taskScheduleConfigProps;
    private final TaskScheduler taskScheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 可以不手动设置
        taskRegistrar.setScheduler(taskScheduler);
        // 添加任务
        taskRegistrar.addTriggerTask(
                () -> {
                    log.info("TriggerTask executed:{} ：{}",
                            System.identityHashCode(this), System.identityHashCode(taskScheduleConfigProps));
                },
                triggerContext -> {
                    // 可动态配置的频率
                    PeriodicTrigger trigger = new PeriodicTrigger(taskScheduleConfigProps.getFixedRate());
                    Date nextDate = trigger.nextExecutionTime(triggerContext);
                    return nextDate != null ? nextDate.toInstant() : null;
                });
    }
}