package com.common.reminder.utils;

import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

/**
 * 农历计算工具类
 * 支持 1900-01-31 到 2100-02-09 之间的日期转换。
 */
public final class LunarConverter {

    // 农历数据:
    // 01.前4位(16-19位): 在这一年是闰年时才有意义，它代表这年闰月的大小月，为1则闰大月(30天)，为0则闰小月(29天)。
    // 02.中间12位(4-15位): 每位代表一个月，为1则为大月(30天)，为0则为小月(29天)。
    // 03.最后4位(0-3位): 代表这一年的闰月月份，为0则不闰。
    private static final int[] LUNAR_DATA = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, //1900-1909
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, //1910-1919
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, //1920-1929
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, //1930-1939
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, //1940-1949
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0, //1950-1959
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, //1960-1969
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6, //1970-1979
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, //1980-1989
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0, //1990-1999
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, //2000-2009
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, //2010-2019
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, //2020-2029
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, //2030-2039
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, //2040-2049
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, //2050-2059
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, //2060-2069
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0, //2070-2079
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160, //2080-2089
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252, //2090-2099
            0x0d520 //2100
    };

    // 农历新年对应的公历日期
    // 前12bit: 公历年份, 中间8bit: 公历月份, 后8bit: 公历日期
    private static final int[] SPRING_FESTIVAL_DATE = {
            0x76c011f, 0x76d0213, 0x76e0208, 0x76f011d, 0x7700210, 0x7710204, 0x7720119, 0x773020d, 0x7740204, 0x7750116, //1900-1909
            0x776020a, 0x777011e, 0x7780212, 0x7790206, 0x77a011a, 0x77b020e, 0x77c0204, 0x77d0117, 0x77e020b, 0x77f0201, //1910-1919
            0x7800214, 0x7810208, 0x782011c, 0x7830210, 0x7840205, 0x7850118, 0x786020d, 0x7870202, 0x7880117, 0x789020a, //1920-1929
            0x78a011e, 0x78b0211, 0x78c0206, 0x78d011a, 0x78e020e, 0x78f0204, 0x7900118, 0x791020b, 0x792011f, 0x7930213, //1930-1939
            0x7940208, 0x795011b, 0x796020f, 0x7970205, 0x7980119, 0x799021d, 0x79a0202, 0x79b0116, 0x79c020a, 0x79d011d, //1940-1949
            0x79e0211, 0x79f0206, 0x7a0011b, 0x7a1020e, 0x7a20203, 0x7a30118, 0x7a4020c, 0x7a5011f, 0x7a60212, 0x7a70208, //1950-1959
            0x7a8011c, 0x7a9020f, 0x7aa0205, 0x7ab0119, 0x7ac020d, 0x7ad0202, 0x7ae0115, 0x7af0209, 0x7b0011e, 0x7b10211, //1960-1969
            0x7b20206, 0x7b3011a, 0x7b4020f, 0x7b50203, 0x7b60117, 0x7b7020b, 0x7b8011f, 0x7b90212, 0x7ba0207, 0x7bb011c, //1970-1979
            0x7bc0210, 0x7bd0205, 0x7be0119, 0x7bf020d, 0x7c00202, 0x7c10214, 0x7c20209, 0x7c3011d, 0x7c40211, 0x7c50206, //1980-1989
            0x7c6011b, 0x7c7020f, 0x7c80204, 0x7c90117, 0x7ca020a, 0x7cb011f, 0x7cc0213, 0x7cd0207, 0x7ce011c, 0x7cf0210, //1990-1999
            0x7d00205, 0x7d10118, 0x7d2021c, 0x7d30201, 0x7d40116, 0x7d50209, 0x7d6011d, 0x7d70212, 0x7d80207, 0x7d9011a, //2000-2009
            0x7da020e, 0x7db0203, 0x7dc0117, 0x7dd020a, 0x7de011f, 0x7df0213, 0x7e00208, 0x7e1010c, 0x7e20210, 0x7e30205, //2010-2019
            0x7e40119, 0x7e5020c, 0x7e60201, 0x7e70116, 0x7e8020a, 0x7e9011d, 0x7ea0211, 0x7eb0206, 0x7ec011a, 0x7ed020d, //2020-2029
            0x7ee0203, 0x7ef0117, 0x7f0020b, 0x7f1011f, 0x7f20213, 0x7f30208, 0x7f4011c, 0x7f5020f, 0x7f60204, 0x7f70118, //2030-2039
            0x7f8020c, 0x7f90201, 0x7fa0116, 0x7fb020a, 0x7fc011e, 0x7fd0211, 0x7fe0206, 0x7ff010a, 0x800020e, 0x8010202, //2040-2049
            0x8020117, 0x803020b, 0x8040201, 0x8050213, 0x8060208, 0x807011c, 0x808020f, 0x8090204, 0x80a0118, 0x80b020c, //2050-2059
            0x80c0202, 0x80d0115, 0x80e0209, 0x80f011d, 0x8100211, 0x8110205, 0x812011a, 0x813020e, 0x8140203, 0x8150117, //2060-2069
            0x816020b, 0x817011f, 0x8180213, 0x8190207, 0x81a011b, 0x81b020e, 0x81c0205, 0x81d0118, 0x81e020c, 0x81f0202, //2070-2079
            0x8200116, 0x8210209, 0x822011d, 0x8230211, 0x8240206, 0x825011a, 0x826021e, 0x8270203, 0x8280118, 0x829020a, //2080-2089
            0x82a011e, 0x82b0212, 0x82c0207, 0x82d011b, 0x82e020f, 0x82f0205, 0x8300109, 0x831020c, 0x8320201, 0x8330115, //2090-2099
            0x8340209 //2100
    };

    private static final List<String> TIAN_GAN = Arrays.asList("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸");
    private static final List<String> DI_ZHI = Arrays.asList("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥");
    private static final List<String> YUE_STR = Arrays.asList("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月");
    private static final List<String> TIAN_STR = Arrays.asList(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十");

    // endregion

    // region 内部类定义
    /**
     * 存储农历日期的数据结构
     */
    public static class LunarDate {
        public int year = 1900;
        public int month = 1;
        public int day = 1;
        public boolean isLeap = false; // 是否闰月

        public LunarDate() {
        }

        /**
         * @deprecated 此构造函数在处理有闰月的年份时存在歧义，因为它会默认将日期设置为闰月。
         *             请使用 {@link #LunarDate(int, int, int, boolean)} 来明确指定是否为闰月。
         *             例如，要创建2025年的正六月，应使用 {@code new LunarDate(2025, 6, 1, false)}。
         */
        @Deprecated
        public LunarDate(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
            // 这是一个有歧义的实现：如果当年有闰X月，此构造函数会默认这是闰X月。
            this.isLeap = isLeapMonth(year, month);
        }

        /**
         * 创建一个农历日期对象。
         * @param year 年份 (例如 2023)
         * @param month 月份 (1-12)
         * @param day 日 (1-30)
         * @param isLeap 是否为闰月
         */
        public LunarDate(int year, int month, int day, boolean isLeap) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.isLeap = isLeap;
        }
        
        @Override
        public String toString() {
            return String.format("%d年%s%s月%s", year, isLeap ? "闰" : "", month, day);
        }
    }

    /**
     * 存储格式化后的农历日期字符串
     */
    public static class LunarDateString {
        public String nian; // 年 如：甲子年
        public String yue;  // 月 如：正月
        public String ri;   // 日 如：初一

        @Override
        public String toString() {
            return nian + " " + yue + ri;
        }
    }

    /**
     * 内部使用，存储某年农历的详细信息
     */
    private static class LunarYearInfo {
        public int leapMonth;
        public int leapDays;
        public int[] monthDays = new int[12];
    }
    // endregion

    // region 单例模式
    private static volatile LunarConverter instance;

    private LunarConverter() {}

    public static LunarConverter getInstance() {
        if (instance == null) {
            synchronized (LunarConverter.class) {
                if (instance == null) {
                    instance = new LunarConverter();
                }
            }
        }
        return instance;
    }
    // endregion

    /**
     * 获取某一个公历年份对应的农历闰月月份。
     * @param gregorianYear 公历年份 (例如 2023)
     * @return 闰月的月份 (1-12)。如果没有闰月，则返回 0。
     */
    private static int getLeapMonth(int gregorianYear) {
        if (gregorianYear < 1900 || gregorianYear > 2100) {
            System.err.println("年份超出支持范围 (1900-2100)");
            return 0;
        }
        // LUNAR_DATA 数组从1900年开始，所以年份需要减去1900来获取索引
        int lunarInfo = LUNAR_DATA[gregorianYear - 1900];

        // 和 0xF (二进制的 0000 0000 0000 1111) 进行按位与运算，
        // 即可取出最后4位的值，这个值就是闰月的月份。
        return lunarInfo & 0xF;
    }

    /**
     * 判断一个日期是否是闰月
     * @param year 公历年份
     * @param month 农历月份
     * @return 是否是闰月
     */
    private static boolean isLeapMonth(int year, int month) {
        int leapMonth = getLeapMonth(year);
        return leapMonth > 0 && leapMonth == month;
    }

    // region 公共方法
    /**
     * 公历转农历
     * @param solarDate 公历日期 (LocalDate)
     * @return 农历日期 (LunarDate)
     */
    public static LunarDate solarToLunar(final LocalDate solarDate) {
        LunarDate lunarDate = new LunarDate();

        // 定义支持的日期范围
        LocalDate startDate = dateFromInt(SPRING_FESTIVAL_DATE[0]);
        // 2100年的农历数据只到春节，所以结束日期是2101年的春节前一天
        LocalDate endDate = dateFromInt(SPRING_FESTIVAL_DATE[SPRING_FESTIVAL_DATE.length - 1]).plusYears(1).minusDays(1);

        if (solarDate.isBefore(startDate) || solarDate.isAfter(endDate)) {
            System.err.println("日期超出支持范围 (1900-01-31 to 2100-12-31)");
            return lunarDate; // 返回默认值
        }

        int year = solarDate.getYear();
        LocalDate springDate = dateFromInt(SPRING_FESTIVAL_DATE[year - 1900]);

        int lunarDays;
        // 如果当前日期在当年春节之前，则农历年份属于上一年
        if (solarDate.isBefore(springDate)) {
            year--;
            springDate = dateFromInt(SPRING_FESTIVAL_DATE[year - 1900]);
            lunarDays = (int) (solarDate.toEpochDay() - springDate.toEpochDay());
        } else {
            lunarDays = (int) (solarDate.toEpochDay() - springDate.toEpochDay());
        }

        lunarDate.year = year;
        int lunarData = LUNAR_DATA[year - 1900];
        LunarYearInfo lyi = getLunarYearInfo(lunarData);

        int days = 0;
        for (int i = 0; i < 12; i++) {
            if (lunarDays < days + lyi.monthDays[i]) {
                lunarDate.isLeap = false;
                lunarDate.month = i + 1;
                lunarDate.day = lunarDays - days + 1;
                return lunarDate;
            }
            days += lyi.monthDays[i];

            // 检查闰月
            if (lyi.leapMonth > 0 && i + 1 == lyi.leapMonth) {
                if (lunarDays < days + lyi.leapDays) {
                    lunarDate.isLeap = true;
                    lunarDate.month = i + 1;
                    lunarDate.day = lunarDays - days + 1;
                    return lunarDate;
                }
                days += lyi.leapDays;
            }
        }
        return lunarDate;
    }

    /**
     * 公历转农历字符串
     * @param solarDate 公历日期 (LocalDate)
     * @return 农历日期字符串 (LunarDateString)
     */
    public LunarDateString solarToLunarStr(final LocalDate solarDate) {
        LunarDate ld = solarToLunar(solarDate);
        LunarDateString lds = new LunarDateString();
        lds.nian = getTianGanDiZhi(ld.year);
        lds.yue = getYue(ld.month, ld.isLeap);
        lds.ri = getTian(ld.day);
        return lds;
    }
    // endregion

    // region 私有辅助方法
    /**
     * 从整数中解析出公历日期
     * @param dateInt 编码的日期整数
     * @return LocalDate 对象
     */
    private static LocalDate dateFromInt(int dateInt) {
        int year = dateInt >> 16;
        int month = (dateInt >> 8) & 0xFF;
        int day = dateInt & 0xFF;
        return LocalDate.of(year, month, day);
    }

    /**
     * 从农历数据中解析出当年的农历信息
     * @param lunarData 当年的农历数据
     * @return LunarYearInfo 对象
     */
    private static LunarYearInfo getLunarYearInfo(int lunarData) {
        LunarYearInfo lyi = new LunarYearInfo();
        lyi.leapMonth = lunarData & 0xF;

        if (lyi.leapMonth != 0) {
            int longShort = (lunarData & 0xF0000) >> 16;
            lyi.leapDays = longShort == 1 ? 30 : 29;
        } else {
            lyi.leapDays = 0;
        }

        int mask = 0x8000;
        for (int i = 0; i < 12; i++) {
            int longShort = (lunarData & mask) >> (15 - i);
            lyi.monthDays[i] = longShort == 1 ? 30 : 29;
            mask >>= 1;
        }
        return lyi;
    }

    /**
     * 获取年份的天干地支表示
     * @param year 公历年份
     * @return 天干地支字符串
     */
    private String getTianGanDiZhi(int year) {
        // 1984年(甲子年)是60年周期的开始
        int offset = year - 1984;
        int ganIndex = offset % 10;
        int zhiIndex = offset % 12;

        if (ganIndex < 0) ganIndex += 10;
        if (zhiIndex < 0) zhiIndex += 12;

        return TIAN_GAN.get(ganIndex) + DI_ZHI.get(zhiIndex) + "年";
    }

    /**
     * 获取农历月份的中文表示
     * @param month 农历月份
     * @param isLeap 是否为闰月
     * @return 月份字符串
     */
    private String getYue(int month, boolean isLeap) {
        if (month < 1 || month > 12) {
            return "";
        }
        if (isLeap) {
            return "闰" + YUE_STR.get(month - 1);
        }
        return YUE_STR.get(month - 1);
    }

    /**
     * 获取农历日期的中文表示
     * @param day 农历日期
     * @return 日期字符串
     */
    private String getTian(int day) {
        if (day < 1 || day > 30) {
            return "";
        }
        return TIAN_STR.get(day - 1);
    }

    /**
     * 农历转公历
     * @param lunarDate 农历日期对象 (LunarDate)
     * @return 公历日期 (LocalDate)，如果转换失败或输入的农历日期无效则返回 null
     */
    public static LocalDate lunarToSolar(final LunarDate lunarDate) {
        // 1. 输入校验
        if (lunarDate == null) {
            System.err.println("错误：输入的农历日期对象不能为 null。");
            return null;
        }

        int year = lunarDate.year;
        if (year < 1900 || year > 2100) {
            System.err.println("年份超出支持范围 (1900-2100)");
            return null;
        }

        // 2. 获取当年的农历信息
        int lunarData = LUNAR_DATA[year - 1900];
        LunarYearInfo lyi = getLunarYearInfo(lunarData);

        // 3. 详细校验输入的农历日期是否合法
        if (lunarDate.isLeap) {
            // 如果是闰月，但当年没有闰月，或者闰的月份不对
            if (lyi.leapMonth == 0 || lyi.leapMonth != lunarDate.month) {
                System.err.printf("错误：%d年没有闰%s%n", year, YUE_STR.get(lunarDate.month - 1));
                return null;
            }
            // 闰月的日期超出范围
            if (lunarDate.day < 1 || lunarDate.day > lyi.leapDays) {
                System.err.printf("错误：%d年闰%s只有 %d天%n", year, YUE_STR.get(lunarDate.month - 1), lyi.leapDays);
                return null;
            }
        } else {
            // 月份或日期超出范围
            if (lunarDate.month < 1 || lunarDate.month > 12) {
                System.err.println("错误：月份必须在 1-12 之间");
                return null;
            }
            if (lunarDate.day < 1 || lunarDate.day > lyi.monthDays[lunarDate.month - 1]) {
                System.err.printf("错误：%d年%s只有 %d天%n", year, YUE_STR.get(lunarDate.month - 1), lyi.monthDays[lunarDate.month - 1]);
                return null;
            }
        }


        // 4. 计算偏移天数
        //    a. 获取当年正月初一对应的公历日期作为基准
        LocalDate springFestivalDate = dateFromInt(SPRING_FESTIVAL_DATE[year - 1900]);

        //    b. 计算从正月初一到目标日期的总天数
        int offsetDays = 0;

        //    c. 累加所有在目标月份之前的整月天数
        for (int i = 1; i < lunarDate.month; i++) {
            offsetDays += lyi.monthDays[i - 1]; // 加上常规月份的天数
            // 如果在目标月之前有一个闰月，也要加上它的天数
            if (lyi.leapMonth > 0 && i == lyi.leapMonth) {
                offsetDays += lyi.leapDays;
            }
        }

        //    d. 如果目标是闰月，需要额外加上对应常规月份的天数
        //       例如，计算闰五月初一，需要先累加完常规五月的天数
        if (lunarDate.isLeap) {
            offsetDays += lyi.monthDays[lunarDate.month - 1];
        }

        //    e. 最后加上目标日期的天数 (减1是因为我们从初一开始算)
        offsetDays += lunarDate.day - 1;

        // 5. 将偏移天数加到基准日期上，得到最终的公历日期
        return springFestivalDate.plusDays(offsetDays);
    }


    public static void main(String[] args) {
        // 使用新的构造函数，明确指定非闰月
        System.out.println("--- 使用新构造函数创建2025年正六月和闰六月 ---");
        LunarDate regularJune2025 = new LunarDate(2025, 6, 1, false);
        System.out.println("农历 " + regularJune2025 + " 对应的公历是: " + LunarConverter.lunarToSolar(regularJune2025));

        // 使用新的构造函数，明确指定是闰月
        LunarDate leapJune2025 = new LunarDate(2025, 6, 1, true);
        System.out.println("农历 " + leapJune2025 + " 对应的公历是: " + LunarConverter.lunarToSolar(leapJune2025));
        System.out.println("------------------------------------");


        // 之前的代码示例
        LocalDate now = LocalDate.now();
        System.out.println("当前公历日期: " + now);
        System.out.println("转换后的农历日期: " + LunarConverter.solarToLunar(now));

        // 使用被废弃的构造函数（会产生警告）
        @SuppressWarnings("deprecation")
        LunarDate ambiguousLunarDate = new LunarDate(2025, 6, 1);
        System.out.println("使用旧构造函数创建的农历日期 (存在歧义): " + ambiguousLunarDate);
        System.out.println("它对应的公历是: " + LunarConverter.lunarToSolar(ambiguousLunarDate));
    }
}
