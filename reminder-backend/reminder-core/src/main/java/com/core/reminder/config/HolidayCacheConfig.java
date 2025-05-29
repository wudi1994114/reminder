package com.core.reminder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 节假日缓存配置类
 * 用于配置节假日缓存的相关参数
 */
@Component
@ConfigurationProperties(prefix = "reminder.holiday.cache")
public class HolidayCacheConfig {

    /**
     * 是否启用预加载缓存
     */
    private boolean enabled = true;

    /**
     * 预加载年份范围：当前年份前几年
     */
    private int yearsBefore = 1;

    /**
     * 预加载年份范围：当前年份后几年
     */
    private int yearsAfter = 1;

    /**
     * 是否在启动时异步预加载
     */
    private boolean asyncPreload = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getYearsBefore() {
        return yearsBefore;
    }

    public void setYearsBefore(int yearsBefore) {
        this.yearsBefore = yearsBefore;
    }

    public int getYearsAfter() {
        return yearsAfter;
    }

    public void setYearsAfter(int yearsAfter) {
        this.yearsAfter = yearsAfter;
    }

    public boolean isAsyncPreload() {
        return asyncPreload;
    }

    public void setAsyncPreload(boolean asyncPreload) {
        this.asyncPreload = asyncPreload;
    }
} 