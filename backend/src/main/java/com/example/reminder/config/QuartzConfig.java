package com.example.reminder.config;

import com.example.reminder.job.ComplexReminderSchedulingJob;
import com.example.reminder.job.MonthlyComplexReminderJob;
import org.quartz.*;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfig {

    // 自定义 JobFactory，使 Job 能够自动注入 Spring Bean
    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

        private transient AutowireCapableBeanFactory beanFactory;

        @Override
        public void setApplicationContext(final ApplicationContext context) {
            beanFactory = context.getAutowireCapableBeanFactory();
        }

        @Override
        protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
            final Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        }
    }

    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    // --- ComplexReminderSchedulingJob的Bean定义 ---

    @Bean
    public JobDetail complexReminderJobDetail() {
        return JobBuilder.newJob(ComplexReminderSchedulingJob.class)
                .withIdentity("complexReminderSchedulingJob", "reminder-scheduling")
                .withDescription("从ComplexReminder模板生成SimpleReminders的任务")
                .storeDurably() // 即使初始没有关联触发器也存储
                .build();
    }

    @Bean
    public Trigger complexReminderJobTrigger(JobDetail complexReminderJobDetail) {
        // 每分钟运行一次
        return TriggerBuilder.newTrigger()
                .forJob(complexReminderJobDetail)
                .withIdentity("complexReminderSchedulingTrigger", "reminder-scheduling")
                .withDescription("complexReminderSchedulingJob的触发器 - 每分钟运行一次")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever() // 无限重复
                        .withIntervalInSeconds(60)) // 每60秒一次
                        // .withMisfireHandlingInstructionIgnoreMisfires()) // 或其他错过处理指令
                .build();
    }

    // --- MonthlyComplexReminderJob的Bean定义 ---
    
    @Bean
    public JobDetail monthlyComplexReminderJobDetail() {
        return JobBuilder.newJob(MonthlyComplexReminderJob.class)
                .withIdentity("monthlyComplexReminderJob", "reminder-scheduling")
                .withDescription("每月检查并生成未来3个月简单任务的定时任务")
                .storeDurably()
                .build();
    }
    
    @Bean
    public Trigger monthlyComplexReminderJobTrigger(JobDetail monthlyComplexReminderJobDetail) {
        // 使用Cron表达式：每月1日凌晨2点执行
        return TriggerBuilder.newTrigger()
                .forJob(monthlyComplexReminderJobDetail)
                .withIdentity("monthlyComplexReminderTrigger", "reminder-scheduling")
                .withDescription("monthlyComplexReminderJob的触发器 - 每月1日凌晨2点执行")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 1 * ?")
                        .withMisfireHandlingInstructionFireAndProceed()) // 错过后执行一次，然后按照正常计划继续
                .build();
    }

    // 如果需要更复杂的 Quartz 配置（例如 JDBC JobStore），可以在这里添加
    // 例如，配置 SchedulerFactoryBean
    /*
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, JobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setDataSource(dataSource); // 配置数据源，用于持久化 Job
        factory.setJobFactory(jobFactory); // 设置自定义 JobFactory
        // 其他配置...
        return factory;
    }
    */
} 