package com.ctid.job;

import com.ctid.service.QuartzService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class TestJob1 extends QuartzJobBean {
    @Resource
    private QuartzService quartzService;
    @Override
    protected void executeInternal(org.quartz.JobExecutionContext context) {
        long scheduledFireTime = context.getScheduledFireTime().getTime();

//        System.out.println(quartzService.queryAllJob());
        String format = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("TestJob-X is executing..." + format);
    }
}
