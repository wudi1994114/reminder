package com.core.reminder.service;

import com.core.reminder.model.LegalHoliday;
import com.core.reminder.repository.LegalHolidayRepository;
import com.core.reminder.utils.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LegalHolidayService {

    @Autowired
    private LegalHolidayRepository legalHolidayRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 内存缓存：年份 -> 该年份的所有节假日列表
    private final ConcurrentHashMap<Integer, List<LegalHoliday>> holidayCache = new ConcurrentHashMap<>();
    
    // 标记哪些年份已经完全加载到内存中
    private final ConcurrentHashMap<Integer, Boolean> loadedYears = new ConcurrentHashMap<>();

    /**
     * 获取指定年份范围内的法定节假日
     */
    public List<LegalHoliday> getHolidaysByYearRange(Integer startYear, Integer endYear) {
        List<LegalHoliday> holidays = new ArrayList<>();
        
        // 优先从内存缓存中获取数据
        for (int year = startYear; year <= endYear; year++) {
            List<LegalHoliday> yearHolidays = getHolidaysFromCacheOrLoad(year);
            holidays.addAll(yearHolidays);
        }
        
        return holidays;
    }
    
    /**
     * 从缓存中获取节假日数据，如果缓存中没有则从数据库或API加载
     */
    private List<LegalHoliday> getHolidaysFromCacheOrLoad(Integer year) {
        // 如果内存中已有该年份的数据，直接返回
        if (loadedYears.containsKey(year) && holidayCache.containsKey(year)) {
            return new ArrayList<>(holidayCache.get(year));
        }
        
        // 从数据库加载该年份的数据
        List<LegalHoliday> dbHolidays = legalHolidayRepository.findByYear(year);
        
        if (!dbHolidays.isEmpty()) {
            // 数据库中有数据，加载到内存缓存
            holidayCache.put(year, new ArrayList<>(dbHolidays));
            loadedYears.put(year, true);
            return new ArrayList<>(dbHolidays);
        }
        
        // 数据库中没有数据，从API获取
        try {
            fetchAndSaveHolidaysForYear(year);
            // 重新从数据库获取刚保存的数据
            List<LegalHoliday> newHolidays = legalHolidayRepository.findByYear(year);
            if (!newHolidays.isEmpty()) {
                holidayCache.put(year, new ArrayList<>(newHolidays));
                loadedYears.put(year, true);
                return new ArrayList<>(newHolidays);
            }
        } catch (Exception e) {
            System.err.println("获取" + year + "年节假日数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 如果都失败了，返回空列表，但标记为已尝试加载
        holidayCache.put(year, new ArrayList<>());
        loadedYears.put(year, true);
        return new ArrayList<>();
    }

    /**
     * 从外部API获取并保存指定年份的节假日数据
     */
    @Transactional
    public void fetchAndSaveHolidaysForYear(Integer year) {
        try {
            String url = "https://timor.tech/api/holiday/year/" + year;
            String response = HttpUtil.get(url, String.class);
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode holidayNode = root.get("holiday");
            
            List<LegalHoliday> holidaysToSave = new ArrayList<>();
            
            if (holidayNode == null || holidayNode.isNull()) {
                System.err.println("No 'holiday' data found in API response for year: " + year);
                return;
            }

            Iterator<String> dateFields = holidayNode.fieldNames();
            while (dateFields.hasNext()) {
                String date = dateFields.next();
                JsonNode holidayInfo = holidayNode.get(date);
                
                if (holidayInfo == null || holidayInfo.isNull() || !holidayInfo.has("holiday") || !holidayInfo.has("name")) {
                    System.err.println("Skipping invalid holiday entry for date: " + date + " in year: " + year);
                    continue;
                }

                LegalHoliday holiday = new LegalHoliday();
                holiday.setYear(year);
                // Date format from API is MM-DD
                String[] dateParts = date.split("-");
                if (dateParts.length != 2) {
                    System.err.println("Invalid date format in API response: " + date + " for year: " + year);
                    continue;
                }
                try {
                    holiday.setMonth(Integer.parseInt(dateParts[0]));
                    holiday.setDay(Integer.parseInt(dateParts[1]));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing month/day from API response: " + date + " for year: " + year);
                    continue;
                }
                
                holiday.setHoliday(holidayInfo.get("holiday").asBoolean());
                holiday.setName(holidayInfo.get("name").asText());
                
                holidaysToSave.add(holiday);
            }
            
            if (!holidaysToSave.isEmpty()) {
                legalHolidayRepository.saveAll(holidaysToSave);
                // 保存成功后，立即更新内存缓存
                holidayCache.put(year, new ArrayList<>(holidaysToSave));
                loadedYears.put(year, true);
            }
            
        } catch (Exception e) {
            System.err.println("获取" + year + "年节假日数据失败. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除指定年份的缓存
     */
    public void clearCacheForYear(Integer year) {
        holidayCache.remove(year);
        loadedYears.remove(year);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        holidayCache.clear();
        loadedYears.clear();
    }
    
    /**
     * 预加载指定年份范围的节假日数据到内存
     */
    public void preloadHolidays(Integer startYear, Integer endYear) {
        for (int year = startYear; year <= endYear; year++) {
            getHolidaysFromCacheOrLoad(year);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("已缓存年份数量: %d, 缓存的节假日总数: %d", 
            loadedYears.size(), 
            holidayCache.values().stream().mapToInt(List::size).sum());
    }
} 