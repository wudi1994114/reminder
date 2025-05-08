package com.example.reminder.config;

import com.example.reminder.job.ComplexReminderSchedulingJob;
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

    // --- Bean definitions for ComplexReminderSchedulingJob ---

    @Bean
    public JobDetail complexReminderJobDetail() {
        return JobBuilder.newJob(ComplexReminderSchedulingJob.class)
                .withIdentity("complexReminderSchedulingJob", "reminder-scheduling")
                .withDescription("Job to generate SimpleReminders from ComplexReminder templates")
                .storeDurably() // Store even if no triggers are associated initially
                .build();
    }

    @Bean
    public Trigger complexReminderJobTrigger(JobDetail complexReminderJobDetail) {
        // Run every minute
        return TriggerBuilder.newTrigger()
                .forJob(complexReminderJobDetail)
                .withIdentity("complexReminderSchedulingTrigger", "reminder-scheduling")
                .withDescription("Trigger for complexReminderSchedulingJob - runs every minute")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever() // Repeat indefinitely
                        .withIntervalInSeconds(60)) // Every 60 seconds
                        // .withMisfireHandlingInstructionIgnoreMisfires()) // Or another misfire instruction
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