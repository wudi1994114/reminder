package com.core.reminder.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 二十四节气实体类
 */
@Entity
@Table(name = "solar_term")
public class SolarTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 节气名称（小寒、大寒、立春等）
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 年份
     */
    @Column(nullable = false)
    private Integer year;

    /**
     * 月份
     */
    @Column(nullable = false)
    private Integer month;

    /**
     * 日
     */
    @Column(nullable = false)
    private Integer day;

    /**
     * 宜做的事情
     */
    @Column(length = 1000)
    private String suitable;

    /**
     * 忌做的事情
     */
    @Column(length = 1000)
    private String taboo;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public String getSuitable() {
        return suitable;
    }

    public void setSuitable(String suitable) {
        this.suitable = suitable;
    }

    public String getTaboo() {
        return taboo;
    }

    public void setTaboo(String taboo) {
        this.taboo = taboo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 