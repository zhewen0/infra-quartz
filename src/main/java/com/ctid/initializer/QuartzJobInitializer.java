package com.ctid.initializer;

import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
@Component
public class QuartzJobInitializer {
    @Resource
    private Scheduler scheduler;
    /**
     * 定时任务初始化
     * @param jobName 任务名称
     * @param groupName 任务组名
     * @param jobClass 任务类
     * @param intervalSeconds 任务执行间隔（秒）
     * @throws SchedulerException
     */
    public void scheduleJob(String jobName, String groupName, Class<? extends QuartzJobBean> jobClass, int intervalSeconds) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, groupName)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName.concat("_trigger"), groupName)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(intervalSeconds)
                        .repeatForever())
                .build();

        if (!scheduler.checkExists(jobDetail.getKey())) {
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    /**
     * 定时任务初始化
     * @param jobName 任务名称
     * @param jobGroupName 任务组名
     * @param jobClass 任务类
     * @param jobTime 任务执行时间（cron表达式）
     * @param jobData 任务参数
     * @throws SchedulerException
     */
    public void scheduleJobCron(String jobName, String jobGroupName, Class<? extends QuartzJobBean> jobClass, String jobTime, Map jobData) throws SchedulerException {
        // 创建jobDetail实例，绑定Job实现类
        // 指明job的名称，所在组的名称，以及绑定job类
        // 任务名称和组构成任务key
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName)
                .build();
        // 设置job参数
        if (jobData != null && !jobData.isEmpty()) {
            jobDetail.getJobDataMap().putAll(jobData);
        }
        // 定义调度触发规则
        // 使用cornTrigger规则
        // 触发器key
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName.concat("_trigger"), jobGroupName)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(CronScheduleBuilder.cronSchedule(jobTime)).startNow().build();
        // 把作业和触发器注册到任务调度中
        if (!scheduler.checkExists(jobDetail.getKey())) {
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }
}
