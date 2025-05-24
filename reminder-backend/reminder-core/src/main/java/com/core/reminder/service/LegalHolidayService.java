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
                    // Make sure to add newly fetched holidays to the list
                    // Filter to avoid duplicates if any were already present from the initial fetch (though existsByYear should prevent this)
                    List<LegalHoliday> newlyFetched = legalHolidayRepository.findByYear(year);
                    newlyFetched.forEach(nf -> {
                        if (holidays.stream().noneMatch(h -> h.getYear().equals(nf.getYear()) && h.getMonth().equals(nf.getMonth()) && h.getDay().equals(nf.getDay()))) {
                            holidays.add(nf);
                        }
                    });
                } catch (Exception e) {
                    // 如果获取失败，继续处理下一年
                    e.printStackTrace();
                }
            }
            // Removed solar term processing logic for the year
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
            
            List<LegalHoliday> holidaysToSave = new ArrayList<>();
            
            if (holidayNode == null || holidayNode.isNull()) {
                System.err.println("No 'holiday' data found in API response for year: " + year);
                return; // Or throw a custom exception
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
                
                // Removed solar term setting logic
                
                holidaysToSave.add(holiday);
            }
            
            if (!holidaysToSave.isEmpty()) {
                legalHolidayRepository.saveAll(holidaysToSave);
            }
            
        } catch (Exception e) {
            // Log the full error for better diagnostics
            System.err.println("获取" + year + "年节假日数据失败. Error: " + e.getMessage());
            e.printStackTrace(); // It's good to have the stack trace for debugging
            // Optionally rethrow as a custom unchecked exception if you want to signal failure more strongly
            // throw new RuntimeException("获取" + year + "年节假日数据失败", e);
        }
    }
    
    // Removed isSolarTerm method
} 