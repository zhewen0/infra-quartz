package com.ctid.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class TestJob extends QuartzJobBean {
    @Resource
    private Scheduler scheduler;
    @Override
    protected void executeInternal(org.quartz.JobExecutionContext context) {
        try {
            System.out.println("TestJob is executing..."+ scheduler.getSchedulerName());
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
        System.out.println(context.getJobDetail().getKey().getName());
    }
}
