import { request } from './request.js';
import { handleApiError } from '@/utils/helpers.js';

// 日历相关API
export const calendarApi = {
    // 获取指定年份范围的法定节假日
    getHolidaysByYearRange: (startYear, endYear) => request({
        url: `/holidays`,
        method: 'GET',
        data: { startYear, endYear }
    }).catch(handleApiError),

    // 获取日历数据（包括节假日、调休日等）
    getCalendarData: (startYear, endYear, apiType = 'all') => {
        // 参数校验和处理
        startYear = startYear || new Date().getFullYear();
        endYear = endYear || (startYear + 1);
        
        console.log(`请求日历数据: ${startYear}-${endYear}, 类型: ${apiType}`);
        
        let url = `/holidays?startYear=${startYear}&endYear=${endYear}`;
        
        // 如果提供了apiType参数且不为空，添加到URL中
        if (apiType && apiType !== 'all') {
            url += `&type=${apiType}`;
        }
        
        return request({
            url,
            method: 'GET'
        }).catch(handleApiError);
    },

    // 获取指定月份的事件数据
    getMonthEvents: (year, month) => {
        return request({
            url: `/calendar/events?year=${year}&month=${month}`,
            method: 'GET'
        }).catch(handleApiError);
    },

    // 获取农历信息
    getLunarInfo: (date) => {
        return request({
            url: `/calendar/lunar?date=${date}`,
            method: 'GET'
        }).catch(handleApiError);
    }
}; 