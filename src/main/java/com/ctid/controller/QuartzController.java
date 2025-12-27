package com.ctid.controller;

import com.ctid.service.QuartzService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/quartz")
public class QuartzController {
    @Resource
    private QuartzService quartzService;

    @RequestMapping("/addJob")
    public ResponseEntity<String> addJob(@RequestParam String jobClassName,
                                         @RequestParam String jobName,
                                         @RequestParam String jobGroupName,
                                         @RequestParam int jobTime,   // 间隔秒
                                         @RequestParam int jobTimes,  // 执行次数，0表示无限循环
                                         @RequestBody(required = false) Map<String, Object> jobData) throws ClassNotFoundException {
        Class<? extends QuartzJobBean> jobClass = (Class<? extends QuartzJobBean>) Class.forName(jobClassName);
        quartzService.addJob(jobClass, jobName, jobGroupName, jobTime, jobTimes, jobData);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/addCronJob")
    public ResponseEntity<String> addJob(@RequestParam String jobClassName,
                                         @RequestParam String jobName,
                                         @RequestParam String jobGroupName,
                                         @RequestParam String jobTime,   // cron表达式
                                         @RequestBody(required = false) Map<String, Object> jobData) throws ClassNotFoundException {
        Class<? extends QuartzJobBean> jobClass = (Class<? extends QuartzJobBean>) Class.forName(jobClassName);
        quartzService.addJob(jobClass, jobName, jobGroupName, jobTime, jobData);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/updateJob")
    public ResponseEntity<String> updateJob(String jobName, String jobGroupName, String jobTime) {
        quartzService.updateJob(jobName, jobGroupName, jobTime);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/deleteJob")
    public ResponseEntity<String> deleteJob(String jobName, String jobGroupName) {
        quartzService.deleteJob(jobName, jobGroupName);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/pauseJob")
    public ResponseEntity<String> pauseJob(String jobName, String jobGroupName) {
        quartzService.pauseJob(jobName, jobGroupName);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/resumeJob")
    public ResponseEntity<String> resumeJob(String jobName, String jobGroupName) {
        quartzService.resumeJob(jobName, jobGroupName);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("/queryAllJob")
    public ResponseEntity<List<Map<String, Object>>> queryAllJob() {
        List<Map<String, Object>> maps = quartzService.queryAllJob();
        return ResponseEntity.ok().body(maps);
    }

    //    queryRunJob
    @RequestMapping("/queryRunJob")
    public ResponseEntity<List<Map<String, Object>>> queryRunJob() {
        List<Map<String, Object>> maps = quartzService.queryRunJob();
        return ResponseEntity.ok().body(maps);
    }
}
