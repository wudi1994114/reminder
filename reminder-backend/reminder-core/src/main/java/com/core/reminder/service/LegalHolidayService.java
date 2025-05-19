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

@Service
public class LegalHolidayService {

    @Autowired
    private LegalHolidayRepository legalHolidayRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取指定年份范围内的法定节假日
     */
    public List<LegalHoliday> getHolidaysByYearRange(Integer startYear, Integer endYear) {
        List<LegalHoliday> holidays = new ArrayList<>();
        
        // 获取数据库中已有的节假日
        holidays.addAll(legalHolidayRepository.findByYearRange(startYear, endYear));
        
        // 检查并获取缺失的年份数据
        for (int year = startYear; year <= endYear; year++) {
            if (!legalHolidayRepository.existsByYear(year)) {
                try {
                    fetchAndSaveHolidaysForYear(year);
                    holidays.addAll(legalHolidayRepository.findByYear(year));
                } catch (Exception e) {
                    // 如果获取失败，继续处理下一年
                    e.printStackTrace();
                }
            }
        }
        
        return holidays;
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
            
            List<LegalHoliday> holidays = new ArrayList<>();
            
            Iterator<String> dateFields = holidayNode.fieldNames();
            while (dateFields.hasNext()) {
                String date = dateFields.next();
                JsonNode holidayInfo = holidayNode.get(date);
                
                LegalHoliday holiday = new LegalHoliday();
                holiday.setYear(year);
                holiday.setMonth(Integer.parseInt(date.split("-")[0]));
                holiday.setDay(Integer.parseInt(date.split("-")[1]));
                holiday.setHoliday(holidayInfo.get("holiday").asBoolean());
                holiday.setName(holidayInfo.get("name").asText());
                
                holidays.add(holiday);
            }
            
            legalHolidayRepository.saveAll(holidays);
            
        } catch (Exception e) {
            throw new RuntimeException("获取" + year + "年节假日数据失败", e);
        }
    }
} 