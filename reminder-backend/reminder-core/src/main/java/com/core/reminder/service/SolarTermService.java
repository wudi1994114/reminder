package com.core.reminder.service;

import com.core.reminder.model.SolarTerm;
import com.core.reminder.model.LegalHoliday;
import com.core.reminder.repository.SolarTermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 节气服务，使用Java内置API计算24节气信息
 */
@Service
public class SolarTermService {
    
    @Autowired
    private SolarTermRepository solarTermRepository;
    
    // 二十四节气名称
    private static final String[] TERM_NAMES = {
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    
    // 1900-2100年节气计算参数
    private static final double[] TERM_INFO = {
        0, 21208.0, 42467.0, 63836.0, 85337.0, 107014.0,
        128867.0, 150921.0, 173149.0, 195551.0, 218072.0, 240693.0,
        263343.0, 285989.0, 308563.0, 331033.0, 353350.0, 375494.0,
        397447.0, 419210.0, 440795.0, 462224.0, 483532.0, 504758.0
    };
    
    /**
     * 获取指定年份的24个节气
     * @param year 年份
     * @return 包含该年24个节气的SolarTerm列表
     */
    public List<SolarTerm> getSolarTermsByYear(int year) {
        // 首先检查数据库中是否已存在该年的节气数据
        if (solarTermRepository.existsByYear(year)) {
            return solarTermRepository.findByYear(year);
        }
        
        // 如果不存在，则计算并保存
        List<SolarTerm> terms = calculateSolarTermsForYear(year);
        solarTermRepository.saveAll(terms);
        return terms;
    }
    
    /**
     * 获取指定年份范围内的所有节气
     * @param startYear 开始年份
     * @param endYear 结束年份
     * @return 节气列表
     */
    public List<SolarTerm> getSolarTermsByYearRange(int startYear, int endYear) {
        List<SolarTerm> allTerms = new ArrayList<>();
        
        // 获取数据库中已有的节气数据
        allTerms.addAll(solarTermRepository.findByYearRange(startYear, endYear));
        
        // 检查并计算缺失的年份数据
        for (int year = startYear; year <= endYear; year++) {
            if (!solarTermRepository.existsByYear(year)) {
                List<SolarTerm> terms = calculateSolarTermsForYear(year);
                solarTermRepository.saveAll(terms);
                allTerms.addAll(terms);
            }
        }
        
        return allTerms;
    }
    
    /**
     * 计算指定年份的24个节气
     * @param year 年份
     * @return 包含该年24个节气的SolarTerm列表
     */
    private List<SolarTerm> calculateSolarTermsForYear(int year) {
        List<SolarTerm> solarTerms = new ArrayList<>();
        
        // 遍历24节气
        for (int i = 0; i < 24; i++) {
            try {
                // 计算节气日期
                Calendar calendar = getSolarTermCalendar(year, i);
                
                SolarTerm term = new SolarTerm();
                term.setName(TERM_NAMES[i]);
                term.setYear(calendar.get(Calendar.YEAR));
                term.setMonth(calendar.get(Calendar.MONTH) + 1); // Calendar月份从0开始
                term.setDay(calendar.get(Calendar.DAY_OF_MONTH));
                
                // 设置创建和更新时间
                LocalDateTime now = LocalDateTime.now();
                term.setCreatedAt(now);
                term.setUpdatedAt(now);
                
                // 初始化宜忌字段为空，后续可通过其他服务填充
                term.setSuitable("");
                term.setTaboo("");
                
                solarTerms.add(term);
            } catch (Exception e) {
                // 如果计算某个节气出错，继续处理下一个
                e.printStackTrace();
            }
        }
        
        return solarTerms;
    }
    
    /**
     * 获取指定日期的节气
     * @param year 年
     * @param month 月
     * @param day 日
     * @return 节气对象，如果不是节气日返回null
     */
    public SolarTerm getSolarTermByDate(int year, int month, int day) {
        // 首先从数据库查询
        SolarTerm term = solarTermRepository.findByYearAndMonthAndDay(year, month, day);
        if (term != null) {
            return term;
        }
        
        // 如果数据库中不存在，尝试计算
        try {
            // 检查该日期是否为节气
            for (int i = 0; i < 24; i++) {
                Calendar termCalendar = getSolarTermCalendar(year, i);
                if (termCalendar.get(Calendar.YEAR) == year && 
                    termCalendar.get(Calendar.MONTH) + 1 == month && 
                    termCalendar.get(Calendar.DAY_OF_MONTH) == day) {
                    
                    // 创建新的节气对象
                    SolarTerm newTerm = new SolarTerm();
                    newTerm.setName(TERM_NAMES[i]);
                    newTerm.setYear(year);
                    newTerm.setMonth(month);
                    newTerm.setDay(day);
                    
                    // 设置创建和更新时间
                    LocalDateTime now = LocalDateTime.now();
                    newTerm.setCreatedAt(now);
                    newTerm.setUpdatedAt(now);
                    
                    // 初始化宜忌字段为空
                    newTerm.setSuitable("");
                    newTerm.setTaboo("");
                    
                    // 保存并返回
                    return solarTermRepository.save(newTerm);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 更新节气的宜忌信息
     * @param id 节气ID
     * @param suitable 宜做的事情
     * @param taboo 忌做的事情
     * @return 更新后的节气对象
     */
    public SolarTerm updateSolarTermSuitableAndTaboo(Long id, String suitable, String taboo) {
        SolarTerm term = solarTermRepository.findById(id).orElse(null);
        if (term != null) {
            term.setSuitable(suitable);
            term.setTaboo(taboo);
            term.setUpdatedAt(LocalDateTime.now());
            return solarTermRepository.save(term);
        }
        return null;
    }

    /**
     * 计算节气的具体日期
     * @param year 年份
     * @param termIndex 节气索引，从0开始（小寒）
     * @return 该节气的Calendar对象
     */
    private Calendar getSolarTermCalendar(int year, int termIndex) {
        Calendar calendar = Calendar.getInstance();
        
        // 计算世纪数
        int century = (year - 1900) / 100;
        
        // 计算偏移量（不同世纪有不同的偏移）
        double centuryValue = 0;
        if (century == 0) {
            centuryValue = 0;
        } else if (century == 1) {
            centuryValue = 6.11;
        } else if (century == 2) {
            centuryValue = 12.22;
        }
        
        // 注意：此算法在1900-2100年范围内有效
        double d = 0.2422 * (year - 1900) + TERM_INFO[termIndex] / 10000.0 - centuryValue;
        
        // 节气最终日期
        int day = (int) d;
        
        // 计算该节气的日期
        if (termIndex < 3) { // 小寒、大寒、立春
            calendar.set(year, Calendar.JANUARY, day);
        } else if (termIndex < 6) { // 雨水、惊蛰、春分
            calendar.set(year, Calendar.FEBRUARY, day);
        } else if (termIndex < 9) { // 清明、谷雨、立夏
            calendar.set(year, Calendar.MARCH, day);
        } else if (termIndex < 12) { // 小满、芒种、夏至
            calendar.set(year, Calendar.APRIL, day);
        } else if (termIndex < 15) { // 小暑、大暑、立秋
            calendar.set(year, Calendar.MAY, day);
        } else if (termIndex < 18) { // 处暑、白露、秋分
            calendar.set(year, Calendar.JUNE, day);
        } else if (termIndex < 21) { // 寒露、霜降、立冬
            calendar.set(year, Calendar.JULY, day);
        } else { // 小雪、大雪、冬至
            calendar.set(year, Calendar.AUGUST, day);
        }
        
        return calendar;
    }
} 