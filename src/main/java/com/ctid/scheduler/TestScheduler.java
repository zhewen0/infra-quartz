package com.ctid.scheduler;

import com.ctid.initializer.QuartzJobInitializer;
import com.ctid.job.TestJob1;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class TestScheduler {
    @Resource
    private QuartzJobInitializer quartzJobInitializer;
    @PostConstruct
    public void execute() throws SchedulerException {
//        quartzJobInitializer.scheduleJob("1111", "222", TestJob1.class, 7);
        quartzJobInitializer.scheduleJobCron("1111", "222", TestJob1.class, "0/7 * * * * ?",null);
    }
}
