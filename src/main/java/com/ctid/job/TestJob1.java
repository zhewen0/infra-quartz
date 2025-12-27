package com.ctid.job;

import com.ctid.service.QuartzService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;

@PersistJobDataAfterExecution
//@DisallowConcurrentExecution
public class TestJob1 extends QuartzJobBean {
    @Resource
    private QuartzService quartzService;
    @Override
    protected void executeInternal(org.quartz.JobExecutionContext context) {
        System.out.println(quartzService.queryAllJob());
        System.out.println("TestJob-X is executing...");
        System.out.println(context.getJobDetail().getKey().getName());
    }
}
