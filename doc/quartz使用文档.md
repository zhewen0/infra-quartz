# 1、配置引用
- 初始化sql脚本==quartz.sql==

- 将doc下==spring-quartz.properties==配置文件，放入resources下，调整以下内容

```properties
# 集群名称
org.quartz.scheduler.instanceName=自己服务名字
# quartz数据库配置
org.quartz.dataSource.quartzDataSource.URL=jdbc:mysql://localhost:3306/quartz?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai  
org.quartz.dataSource.quartzDataSource.user=root  
org.quartz.dataSource.quartzDataSource.password=password
# 其他内容按需调整
```

# 2、代码使用
## 1. 创建Job任务##
```java
import org.quartz.DisallowConcurrentExecution;  
import org.quartz.PersistJobDataAfterExecution;  
import org.springframework.scheduling.quartz.QuartzJobBean;  
// 用来控制JobDataMap的持久化行为
@PersistJobDataAfterExecution  
// 禁止同一个 JobDetail 的并发执行
@DisallowConcurrentExecution  
public class TestJob extends QuartzJobBean {  
	@Resource  
	private QuartzService quartzService;
    @Override  
    protected void executeInternal(org.quartz.JobExecutionContext context) {  
        System.out.println("TestJob is executing..."); 
        System.out.println(quartzService.queryAllJob()); 
        System.out.println(context.getJobDetail().getKey().getName());  
    }  
}
```
## 2.将任务注册调度
```java
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
	    // 全部任务 
	    // cron方式
        quartzJobInitializer.scheduleJobCron("job1", "group1", TestJob1.class, "0/5 * * * * ?", null);  
        // 定时方式
       quartzJobInitializer.scheduleJob("job2", "group2", TestJob1.classTestJob1.class, 3);
    }  
}
```