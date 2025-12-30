package com.ctid.service.impl;

import com.ctid.service.QuartzService;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class QuartzServiceImpl implements QuartzService {
    @Autowired
    private Scheduler scheduler;


    /**
     * 增加一个job
     *
     * @param jobClass     任务实现类
     * @param jobName      任务名称
     * @param jobGroupName 任务组名
     * @param jobTime      时间表达式 (这是每隔多少秒为一次任务)
     * @param jobTimes     运行的次数 （<0:表示不限次数）
     * @param jobData      参数
     */
    @Override
    public void addJob(Class<? extends QuartzJobBean> jobClass, String jobName, String jobGroupName, int jobTime,
                       int jobTimes, Map jobData) {
        try {
            log.info("addJob, jobName: {}, jobGroupName: {}, jobTime: {}, jobTimes: {}, jobData: {}", jobName, jobGroupName, jobTime, jobTimes, jobData);
            // 任务名称和组构成任务key
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName)
                    .build();
            // 设置job参数
            if (jobData != null && !jobData.isEmpty()) {
                jobDetail.getJobDataMap().putAll(jobData);
            }
            // 使用simpleTrigger规则
            Trigger trigger = null;
            if (jobTimes < 0) {
                trigger = TriggerBuilder.newTrigger().withIdentity(jobName.concat("_trigger"), jobGroupName)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(jobTime).repeatForever())
                        .startNow().build();
            } else {
                trigger = TriggerBuilder.newTrigger().withIdentity(jobName.concat("_trigger"), jobGroupName).withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(jobTime).withRepeatCount(jobTimes))
                        .startNow().build();
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            log.error("add job error!", e);
            throw new RuntimeException("add job error!");
        }
    }

    /**
     * 增加一个job
     *
     * @param jobClass     任务实现类
     * @param jobName      任务名称(建议唯一)
     * @param jobGroupName 任务组名
     * @param jobTime      时间表达式 （如：0/5 * * * * ? ）
     * @param misfire      任务过期策略（null:默认=2 1:忽略 2:触发一次 3:不触发）
     * @param jobData      参数
     */
    @Override
    public void addJob(Class<? extends QuartzJobBean> jobClass, String jobName, String jobGroupName, String jobTime, Integer misfire, Map jobData) {
        try {
            log.info("addJob, jobName: {}, jobGroupName: {}, jobTime: {}, jobData: {}", jobName, jobGroupName, jobTime, jobData);
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
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobTime);
            if (misfire != null) {
                switch (misfire) {
                    case 1:
                        cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
                        break;
                    case 2:
                        cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
                        break;
                    case 3:
                        cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                        break;
                }
            }
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName.concat("_trigger"), jobGroupName)
                    .withSchedule(cronScheduleBuilder).startNow().build();
            // 把作业和触发器注册到任务调度中
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            log.error("add job error", e);
            throw new RuntimeException("add job error!");
        }
    }

    /**
     * 修改 一个job的 时间表达式
     *
     * @param jobName
     * @param jobGroupName
     * @param jobTime
     * @param misfire 任务过期策略（null:默认=2 1:忽略 2:触发一次 3:不触发）
     */
    @Override
    public void updateJob(String jobName, String jobGroupName, String jobTime, Integer misfire) {
        try {
            log.info("updateJob, jobName: {}, jobGroupName: {}, jobTime: {}", jobName, jobGroupName, jobTime);
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroupName);
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobTime);
            if (misfire != null) {
                switch (misfire) {
                    case 1:
                        cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
                        break;
                    case 2:
                        cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
                        break;
                    case 3:
                        cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                        break;
                }
            }
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey)
                    .withSchedule(cronScheduleBuilder).build();
            // 重启触发器
            scheduler.rescheduleJob(triggerKey, trigger);
        } catch (SchedulerException e) {
            log.error("update job error!", e);
            throw new RuntimeException("update job error!");
        }
    }

    /**
     * 删除任务一个job
     *
     * @param jobName      任务名称
     * @param jobGroupName 任务组名
     */
    @Override
    public void deleteJob(String jobName, String jobGroupName) {
        try {
            log.info("deleteJob, jobName: {}, jobGroupName: {}", jobName, jobGroupName);
            boolean b = scheduler.deleteJob(new JobKey(jobName, jobGroupName));
            log.info("deleteJob, jobName: {}, jobGroupName: {}，result: {}", jobName, jobGroupName, b);
        } catch (Exception e) {
            log.error("", e);
            throw new RuntimeException("delete job error!");
        }
    }

    /**
     * 暂停一个job
     *
     * @param jobName
     * @param jobGroupName
     */
    @Override
    public void pauseJob(String jobName, String jobGroupName) {
        try {
            log.info("pauseJob, jobName: {}, jobGroupName: {}", jobName, jobGroupName);
            JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
            scheduler.pauseJob(jobKey);
        } catch (SchedulerException e) {
            log.error("pause job error!", e);
            throw new RuntimeException("pause job error!");
        }
    }

    /**
     * 恢复一个job
     *
     * @param jobName
     * @param jobGroupName
     */
    @Override
    public void resumeJob(String jobName, String jobGroupName) {
        try {
            log.info("resumeJob, jobName: {}, jobGroupName: {}", jobName, jobGroupName);
            JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
            scheduler.resumeJob(jobKey);
        } catch (SchedulerException e) {
            log.error("resume job error!", e);
            throw new RuntimeException("resume job error!");
        }
    }

    /**
     * 立即执行一个job
     *
     * @param jobName
     * @param jobGroupName
     */
    @Override
    public void runAJobNow(String jobName, String jobGroupName) {
        try {
            log.info("runAJobNow, jobName: {}, jobGroupName: {}", jobName, jobGroupName);
            JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            log.error("run a job error!", e);
            throw new RuntimeException("run a job error!");
        }
    }

    /**
     * 获取所有计划中的任务列表
     *
     * @return
     */
    @Override
    public List<Map<String, Object>> queryAllJob() {
        List<Map<String, Object>> jobList = null;
        try {
            GroupMatcher<JobKey> matcher = GroupMatcher.anyJobGroup();
            Set<JobKey> jobKeys = scheduler.getJobKeys(matcher);
            jobList = new ArrayList<Map<String, Object>>();
            for (JobKey jobKey : jobKeys) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                for (Trigger trigger : triggers) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("jobName", jobKey.getName());
                    map.put("jobGroupName", jobKey.getGroup());
                    map.put("description", "触发器:" + trigger.getKey());
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    map.put("jobStatus", triggerState.name());
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        String cronExpression = cronTrigger.getCronExpression();
                        map.put("jobTime", cronExpression);
                    }
                    jobList.add(map);
                }
            }
        } catch (SchedulerException e) {
            log.error("query all jobs error!", e);
            throw new RuntimeException("query all jobs error!");
        }
        return jobList;
    }

    /**
     * 获取所有正在运行的job
     *
     * @return
     */
    @Override
    public List<Map<String, Object>> queryRunJob() {
        List<Map<String, Object>> jobList = null;
        try {
            List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
            jobList = new ArrayList<Map<String, Object>>(executingJobs.size());
            for (JobExecutionContext executingJob : executingJobs) {
                Map<String, Object> map = new HashMap<String, Object>();
                JobDetail jobDetail = executingJob.getJobDetail();
                JobKey jobKey = jobDetail.getKey();
                Trigger trigger = executingJob.getTrigger();
                map.put("jobName", jobKey.getName());
                map.put("jobGroupName", jobKey.getGroup());
                map.put("description", "触发器:" + trigger.getKey());
                Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                map.put("jobStatus", triggerState.name());
                if (trigger instanceof CronTrigger) {
                    CronTrigger cronTrigger = (CronTrigger) trigger;
                    String cronExpression = cronTrigger.getCronExpression();
                    map.put("jobTime", cronExpression);
                }
                jobList.add(map);
            }
        } catch (SchedulerException e) {
            log.error("query run jobs error!", e);
            throw new RuntimeException("query run jobs error!");
        }
        return jobList;
    }

}
