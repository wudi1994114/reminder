<template>
	<view class="calendar-content" :style="{background:bgColor == '' || bgColor == 'none' ? '' : bgColor }">
		<view class="calendar-header">
			<view class="last-month" @tap="lastMonth">
				<image :src="lastIcon" mode="" v-if="lastIcon != ''"></image>
				<text v-else :style="{color:defColor}">{{lastText}}</text>
			</view>
			<view class="header-time" :style="{color:defColor}">{{time.year}}年{{time.month}}月</view>
			<view class="next-month" @tap="nextMonth">
				<image :src="nextIcon" mode="" v-if="nextIcon != ''"></image>
				<text v-else :style="{color:defColor}">{{nextText}}</text>
			</view>
		</view>
		<view class="calendar-week">
			<view class="week-day" v-for="(item,index) in week" :key="index" :style="{color: index === 0 || index === 6 ? restDayColor : defColor}">{{item}}</view>
		</view>
		<view class="calendar-board">
			<view class="board-row" v-for="(item,index) in date" :key="index">
				<view 
				class="row-col" 
				v-for="(itm,idx) in item" 
				:key="idx" 
				:class="itm.cssClass"
				:style="{
					background: itm.bgColor,
					border: itm.border
				}"
				@tap="dateTap(itm,index,idx)">
				
					<!-- 日期数字区域 -->
					<view class="date-number">
						<text :style="{color: itm.textColor}">{{itm.date}}</text>
					</view>
					
					<!-- 节日/节气/自定义值区域 -->
					<view class="date-info">
						<text 
							v-if="itm.displayText && itm.type == 'cur'"
							:class="{ 
								'festival': itm.holiday, 
								'solar-term': itm.solarTerm && !itm.holiday, 
								'val': !itm.holiday && !itm.solarTerm
							}"
							:style="{color: itm.displayTextColor}"
						>
							{{itm.displayText}}
						</text>
					</view>
					
					<!-- 红点标记 -->
					<text 
						class="dot" 
						v-if="itm.dot && itm.type == 'cur'" 
						:style="{
							background: '#ff4500',
							right: (itm.showRestMark || itm.showWorkMark) ? '38upx' : '20upx'
						}"
					></text>
					
					<!-- 休息日标记 -->
					<text 
						class="rest-mark" 
						v-if="itm.showRestMark"
					>
						{{itm.restMarkText}}
					</text>
					
					<!-- 补班日标记 -->
					<text 
						class="work-mark" 
						v-if="itm.showWorkMark"
						:style="{ color: '#FF0000' }"
					>
						{{itm.workMarkText}}
					</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	// 导入 lunar-typescript 库，用于计算节气
	import { Solar, Lunar, LunarUtil } from 'lunar-typescript';
	// 导入API
	import { getCalendarData } from '../../services/api.js';
	
	export default {
		name:'v-calendar',
		props:{
			// 背景颜色
			bgColor:{
				type:String,
				default:''
			},
			// 选中日期颜色
			selColor:{
				type:String,
				default:'#FF6B35' // 温暖橘色
			},
			// 默认文字颜色
			defColor:{
				type:String,
				default:'#2C2C2E' // 深灰色
			},
			// 上个月文字
			lastText:{
				type:String,
				default:'‹' // 更优雅的箭头
			},
			// 下个月文字
			nextText:{
				type:String,
				default:'›' // 更优雅的箭头
			},
			// 上个月图标
			lastIcon:{
				type:String,
				default:''
			},
			// 下个月图标
			nextIcon:{
				type:String,
				default:''
			},
			// 是否显示圆点
			showDot:{
				type:Boolean,
				default:true
			},
			// 是否显示value值
			showText:{
				type:Boolean,
				default:true
			},
			// 是否显示工作日和休息日差异
			showWorkRestDay:{
				type:Boolean,
				default:true
			},
			// 工作日背景色
			workDayBgColor:{
				type:String,
				default:'transparent' // 保持透明，简约风格
			},
			// 休息日背景色
			restDayBgColor:{
				type:String,
				default:'#FFF4E6' // 浅橘色背景
			},
			// 休息日文字颜色
			restDayColor:{
				type:String,
				default:'#FF6B35' // 橘色
			},
			// 休息日显示标记
			showRestMark:{
				type:Boolean,
				default:true // 默认显示标记
			},
			// 休息日标记文本
			restMarkText:{
				type:String,
				default:'休'
			},
			// 法定假日数据 [{date:'2023-1-1',isRest:true}] isRest为true表示为放假，false表示需要补班
			legalHolidays:{
				type:Array,
				default:()=>{
					return []
				}
			},
			// 法定假日放假标记文本
			legalHolidayText:{
				type:String,
				default:'假'
			},
			// 法定假日补班标记文本
			legalWorkdayText:{
				type:String,
				default:'班'
			},
			// 法定假日背景色
			legalHolidayBgColor:{
				type:String,
				default:'#FFE5CC' // 浅橘色背景
			},
			// 法定假日文字颜色
			legalHolidayColor:{
				type:String,
				default:'#E55100' // 深橘色
			},
			// 法定补班日背景色
			legalWorkdayBgColor:{
				type:String,
				default:'#F3E5AB' // 浅黄色背景
			},
			// 法定补班日文字颜色
			legalWorkdayColor:{
				type:String,
				default:'#FF8F00' // 琥珀色
			},
			// 节日数据 [{date:'2023-1-1',name:'元旦'}]
			holidays:{
				type:Array,
				default:()=>{
					return []
				}
			},
			// 节日文字颜色
			holidayColor:{
				type:String,
				default:'#E55100' // 深橘色
			},
			// 节气数据 [{date:'2023-2-4',name:'立春'}]
			solarTerms:{
				type:Array,
				default:()=>{
					return []
				}
			},
			// 节气文字颜色
			solarTermColor:{
				type:String,
				default:'#8D6E63' // 棕色
			},
			// 默认时间{year:2020,month:5}
			defaultTime:{
				type:Object,
				default:()=>{
					return {}
				}
			},
			// 额外数据 [{date:'2020-6-3',value:'签到',dot:true,active:true}]
			// date 日期格式YYYY-M-D 或 YYYY/M/D
			// value 显示值
			// dot 是否显示圆点
			// active 是否选中
			extraData:{
				type:Array,
				default:()=>{
					return []
				}
			},
			// 是否自动计算节气（使用lunar-typescript库）
			autoCalculateSolarTerms: {
				type: Boolean,
				default: true
			},
			// 是否自动从后端获取节日和调休数据
			autoFetchHolidayData: {
				type: Boolean,
				default: true
			},
			
			// API数据类型，用于区分获取不同类型的数据
			// 可选值: 'holiday'(节假日), 'event'(普通事件), 'all'(全部)
			apiType: {
				type: String,
				default: 'all'
			},
			
			// 统一的日期类型样式配置对象
			// 可以直接配置不同类型日期的样式
			dateStyles: {
				type: Object,
				default: () => ({})
			},
			
			// 统一的文字颜色方案
			// 允许一次性设置所有日期类型的文字颜色
			textColorScheme: {
				type: Object,
				default: () => ({
					// 日期数字颜色
					date: {
						workday: '', // 工作日日期数字颜色
						weekend: '', // 周末日期数字颜色
						holiday: '', // 节假日日期数字颜色
						adjustWorkday: '' // 调休日日期数字颜色
					},
					// 事件/节日文字颜色
					event: {
						workday: '', // 工作日事件文字颜色
						weekend: '', // 周末事件文字颜色
						holiday: '', // 节假日事件文字颜色
						adjustWorkday: '' // 调休日事件文字颜色
					},
					// 节气文字颜色
					solarTerm: {
						workday: '', // 工作日节气文字颜色
						weekend: '', // 周末节气文字颜色
						holiday: '', // 节假日节气文字颜色
						adjustWorkday: '' // 调休日节气文字颜色
					}
				})
			}
		},
		data() {
			return {
				week:['日','一','二','三','四','五','六'],
				time:{
					year:new Date().getFullYear(),
					month:new Date().getMonth()+1
				},
				date:[],
				selDate:'',
				debugLog: [], // 添加调试日志数组
				extraDataMap: new Map(), // 用于存储 extraData 的 Map 结构
				holidaysMap: new Map(), // 用于存储节日数据的 Map 结构
				solarTermsMap: new Map(), // 用于存储节气数据的 Map 结构
				legalHolidaysMap: new Map(), // 用于存储法定假日数据的 Map 结构
				calculatedSolarTermsMap: new Map(), // 用于存储计算得到的节气数据
				localHolidays: [], // 本地存储的节日数据
				localLegalHolidays: [], // 本地存储的法定假日数据
				
				// 动态颜色配置（不修改props，而是使用data变量）
				dynamicColors: {
					selColor: '',
					defColor: '',
					restDayBgColor: '',
					restDayColor: '',
					legalHolidayBgColor: '',
					legalHolidayColor: '',
					legalWorkdayBgColor: '',
					legalWorkdayColor: '',
					holidayColor: '',
					solarTermColor: '',
					bgColor: '',
					headerBgColor: '',
					headerBorderColor: '',
					weekTextColor: '',
					buttonBgColor: '',
					buttonActiveBgColor: '',
					dotColor: ''
				},
				
				// 12个月的固定色系配置
				monthColorSchemes: {
					1: { // 一月 - 冰雪蓝
						name: '冰雪蓝',
						primary: '#4A90E2',
						background: '#F8FBFF',
						
						header: '#EBF4FF',
						border: '#D1E7FF',
						button: '#E3F2FD',
						buttonActive: '#BBDEFB',
						weekText: '#5C7CFA',
						restDay: '#E3F2FD',
						restDayText: '#4A90E2',
						holiday: '#D1E7FF',
						holidayText: '#1976D2',
						workday: '#FFF3E0',
						workdayText: '#FF8F00',
						dot: '#1976D2'
					},
					2: { // 二月 - 浪漫粉
						name: '浪漫粉',
						primary: '#E91E63',
						background: '#FFF8FA',
						header: '#FCE4EC',
						border: '#F8BBD9',
						button: '#F3E5F5',
						buttonActive: '#E1BEE7',
						weekText: '#AD1457',
						restDay: '#F3E5F5',
						restDayText: '#E91E63',
						holiday: '#F8BBD9',
						holidayText: '#C2185B',
						workday: '#FFF3E0',
						workdayText: '#FF8F00',
						dot: '#C2185B'
					},
					3: { // 三月 - 春绿色
						name: '春绿色',
						primary: '#4CAF50',
						background: '#F9FFF9',
						header: '#E8F5E8',
						border: '#C8E6C9',
						button: '#E8F5E8',
						buttonActive: '#C8E6C9',
						weekText: '#2E7D32',
						restDay: '#E8F5E8',
						restDayText: '#4CAF50',
						holiday: '#C8E6C9',
						holidayText: '#388E3C',
						workday: '#FFF3E0',
						workdayText: '#FF8F00',
						dot: '#388E3C'
					},
					4: { // 四月 - 樱花粉
						name: '樱花粉',
						primary: '#FF9800',
						background: '#FFFBF5',
						header: '#FFF3E0',
						border: '#FFCC80',
						button: '#FFF3E0',
						buttonActive: '#FFCC80',
						weekText: '#E65100',
						restDay: '#FFF3E0',
						restDayText: '#FF9800',
						holiday: '#FFCC80',
						holidayText: '#F57C00',
						workday: '#E8F5E8',
						workdayText: '#4CAF50',
						dot: '#F57C00'
					},
					5: { // 五月 - 薄荷绿
						name: '薄荷绿',
						primary: '#00BCD4',
						background: '#F4FDFF',
						header: '#E0F7FA',
						border: '#B2EBF2',
						button: '#E0F7FA',
						buttonActive: '#B2EBF2',
						weekText: '#00695C',
						restDay: '#E0F7FA',
						restDayText: '#00BCD4',
						holiday: '#B2EBF2',
						holidayText: '#0097A7',
						workday: '#FFF3E0',
						workdayText: '#FF8F00',
						dot: '#0097A7'
					},
					6: { // 六月 - 阳光黄
						name: '阳光黄',
						primary: '#FFC107',
						background: '#FFFEF7',
						header: '#FFF8E1',
						border: '#FFE082',
						button: '#FFF8E1',
						buttonActive: '#FFE082',
						weekText: '#F57F17',
						restDay: '#FFF8E1',
						restDayText: '#FFC107',
						holiday: '#FFE082',
						holidayText: '#FF8F00',
						workday: '#E0F7FA',
						workdayText: '#00BCD4',
						dot: '#FF8F00'
					},
					7: { // 七月 - 热情红
						name: '热情红',
						primary: '#F44336',
						background: '#FFFAFA',
						header: '#FFEBEE',
						border: '#FFCDD2',
						button: '#FFEBEE',
						buttonActive: '#FFCDD2',
						weekText: '#B71C1C',
						restDay: '#FFEBEE',
						restDayText: '#F44336',
						holiday: '#FFCDD2',
						holidayText: '#D32F2F',
						workday: '#E0F7FA',
						workdayText: '#00BCD4',
						dot: '#D32F2F'
					},
					8: { // 八月 - 紫罗兰
						name: '紫罗兰',
						primary: '#9C27B0',
						background: '#FEFAFF',
						header: '#F3E5F5',
						border: '#CE93D8',
						button: '#F3E5F5',
						buttonActive: '#CE93D8',
						weekText: '#4A148C',
						restDay: '#F3E5F5',
						restDayText: '#9C27B0',
						holiday: '#CE93D8',
						holidayText: '#7B1FA2',
						workday: '#FFF8E1',
						workdayText: '#FFC107',
						dot: '#7B1FA2'
					},
					9: { // 九月 - 深海蓝
						name: '深海蓝',
						primary: '#3F51B5',
						background: '#F8F9FF',
						header: '#E8EAF6',
						border: '#C5CAE9',
						button: '#E8EAF6',
						buttonActive: '#C5CAE9',
						weekText: '#1A237E',
						restDay: '#E8EAF6',
						restDayText: '#3F51B5',
						holiday: '#C5CAE9',
						holidayText: '#303F9F',
						workday: '#FFF8E1',
						workdayText: '#FFC107',
						dot: '#303F9F'
					},
					10: { // 十月 - 金秋橙
						name: '金秋橙',
						primary: '#FF6B35',
						background: '#FEFEFE',
						header: '#FFF8F0',
						border: '#FFE5CC',
						button: '#FFF4E6',
						buttonActive: '#FFE5CC',
						weekText: '#8D6E63',
						restDay: '#FFF4E6',
						restDayText: '#FF6B35',
						holiday: '#FFE5CC',
						holidayText: '#E55100',
						workday: '#F3E5AB',
						workdayText: '#FF8F00',
						dot: '#E55100'
					},
					11: { // 十一月 - 暖棕色
						name: '暖棕色',
						primary: '#795548',
						background: '#FAFAFA',
						header: '#EFEBE9',
						border: '#D7CCC8',
						button: '#EFEBE9',
						buttonActive: '#D7CCC8',
						weekText: '#3E2723',
						restDay: '#EFEBE9',
						restDayText: '#795548',
						holiday: '#D7CCC8',
						holidayText: '#5D4037',
						workday: '#E8F5E8',
						workdayText: '#4CAF50',
						dot: '#5D4037'
					},
					12: { // 十二月 - 圣诞红绿
						name: '圣诞红绿',
						primary: '#D32F2F',
						background: '#FFFAFA',
						header: '#FFEBEE',
						border: '#FFCDD2',
						button: '#E8F5E8',
						buttonActive: '#C8E6C9',
						weekText: '#1B5E20',
						restDay: '#FFEBEE',
						restDayText: '#D32F2F',
						holiday: '#FFCDD2',
						holidayText: '#C62828',
						workday: '#E8F5E8',
						workdayText: '#2E7D32',
						dot: '#C62828'
					}
				},
				
				// 统一的日期类型样式配置
				dateTypeStyles: {
					// 普通工作日样式
					workday: {
						bgColor: '', // 将使用 props 中的 workDayBgColor
						textColor: 'black', // 将使用 props 中的 defColor
						eventTextColor: 'black', // 事件/节日文字颜色
						solarTermColor: '#1C5480', // 节气文字颜色
						cssClass: 'work-day',
						markText: '工',
						markShow: false
					},
					// 普通休息日（周末）样式
					weekend: {
						bgColor: 'transparent', // 将使用 props 中的 restDayBgColor
						textColor: '#ff4500', // 将使用 props 中的 restDayColor
						eventTextColor: '#ff4500', // 事件/节日文字颜色
						solarTermColor: '#1C5480', // 节气文字颜色
						cssClass: 'rest-day',
						markText: '休',
						markShow: false // 将使用 props 中的 showRestMark
					},
					// 法定节假日样式
					holiday: {
						bgColor: 'transparent', // 将使用 props 中的 legalHolidayBgColor
						textColor: '#ff4500', // 将使用 props 中的 legalHolidayColor
						eventTextColor: '#ff4500', // 节日文字颜色
						solarTermColor: '#1C5480', // 节气文字颜色
						cssClass: 'legal-holiday',
						markText: '假', // 将使用 props 中的 legalHolidayText
						markShow: false // 将使用 props 中的 showRestMark
					},
					// 调休工作日样式
					adjustWorkday: {
						bgColor: '', // 将使用 props 中的 legalWorkdayBgColor
						textColor: '', // 将使用 props 中的 legalWorkdayColor
						eventTextColor: '#008000', // 事件文字颜色
						solarTermColor: '#1C5480', // 节气文字颜色
						cssClass: 'legal-workday',
						markText: '班', // 将使用 props 中的 legalWorkdayText
						markShow: false // 将使用 props 中的 showRestMark
					},
					// 选中日期样式
					active: {
						bgColor: '', // 将使用 props 中的 selColor
						textColor: '#fff',
						eventTextColor: '#fff',
						solarTermColor: '#fff',
						cssClass: 'active-text',
						markShow: false
					}
				},
				stylesInitialized: false,
				currentColorScheme: {} // 当前月份的色系配置
			};
		},
		created() {
			console.log('v-calendar 组件创建, extraData:', this.extraData?.length || 0);
			this.time = this.defaultTime == {} ? this.time : this.defaultTime;
			
			// 初始化动态颜色配置的默认值
			this.dynamicColors.selColor = this.selColor;
			this.dynamicColors.defColor = this.defColor;
			this.dynamicColors.restDayBgColor = this.restDayBgColor;
			this.dynamicColors.restDayColor = this.restDayColor;
			this.dynamicColors.legalHolidayBgColor = this.legalHolidayBgColor;
			this.dynamicColors.legalHolidayColor = this.legalHolidayColor;
			this.dynamicColors.legalWorkdayBgColor = this.legalWorkdayBgColor;
			this.dynamicColors.legalWorkdayColor = this.legalWorkdayColor;
			this.dynamicColors.holidayColor = this.holidayColor;
			this.dynamicColors.solarTermColor = this.solarTermColor;
			this.dynamicColors.bgColor = this.bgColor;
			
			// 应用当前月份的色系
			this.applyMonthColorScheme();
			
			// 初始化统一样式配置
			this.initDateTypeStyles();
			this.updateExtraDataMap(); // 初始化时更新 Map
			this.updateHolidaysMap(); // 初始化节日 Map
			this.updateSolarTermsMap(); // 初始化节气 Map
			this.updateLegalHolidaysMap(); // 初始化法定假日 Map
			
			// 如果开启了自动获取节日和调休数据，则从后端获取数据
			if (this.autoFetchHolidayData) {
				this.fetchHolidayData();
			}
			
			this.createCalendar();
			
			// 设置默认选中当前日期
			const today = new Date();
			const currentYear = today.getFullYear();
			const currentMonth = today.getMonth() + 1;
			const currentDay = today.getDate();
			this.selDate = `${currentYear}-${currentMonth}-${currentDay}`;
			
			// 触发日期选中事件，通知父组件
			this.$nextTick(() => {
				this.$emit('calendarTap', this.selDate);
			});
		},
		watch: {
			// 监听样式相关props变化，更新统一样式配置
			workDayBgColor() { this.updateDateTypeStyles(); },
			restDayBgColor() { this.updateDateTypeStyles(); },
			defColor() { this.updateDateTypeStyles(); },
			restDayColor() { this.updateDateTypeStyles(); },
			legalHolidayBgColor() { this.updateDateTypeStyles(); },
			legalHolidayColor() { this.updateDateTypeStyles(); },
			legalWorkdayBgColor() { this.updateDateTypeStyles(); },
			legalWorkdayColor() { this.updateDateTypeStyles(); },
			selColor() { this.updateDateTypeStyles(); },
			showRestMark() { this.updateDateTypeStyles(); },
			legalHolidayText() { this.updateDateTypeStyles(); },
			legalWorkdayText() { this.updateDateTypeStyles(); },
			restMarkText() { this.updateDateTypeStyles(); },
			
			// 监听统一样式配置的变化
			dateStyles: {
				handler() {
					this.updateDateTypeStyles();
				},
				deep: true
			},
			
			// 监听文字颜色方案的变化
			textColorScheme: {
				handler() {
					this.updateDateTypeStyles();
				},
				deep: true
			},
			
			// 监听 autoCalculateSolarTerms 变化
			autoCalculateSolarTerms: {
				handler(newVal) {
					console.log('autoCalculateSolarTerms 已更新:', newVal);
					this.updateSolarTermsMap();
					this.createCalendar();
				}
			},
			// 监听 extraData 的变化，当有新数据时重新渲染日历
			extraData: {
				handler(newVal) {
					console.log('extraData 已更新，数据长度:', newVal?.length || 0);
					this.updateExtraDataMap(); // extraData 变化时更新 Map
					if (newVal && newVal.length > 0) {
						this.createCalendar(); // 重新渲染日历
					}
				},
				deep: true
			},
			// 监听节日数据变化
			holidays: {
				handler(newVal) {
					console.log('holidays 已更新，数据长度:', newVal?.length || 0);
					this.updateHolidaysMap();
					this.createCalendar();
				},
				deep: true
			},
			// 监听节气数据变化
			solarTerms: {
				handler(newVal) {
					console.log('solarTerms 已更新，数据长度:', newVal?.length || 0);
					this.updateSolarTermsMap();
					this.createCalendar();
				},
				deep: true
			},
			// 监听法定假日数据变化
			legalHolidays: {
				handler(newVal) {
					console.log('legalHolidays 已更新，数据长度:', newVal?.length || 0);
					this.updateLegalHolidaysMap();
					this.createCalendar();
				},
				deep: true
			},
			// 监听自动获取节日和调休数据设置变化
			autoFetchHolidayData: {
				handler(newVal) {
					console.log('autoFetchHolidayData 已更新:', newVal);
					if (newVal) {
						this.fetchHolidayData();
					}
				}
			},
		},
		methods:{
			// 应用当前月份的色系配置
			applyMonthColorScheme() {
				const month = this.time.month;
				const colorScheme = this.monthColorSchemes[month];
				
				if (colorScheme) {
					console.log(`应用${month}月色系: ${colorScheme.name}`);
					this.currentColorScheme = colorScheme;
					
					// 更新动态颜色配置（不修改props）
					this.dynamicColors.selColor = colorScheme.primary;
					this.dynamicColors.defColor = '#2C2C2E';
					this.dynamicColors.restDayBgColor = colorScheme.restDay;
					this.dynamicColors.restDayColor = colorScheme.restDayText;
					this.dynamicColors.legalHolidayBgColor = colorScheme.holiday;
					this.dynamicColors.legalHolidayColor = colorScheme.holidayText;
					this.dynamicColors.legalWorkdayBgColor = colorScheme.workday;
					this.dynamicColors.legalWorkdayColor = colorScheme.workdayText;
					this.dynamicColors.holidayColor = colorScheme.holidayText;
					this.dynamicColors.solarTermColor = colorScheme.weekText;
					this.dynamicColors.bgColor = colorScheme.background;
					this.dynamicColors.headerBgColor = colorScheme.header;
					this.dynamicColors.headerBorderColor = colorScheme.border;
					this.dynamicColors.weekTextColor = colorScheme.weekText;
					this.dynamicColors.buttonBgColor = colorScheme.button;
					this.dynamicColors.buttonActiveBgColor = colorScheme.buttonActive;
					this.dynamicColors.dotColor = colorScheme.dot;
					
					// 触发样式更新
					this.$nextTick(() => {
						this.updateDateTypeStyles();
					});
				}
			},
			
			// 初始化统一样式配置
			initDateTypeStyles() {
				// 保存当前样式配置以便后续可能的恢复
				const originalStyles = {
					workday: { ...this.dateTypeStyles.workday },
					weekend: { ...this.dateTypeStyles.weekend },
					holiday: { ...this.dateTypeStyles.holiday },
					adjustWorkday: { ...this.dateTypeStyles.adjustWorkday },
					active: { ...this.dateTypeStyles.active }
				};
				
				// 标记是否是首次初始化
				const isFirstInit = !this.stylesInitialized;
				
				// 获取有效颜色值（优先使用动态颜色，fallback到props）
				const getEffectiveColor = (dynamicKey, propValue) => {
					return this.dynamicColors[dynamicKey] || propValue;
				};
				
				// 配置普通工作日样式
				this.dateTypeStyles.workday.bgColor = getEffectiveColor('workDayBgColor', this.workDayBgColor);
				this.dateTypeStyles.workday.textColor = getEffectiveColor('defColor', this.defColor);
				// 只在首次初始化时设置，或者尊重原始值
				if (isFirstInit || !originalStyles.workday.eventTextColor) {
					this.dateTypeStyles.workday.eventTextColor = getEffectiveColor('defColor', this.defColor);
				} else {
					this.dateTypeStyles.workday.eventTextColor = originalStyles.workday.eventTextColor;
				}
				if (isFirstInit || !originalStyles.workday.solarTermColor) {
					this.dateTypeStyles.workday.solarTermColor = getEffectiveColor('solarTermColor', this.solarTermColor);
				} else {
					this.dateTypeStyles.workday.solarTermColor = originalStyles.workday.solarTermColor;
				}
				// 工作日样式也需要支持标记显示（用于调休补班日）
				this.dateTypeStyles.workday.markShow = true;
				this.dateTypeStyles.workday.markText = this.legalWorkdayText;
				
				// 配置周末休息日样式
				this.dateTypeStyles.weekend.bgColor = getEffectiveColor('restDayBgColor', this.restDayBgColor);
				this.dateTypeStyles.weekend.textColor = getEffectiveColor('restDayColor', this.restDayColor);
				// 只在首次初始化时设置，或者尊重原始值
				if (isFirstInit || !originalStyles.weekend.eventTextColor) {
					this.dateTypeStyles.weekend.eventTextColor = getEffectiveColor('restDayColor', this.restDayColor);
				} else {
					this.dateTypeStyles.weekend.eventTextColor = originalStyles.weekend.eventTextColor;
				}
				if (isFirstInit || !originalStyles.weekend.solarTermColor) {
					this.dateTypeStyles.weekend.solarTermColor = getEffectiveColor('solarTermColor', this.solarTermColor);
				} else {
					this.dateTypeStyles.weekend.solarTermColor = originalStyles.weekend.solarTermColor;
				}
				this.dateTypeStyles.weekend.markShow = this.showRestMark;
				this.dateTypeStyles.weekend.markText = this.restMarkText;
				
				// 配置法定节假日样式
				this.dateTypeStyles.holiday.bgColor = getEffectiveColor('legalHolidayBgColor', this.legalHolidayBgColor);
				this.dateTypeStyles.holiday.textColor = getEffectiveColor('legalHolidayColor', this.legalHolidayColor);
				// 只在首次初始化时设置，或者尊重原始值
				if (isFirstInit || !originalStyles.holiday.eventTextColor) {
					this.dateTypeStyles.holiday.eventTextColor = getEffectiveColor('holidayColor', this.holidayColor);
				} else {
					this.dateTypeStyles.holiday.eventTextColor = originalStyles.holiday.eventTextColor;
				}
				if (isFirstInit || !originalStyles.holiday.solarTermColor) {
					this.dateTypeStyles.holiday.solarTermColor = getEffectiveColor('solarTermColor', this.solarTermColor);
				} else {
					this.dateTypeStyles.holiday.solarTermColor = originalStyles.holiday.solarTermColor;
				}
				this.dateTypeStyles.holiday.markShow = this.showRestMark;
				this.dateTypeStyles.holiday.markText = this.legalHolidayText;
				
				// 配置调休工作日样式
				this.dateTypeStyles.adjustWorkday.bgColor = getEffectiveColor('legalWorkdayBgColor', this.legalWorkdayBgColor);
				this.dateTypeStyles.adjustWorkday.textColor = getEffectiveColor('legalWorkdayColor', this.legalWorkdayColor);
				// 只在首次初始化时设置，或者尊重原始值
				if (isFirstInit || !originalStyles.adjustWorkday.eventTextColor) {
					this.dateTypeStyles.adjustWorkday.eventTextColor = getEffectiveColor('legalWorkdayColor', this.legalWorkdayColor);
				} else {
					this.dateTypeStyles.adjustWorkday.eventTextColor = originalStyles.adjustWorkday.eventTextColor;
				}
				if (isFirstInit || !originalStyles.adjustWorkday.solarTermColor) {
					this.dateTypeStyles.adjustWorkday.solarTermColor = getEffectiveColor('solarTermColor', this.solarTermColor);
				} else {
					this.dateTypeStyles.adjustWorkday.solarTermColor = originalStyles.adjustWorkday.solarTermColor;
				}
				this.dateTypeStyles.adjustWorkday.markShow = this.showRestMark;
				this.dateTypeStyles.adjustWorkday.markText = this.legalWorkdayText;
				
				// 配置选中日期样式
				this.dateTypeStyles.active.bgColor = getEffectiveColor('selColor', this.selColor);
				this.dateTypeStyles.active.textColor = '#fff';
				this.dateTypeStyles.active.eventTextColor = '#fff';
				this.dateTypeStyles.active.solarTermColor = '#fff';
				
				// 应用统一的文字颜色方案
				if (this.textColorScheme) {
					// 应用日期数字颜色
					if (this.textColorScheme.date) {
						const dateColors = this.textColorScheme.date;
						if (dateColors.workday) this.dateTypeStyles.workday.textColor = dateColors.workday;
						if (dateColors.weekend) this.dateTypeStyles.weekend.textColor = dateColors.weekend;
						if (dateColors.holiday) this.dateTypeStyles.holiday.textColor = dateColors.holiday;
						if (dateColors.adjustWorkday) this.dateTypeStyles.adjustWorkday.textColor = dateColors.adjustWorkday;
					}
					
					// 应用事件文字颜色
					if (this.textColorScheme.event) {
						const eventColors = this.textColorScheme.event;
						if (eventColors.workday) this.dateTypeStyles.workday.eventTextColor = eventColors.workday;
						if (eventColors.weekend) this.dateTypeStyles.weekend.eventTextColor = eventColors.weekend;
						if (eventColors.holiday) this.dateTypeStyles.holiday.eventTextColor = eventColors.holiday;
						if (eventColors.adjustWorkday) this.dateTypeStyles.adjustWorkday.eventTextColor = eventColors.adjustWorkday;
					}
					
					// 应用节气文字颜色
					if (this.textColorScheme.solarTerm) {
						const solarTermColors = this.textColorScheme.solarTerm;
						if (solarTermColors.workday) this.dateTypeStyles.workday.solarTermColor = solarTermColors.workday;
						if (solarTermColors.weekend) this.dateTypeStyles.weekend.solarTermColor = solarTermColors.weekend;
						if (solarTermColors.holiday) this.dateTypeStyles.holiday.solarTermColor = solarTermColors.holiday;
						if (solarTermColors.adjustWorkday) this.dateTypeStyles.adjustWorkday.solarTermColor = solarTermColors.adjustWorkday;
					}
				}
				
				// 合并用户提供的样式配置
				if (this.dateStyles && typeof this.dateStyles === 'object') {
					// 遍历用户提供的样式配置
					Object.keys(this.dateStyles).forEach(dateType => {
						if (this.dateTypeStyles[dateType]) {
							// 合并用户提供的配置到默认配置
							this.dateTypeStyles[dateType] = {
								...this.dateTypeStyles[dateType],
								...this.dateStyles[dateType]
							};
						}
					});
				}
				
				// 标记样式已初始化
				this.stylesInitialized = true;
				
				console.log('日期样式配置已更新:', this.dateTypeStyles);
			},
			
			// 更新统一样式配置（用于响应props变化）
			updateDateTypeStyles() {
				console.log('更新样式配置');
				
				// 保存样式配置更新前的副本，用于检测变化
				const beforeUpdate = JSON.stringify({
					workday: { ...this.dateTypeStyles.workday },
					weekend: { ...this.dateTypeStyles.weekend },
					holiday: { ...this.dateTypeStyles.holiday },
					adjustWorkday: { ...this.dateTypeStyles.adjustWorkday }
				});
				
				// 执行样式初始化
				this.initDateTypeStyles();
				
				// 检查更新后的样式配置是否发生了实质性变化
				const afterUpdate = JSON.stringify({
					workday: { ...this.dateTypeStyles.workday },
					weekend: { ...this.dateTypeStyles.weekend },
					holiday: { ...this.dateTypeStyles.holiday },
					adjustWorkday: { ...this.dateTypeStyles.adjustWorkday }
				});
				
				// 只有在样式发生实质性变化时才重新渲染日历
				if (beforeUpdate !== afterUpdate) {
					console.log('样式已变化，重新渲染日历');
					this.createCalendar();
				} else {
					console.log('样式未变化，跳过重新渲染');
				}
			},
			// 更新 extraDataMap 的方法
			updateExtraDataMap(){
				this.extraDataMap.clear();
				if(this.extraData && this.extraData.length > 0){
					this.extraData.forEach(item => {
						if (item.date) {
							const normalizedItemDate = item.date.replace(/\b0+/g, '');
							this.extraDataMap.set(normalizedItemDate, item.dot);
						}
					});
				}
				console.log('extraDataMap 已更新:', this.extraDataMap.size);
			},
			// 更新节日数据的方法
			updateHolidaysMap(){
				this.holidaysMap.clear();
				if(this.holidays && this.holidays.length > 0){
					this.holidays.forEach(item => {
						if (item.date) {
							const normalizedItemDate = item.date.replace(/\b0+/g, '');
							this.holidaysMap.set(normalizedItemDate, item.name);
						}
					});
				}
				console.log('holidaysMap 已更新:', this.holidaysMap.size);
			},
			// 更新节气数据的方法
			updateSolarTermsMap(){
				this.solarTermsMap.clear();
				
				// 先加载手动传入的节气数据
				if(this.solarTerms && this.solarTerms.length > 0){
					this.solarTerms.forEach(item => {
						if (item.date) {
							const normalizedItemDate = item.date.replace(/\b0+/g, '');
							this.solarTermsMap.set(normalizedItemDate, item.name);
						}
					});
				}
				
				// 如果开启了自动计算节气，则计算当前月份的节气
				if (this.autoCalculateSolarTerms) {
					this.calculateSolarTerms();
				}
				
				console.log('solarTermsMap 已更新:', this.solarTermsMap.size);
			},
			// 计算节气数据的方法
			calculateSolarTerms() {
				// 清空已计算的节气数据
				this.calculatedSolarTermsMap.clear();
				
				// 获取当前年份
				const year = this.time.year;
				
				try {
					// 创建一个当年的日期对象
					const date = new Date(year, 0, 1);
					
					// 尝试获取当年的节气数据
					try {
						// 方法1: 使用LunarUtil中的节气列表
						const jieQiList = LunarUtil.JIE_QI || [];
						if (jieQiList && jieQiList.length > 0) {
							// 获取当年的农历对象
							const lunar = Lunar.fromDate(date);
							
							// 遍历节气列表
							for (let i = 0; i < jieQiList.length; i++) {
								try {
									const jieQiName = jieQiList[i];
									// 尝试获取节气日期
									const solar = lunar.getJieQiSolar(jieQiName);
									
									if (solar) {
										// 获取节气日期
										const termYear = solar.getYear();
										const termMonth = solar.getMonth();
										const termDay = solar.getDay();
										
										// 创建日期字符串，格式为 "year-month-day"
										const dateStr = `${termYear}-${termMonth}-${termDay}`;
										
										// 将计算得到的节气添加到Map中
										this.calculatedSolarTermsMap.set(dateStr, jieQiName);
										
										// 如果不存在手动设置的节气数据，则添加到solarTermsMap
										if (!this.solarTermsMap.has(dateStr)) {
											this.solarTermsMap.set(dateStr, jieQiName);
										}
									}
								} catch (error) {
									console.error(`计算节气 ${i} 出错:`, error);
								}
							}
						} else {
							console.log("无法从LunarUtil中获取节气列表");
						}
					} catch (innerError) {
						console.error('获取节气列表出错:', innerError);
						
						// 方法2: 直接计算特定日期的节气
						// 遍历全年的每一天，检查是否为节气日
						const startDate = new Date(year, 0, 1);
						const endDate = new Date(year, 11, 31);
						
						for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
							try {
								const solar = Solar.fromDate(d);
								const lunar = solar.getLunar();
								
								// 尝试获取节气
								let jieQiName = '';
								try {
									jieQiName = lunar.getJieQi();
								} catch (err) {
									// 忽略错误
								}
								
								if (jieQiName) {
									const dateStr = `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
									this.calculatedSolarTermsMap.set(dateStr, jieQiName);
									
									// 如果不存在手动设置的节气数据，则添加到solarTermsMap
									if (!this.solarTermsMap.has(dateStr)) {
										this.solarTermsMap.set(dateStr, jieQiName);
									}
								}
							} catch (dateError) {
								// 忽略特定日期的错误
							}
						}
					}
				} catch (error) {
					console.error('计算节气出错:', error);
				}
			},
			// 更新法定假日数据的方法
			updateLegalHolidaysMap(){
				this.legalHolidaysMap.clear();
				if(this.legalHolidays && this.legalHolidays.length > 0){
					this.legalHolidays.forEach(item => {
						if (item.date) {
							const normalizedItemDate = item.date.replace(/\b0+/g, '');
							this.legalHolidaysMap.set(normalizedItemDate, item.isRest);
						}
					});
				}
				console.log('legalHolidaysMap 已更新:', this.legalHolidaysMap.size);
			},
			// 获取节假日信息 - 处理日期节假日和样式
			getHolidayInfo(normalizedDate, isCurrentMonth, isActive, dayIdx) {
				// 获取节日信息
				const holiday = this.holidaysMap.get(normalizedDate) || '';
				
				// 获取法定假日信息
				const legalHoliday = this.legalHolidaysMap.has(normalizedDate) ? 
					this.legalHolidaysMap.get(normalizedDate) : null;
				
				// 计算是否为休息日
				const isWeekend = dayIdx === 0 || dayIdx === 6;
				
				// 确定日期类型 - 修改逻辑以实现需求
				let dateType = 'workday'; // 默认为工作日
				let actualDisplay = 'workday'; // 实际显示样式
				
				if (isActive) {
					dateType = 'active';
					actualDisplay = 'active';
				} else if (legalHoliday === true) {
					// 法定假日：使用周末样式显示
					dateType = 'holiday';
					actualDisplay = 'weekend';
				} else if (legalHoliday === false) {
					// 调休补班：使用工作日样式显示
					dateType = 'adjustWorkday';
					actualDisplay = 'workday';
				} else if (isWeekend) {
					// 普通周末：使用周末样式显示
					dateType = 'weekend';
					actualDisplay = 'weekend';
				}
				
				// 获取相应类型的样式配置 - 使用实际显示样式
				const typeStyle = this.dateTypeStyles[actualDisplay];
				
				// 计算样式类名
				const cssClass = [];
				if (isCurrentMonth) {
					cssClass.push(typeStyle.cssClass);
				}
				
				// 计算背景颜色 - 使用实际显示样式
				let bgColor = isCurrentMonth ? typeStyle.bgColor : '';
				
				// 计算日期数字文字颜色 - 使用实际显示样式
				let textColor = isCurrentMonth ? typeStyle.textColor : this.defColor;
				
				// 计算是否显示休息日标记
				// 法定假日和普通周末显示休息标记
				const showRestMark = isCurrentMonth && !isActive && 
					(dateType === 'holiday' || dateType === 'weekend') && 
					this.dateTypeStyles[actualDisplay].markShow;
				
				// 计算是否显示补班日标记  
				// 只有调休补班日显示补班标记
				const showWorkMark = isCurrentMonth && !isActive && 
					dateType === 'adjustWorkday' && 
					this.dateTypeStyles.workday.markShow;
				
				// 计算要显示的文本及其颜色
				let displayText = '';
				let displayTextColor = '';
				
				if (holiday && isCurrentMonth) {
					displayText = holiday;
					// 使用实际显示样式的事件文字颜色
					displayTextColor = isActive ? 
						this.dateTypeStyles.active.eventTextColor : 
						typeStyle.eventTextColor;
				}
				
				// 获取标记文本
				let restMarkText = '';
				let workMarkText = '';
				
				if (dateType === 'holiday') {
					// 法定假日显示"假"
					restMarkText = this.legalHolidayText;
				} else if (dateType === 'weekend') {
					// 普通周末显示"休"
					restMarkText = this.restMarkText;
				} else if (dateType === 'adjustWorkday') {
					// 调休补班显示"班"
					workMarkText = this.legalWorkdayText;
				}
				
				return {
					holiday: holiday,
					legalHoliday: legalHoliday,
					cssClass: cssClass,
					bgColor: bgColor,
					textColor: textColor,
					displayText: displayText,
					displayTextColor: displayTextColor,
					showRestMark: showRestMark,
					showWorkMark: showWorkMark,
					restMarkText: restMarkText,
					workMarkText: workMarkText,
					dateType: dateType, // 保留原始日期类型信息
					actualDisplay: actualDisplay, // 添加实际显示样式类型
					typeStyle: typeStyle // 使用实际显示样式配置
				};
			},
			
			// 1. 计算并获取节气信息
			getSolarTermInfo(normalizedDate, isCurrentMonth, isActive, dateType, typeStyle) {
				// 获取节气信息（优先使用手动设置的数据，其次使用计算得到的数据）
				const solarTerm = this.solarTermsMap.get(normalizedDate) || this.calculatedSolarTermsMap.get(normalizedDate) || '';
				
				// 如果没有找到节气，尝试使用lunar-typescript库实时计算
				let dynamicSolarTerm = '';
				if (!solarTerm && this.autoCalculateSolarTerms && isCurrentMonth) {
					try {
						// 解析日期
						const dateParts = normalizedDate.split('-');
						if (dateParts.length >= 3) {
							const year = parseInt(dateParts[0]);
							const month = parseInt(dateParts[1]);
							const day = parseInt(dateParts[2]);
							
							// 创建Solar对象
							const solar = Solar.fromYmd(year, month, day);
							
							// 获取农历对象和节气信息
							const lunar = solar.getLunar();
							
							// 农历的节气获取可能因库版本而异，尝试不同的方法
							try {
								// 尝试方法1: 直接调用getJieQi
								dynamicSolarTerm = lunar.getJieQi();
							} catch (err1) {
								try {
									// 尝试方法2: 通过当前节气属性
									const jieQi = lunar.getJieQiInUse();
									if (jieQi) {
										dynamicSolarTerm = jieQi;
									}
								} catch (err2) {
									try {
										// 尝试方法3: 检查当天是否是"节"或"气"
										if (lunar.getJie()) {
											dynamicSolarTerm = lunar.getJie();
										} else if (lunar.getQi()) {
											dynamicSolarTerm = lunar.getQi();
										}
									} catch (err3) {
										console.log('无法通过任何方法获取节气');
									}
								}
							}
							
							// 如果获取到节气信息，添加到缓存中
							if (dynamicSolarTerm) {
								this.calculatedSolarTermsMap.set(normalizedDate, dynamicSolarTerm);
							}
						}
					} catch (error) {
						console.error('实时计算节气出错:', error);
					}
				}
				
				// 使用找到的节气或实时计算的节气
				const finalSolarTerm = solarTerm || dynamicSolarTerm;
				
				// 计算节气文字颜色
				let displayText = '';
				let displayTextColor = '';
				
				if (finalSolarTerm && isCurrentMonth) {
					displayText = finalSolarTerm;
					
					// 根据日期类型和样式配置决定节气文字颜色
					if (typeStyle && typeStyle.solarTermColor) {
						displayTextColor = isActive ? 
							this.dateTypeStyles.active.solarTermColor : 
							typeStyle.solarTermColor;
					} else {
						displayTextColor = isActive ? '#fff' : this.solarTermColor;
					}
				}
				
				return {
					solarTerm: finalSolarTerm,
					displayText: finalSolarTerm ? finalSolarTerm : '',
					displayTextColor: finalSolarTerm ? displayTextColor : ''
				};
			},
			
			// 2. 获取提醒事件红点信息
			getReminderDotInfo(normalizedDate) {
				// 检查是否有红点标记
				const hasDot = this.extraDataMap.get(normalizedDate) || false;
				
				// 获取自定义值
				const customValue = this.getCustomValue(normalizedDate);
				
				return {
					dot: hasDot,
					value: customValue
				};
			},
			
			// 获取日期所有信息 - 统一处理日期数据
			getDateInfo(date, type, dayIdx) {
				// 规范化日期格式
				const normalizedDate = date.replace(/\b0+/g, '');
				
				// 获取日期基本信息
				const dateNumber = parseInt(date.split('-')[2]);
				const isCurrentMonth = type === 'cur';
				const isActive = this.selDate === date;
				
				// 3. 获取节假日信息 (先获取，以便确定日期类型)
				const holidayInfo = this.getHolidayInfo(normalizedDate, true, isActive, dayIdx); // 改为true，让所有日期都应用节假日规则
				
				// 1. 获取节气信息 (使用节假日信息中的实际显示样式)
				const solarTermInfo = this.getSolarTermInfo(
					normalizedDate, 
					true, // 改为true，让所有日期都显示节气
					isActive, 
					holidayInfo.actualDisplay, // 使用实际显示样式类型
					holidayInfo.typeStyle
				);
				
				// 2. 获取提醒事件红点信息
				const reminderInfo = this.getReminderDotInfo(normalizedDate);
				
				// 合并样式类
				let cssClass = [];
				if (type === 'last' || type === 'next') {
					cssClass.push('gray-text'); // 保留灰色文字标识，但仍应用节假日背景色
				}
				cssClass = cssClass.concat(holidayInfo.cssClass);
				
				// 确定要显示的文本（优先级：节日 > 节气 > 自定义值）
				let displayText = '';
				let displayTextColor = isActive ? '#fff' : this.defColor;
				
				if (holidayInfo.displayText) {
					displayText = holidayInfo.displayText;
					displayTextColor = holidayInfo.displayTextColor;
				} else if (solarTermInfo.displayText) {
					displayText = solarTermInfo.displayText;
					displayTextColor = solarTermInfo.displayTextColor;
				} else if (reminderInfo.value && isCurrentMonth) { // 只有当月才显示自定义值
					displayText = reminderInfo.value;
					// 使用事件文字颜色
					if (holidayInfo.typeStyle && holidayInfo.typeStyle.eventTextColor) {
						displayTextColor = isActive ? 
							this.dateTypeStyles.active.eventTextColor : 
							holidayInfo.typeStyle.eventTextColor;
					}
				}
				
				// 对于非当月日期，调整文字颜色为较淡的颜色
				let finalTextColor = holidayInfo.textColor;
				let finalDisplayTextColor = displayTextColor;
				
				if (type === 'last' || type === 'next') {
					// 非当月日期使用较淡的颜色
					finalTextColor = this.adjustColorOpacity(holidayInfo.textColor, 0.4);
					finalDisplayTextColor = this.adjustColorOpacity(displayTextColor, 0.4);
				}
				
				// 返回统一的日期信息对象
				return {
					date: dateNumber,
					type: type,
					cssClass: cssClass.join(' '),
					bgColor: holidayInfo.bgColor,
					textColor: finalTextColor,
					holiday: holidayInfo.holiday,
					solarTerm: solarTermInfo.solarTerm,
					value: reminderInfo.value, 
					dot: reminderInfo.dot && isCurrentMonth, // 只有当月才显示红点
					active: isActive,
					legalHoliday: holidayInfo.legalHoliday,
					displayText: displayText,
					displayTextColor: finalDisplayTextColor,
					showRestMark: holidayInfo.showRestMark && isCurrentMonth, // 只有当月才显示标记
					showWorkMark: holidayInfo.showWorkMark && isCurrentMonth, // 只有当月才显示标记
					restMarkText: holidayInfo.restMarkText,
					border: isActive && isCurrentMonth ? 'none' : '',
					workMarkText: holidayInfo.workMarkText,
					dateType: holidayInfo.dateType,
					actualDisplay: holidayInfo.actualDisplay, // 添加实际显示样式类型
					typeStyle: holidayInfo.typeStyle
				};
			},
			// 获取自定义值
			getCustomValue(normalizedDate) {
				if (!this.showText) {
					return '';
				}
				for (let i = 0; i < this.extraData.length; i++) {
					const item = this.extraData[i];
					if (item.date && item.date.replace(/\b0+/g, '') === normalizedDate) {
						return item.value || '';
					}
				}
				return '';
			},
			// 渲染日历
			createCalendar(type='cur'){
				console.log('createCalendar 被调用, extraData 长度:', this.extraData.length);
				let date = [];
				let days = new Date(this.time.year,this.time.month,0).getDate(); //当月总天数
				let lastDays = new Date(this.time.year,this.time.month-1,0).getDate(); //上月总天数
				let firstDay = new Date(this.time.year,this.time.month-1,1).getDay(); // 当月第一天星期几
				let sum = days + firstDay; //当月总天数 + 前面空的天数 = 当月总格子数
				let line = Math.ceil(sum / 7); //当月总行数
				let curDate = this.time.year + '-' + this.time.month + '-'; //当前年月
				
				// 计算上个月和下个月的年月
				let lastYear = this.time.year;
				let lastMonth = this.time.month - 1;
				if (lastMonth === 0) {
					lastYear = this.time.year - 1;
					lastMonth = 12;
				}
				let lastDate = lastYear + '-' + lastMonth + '-'; //上月年月
				
				let nextYear = this.time.year;
				let nextMonth = this.time.month + 1;
				if (nextMonth === 13) {
					nextYear = this.time.year + 1;
					nextMonth = 1;
				}
				let nextDate = nextYear + '-' + nextMonth + '-'; //下月年月
				
				// 赋值当前选中日期
				this.selDate = type == 'last' ? lastDate + this.selDate.split('-')[2] : type == 'next' ? nextDate + this.selDate.split('-')[2] : this.selDate == '' ? curDate + new Date().getDate() : this.selDate;
				let d = 1;
				for(let i=1; i<=line; i++){
					let arr = [];
					for(let j=1; j<=7; j++){
						let dateObj = {};
						if(i == 1 && j <= firstDay){ //第一行上月数据
							let day = lastDays - firstDay + j;
							dateObj = this.getDateInfo(lastDate + day, 'last', j - 1);
						}else if(d > days){ // 最后一行下月数据
							let day = d - days;
							dateObj = this.getDateInfo(nextDate + day, 'next', j - 1);
							d++;
						}else{ // 当月数据
							let fullDate = curDate + d;
							dateObj = this.getDateInfo(fullDate, 'cur', j - 1);
							d++;
						}
						arr.push(dateObj);
					}
					date.push(arr);
				}
				this.date = date;
				
				// 确保当前选中的日期在日历中正确显示为选中状态
				if (this.selDate) {
					const [selYear, selMonth, selDay] = this.selDate.split('-').map(Number);
					if (selYear === this.time.year && selMonth === this.time.month) {
						// 找到选中的日期并设置为active状态
						for(let i = 0; i < this.date.length; i++) {
							for(let j = 0; j < this.date[i].length; j++) {
								if(this.date[i][j].type === 'cur' && this.date[i][j].date === selDay) {
									this.date[i][j].active = true;
									this.date[i][j].cssClass = this.date[i][j].cssClass.replace('rest-day', '')
										.replace('work-day', '')
										.replace('legal-holiday', '')
										.replace('legal-workday', '')
										+ ' active-text';
									this.date[i][j].bgColor = this.dynamicColors.selColor || this.selColor;
									this.date[i][j].textColor = '#fff';
									this.date[i][j].displayTextColor = '#fff';
									this.date[i][j].border = 'none';
									this.date[i][j].showRestMark = false;
									this.date[i][j].showWorkMark = false;
									break;
								}
							}
						}
					}
				}
			},
			// 点击日期
			dateTap(itm,index,idx){
				if(itm.type == 'last' || itm.type == 'next'){ //判断是否是当前月的数据
					if(itm.type == 'last'){ // 上月
						this.lastMonth()
					}else{ // 下月
						this.nextMonth()
					}
					return
				}
				this.selDate = this.time.year + '-' + this.time.month + '-' + itm.date
				
				// 更新所有日期的active状态
				for(let i = 0; i < this.date.length; i++) {
					for(let j = 0; j < this.date[i].length; j++) {
						this.date[i][j].active = false;
						
						// 更新样式相关属性
						if(i === index && j === idx) {
							this.date[i][j].active = true;
							this.date[i][j].cssClass = this.date[i][j].cssClass.replace('rest-day', '')
								.replace('work-day', '')
								.replace('legal-holiday', '')
								.replace('legal-workday', '')
								+ ' active-text';
							this.date[i][j].bgColor = this.dynamicColors.selColor || this.selColor;
							this.date[i][j].textColor = '#fff';
							this.date[i][j].displayTextColor = '#fff';
							this.date[i][j].border = 'none';
							this.date[i][j].showRestMark = false;
							this.date[i][j].showWorkMark = false;
						} else if(this.date[i][j].type === 'cur') {
							// 重新计算非选中日期的样式
							this.date[i][j] = this.getDateInfo(
								this.time.year + '-' + this.time.month + '-' + this.date[i][j].date, 
								'cur', 
								j
							);
						}
					}
				}
				
				this.$emit('calendarTap', this.selDate)
			},
			// 上个月
			lastMonth(){
				if(this.time.month == 1){
					this.time.year --
					this.time.month = 12
				}else{
					this.time.month --
				}
				
				// 应用新月份的色系
				this.applyMonthColorScheme();
				
				this.$emit('monthTap',this.time)
				
				// 如果启用了自动计算节气，则在月份变化时重新计算
				if (this.autoCalculateSolarTerms) {
					this.calculateSolarTerms();
				}
				
				// 如果启用了自动获取节日和调休数据，则在月份变化时重新获取
				if (this.autoFetchHolidayData) {
					this.fetchHolidayData();
				}
				
				this.createCalendar('last')
			},
			// 下个月
			nextMonth(){
				if(this.time.month == 12){
					this.time.year ++
					this.time.month = 1
				}else{
					this.time.month ++
				}
				
				// 应用新月份的色系
				this.applyMonthColorScheme();
				
				this.$emit('monthTap',this.time)
				
				// 如果启用了自动计算节气，则在月份变化时重新计算
				if (this.autoCalculateSolarTerms) {
					this.calculateSolarTerms();
				}
				
				// 如果启用了自动获取节日和调休数据，则在月份变化时重新获取
				if (this.autoFetchHolidayData) {
					this.fetchHolidayData();
				}
				
				this.createCalendar('next')
			},
			// 获取节假日名称的辅助方法
			getHolidayName(item) {
				// 如果明确提供了名称，则直接使用
				if (item.name) {
					return item.name;
				}
				
				// 如果提供了描述，则使用描述
				if (item.description) {
					return item.description;
				}
				
				// 如果没有明确的名称，根据日期自动判断常见节假日
				const month = item.month;
				const day = item.day;
				
				// 常见的中国节假日
				if (month === 1 && day === 1) return '元旦';
				if (month === 5 && day === 1) return '劳动节';
				if (month === 10 && (day >= 1 && day <= 7)) return '国庆节';
				
				// 农历节日需要通过lunar-typescript库转换，这里简化处理
				if ((month === 1 && day >= 20) || (month === 2 && day <= 20)) {
					// 农历春节一般在这个时间范围
					if (item.holiday === true) return '春节';
				}
				
				if (month === 4 && day >= 4 && day <= 6) return '清明节';
				if (month === 5 && day >= 1 && day <= 5) return '五一';
				if (month === 6 && (day === 1 || day === 2)) return '端午节';
				if (month === 9 && day >= 15 && day <= 30) return '中秋节';
				
				// 如果不能确定具体节日名称，则使用通用标记
				return item.holiday ? '假期' : '工作日';
			},
			
			fetchHolidayData() {
				const year = this.time.year;
				// 为了确保获取完整的数据，我们请求当年和下一年的数据
				const nextYear = year + 1;
				
				console.log(`开始获取 ${year}-${nextYear} 年的日历数据，类型: ${this.apiType}`);
				
				// 使用API服务获取日历数据
				getCalendarData(year, nextYear, this.apiType)
					.then(data => {
						if (data && data.length > 0) {
							console.log('获取日历数据成功:', data);
							
							// 保存当前样式配置，以便在渲染日历后恢复
							const savedStyles = JSON.parse(JSON.stringify(this.dateTypeStyles));
							
							// 清空现有数据
							this.holidaysMap.clear();
							this.legalHolidaysMap.clear();
							const events = []; // 新增事件数组
							
							// 处理后端返回的数据
							data.forEach(item => {
								if (item.year && item.month && item.day) {
									// 构建日期字符串，格式为 "year-month-day"
									const dateStr = `${item.year}-${item.month}-${item.day}`;
									
									// 判断是否有节日名称
									if (item.holiday !== undefined) {
										// 添加节日数据到Map（只为放假日添加节日名称）
										if (item.holiday === true) {
											const holidayName = this.getHolidayName(item);
											this.holidaysMap.set(dateStr, holidayName);
										}
										
										// 添加调休数据到Map (true表示放假，false表示补班)
										this.legalHolidaysMap.set(dateStr, item.holiday);
									}
									
									// 如果有特殊事件标记，添加为红点事件
									if (item.isEvent || item.type === 'event') {
										events.push({
											date: dateStr,
											value: item.name || item.description || '事件',
											dot: true
										});
									}
								}
							});
							
							console.log('已更新 holidaysMap，共有节日数据:', this.holidaysMap.size);
							console.log('已更新 legalHolidaysMap，共有法定假日数据:', this.legalHolidaysMap.size);
							
							// 更新事件数据
							if (events.length > 0) {
								this.extraData = this.extraData || [];
								// 合并已有的事件数据和新的事件数据
								// 首先移除重复的日期
								const existingDates = new Set(events.map(e => e.date));
								const filteredExtraData = this.extraData.filter(e => !existingDates.has(e.date));
								this.extraData = [...filteredExtraData, ...events];
								// 更新extraDataMap
								this.updateExtraDataMap();
							}
							
							// 重新创建日历（使用保存的样式配置）
							const tempStylesInitialized = this.stylesInitialized;
							this.createCalendar();
							
							// 恢复样式配置，以防被覆盖
							Object.keys(savedStyles).forEach(key => {
								if (this.dateTypeStyles[key]) {
									Object.assign(this.dateTypeStyles[key], savedStyles[key]);
								}
							});
							
							// 确保样式配置标记保持不变
							this.stylesInitialized = tempStylesInitialized;
							
						} else {
							console.error('获取日历数据失败或为空');
						}
					})
					.catch(err => {
						console.error('获取日历数据请求失败:', err);
					});
			},
			
			// 公共方法：刷新当前月份的节日和调休数据
			refreshHolidayData() {
				console.log('手动刷新节日和调休数据');
				this.fetchHolidayData();
			},
			
			// 调整颜色透明度的辅助方法
			adjustColorOpacity(color, opacity) {
				if (!color || color === 'transparent') {
					return color;
				}
				
				// 如果是十六进制颜色
				if (color.startsWith('#')) {
					// 转换为rgba格式
					const hex = color.replace('#', '');
					const r = parseInt(hex.substr(0, 2), 16);
					const g = parseInt(hex.substr(2, 2), 16);
					const b = parseInt(hex.substr(4, 2), 16);
					return `rgba(${r}, ${g}, ${b}, ${opacity})`;
				}
				
				// 如果已经是rgba格式
				if (color.startsWith('rgba')) {
					// 替换透明度值
					return color.replace(/[\d\.]+\)$/g, `${opacity})`);
				}
				
				// 如果是rgb格式
				if (color.startsWith('rgb')) {
					// 转换为rgba格式
					return color.replace('rgb', 'rgba').replace(')', `, ${opacity})`);
				}
				
				// 如果是颜色名称，返回原色（无法直接调整透明度）
				return color;
			},
		}
	}
</script>

<style lang="scss" scoped>
	.calendar-content{
		width: 100%;
		display: flex;
		flex-direction: column;
		box-sizing: border-box;
		border-radius: 16upx;
		overflow: hidden;
		transition: all 0.3s ease;
		
		.calendar-header{
			width: 100%;
			height: 120upx;
			display: flex;
			align-items: center;
			justify-content: space-between;
			padding: 0 24upx;
			transition: all 0.3s ease;
			
			.last-month,.next-month{
				width: 80upx;
				height: 80upx;
				display: flex;
				align-items: center;
				justify-content: center;
				border-radius: 50%;
				transition: all 0.2s ease;
				
				&:active {
					transform: scale(0.95);
				}
				
				text {
					font-size: 32upx;
					font-weight: 500;
				}
				
				image{
					width: 32upx;
					height: 32upx;
				}
			}
			.header-time{
				font-size: 34upx;
				font-weight: 600;
			}
		}
		
		.calendar-week{
			width: 100%;
			display: flex;
			transition: all 0.3s ease;
			
			.week-day{
				width: calc(100% / 7);
				height: 80upx;
				display: flex;
				align-items: center;
				justify-content: center;
				font-size: 26upx;
				font-weight: 500;
			}
		}
		
		.calendar-board{
			width: 100%;
			display: flex;
			flex-direction: column;
			transition: all 0.3s ease;
			
			.board-row{
				width: 100%;
				display: flex;
				
				.row-col{
					width: calc(100% / 7);
					height: 120upx;
					position: relative;
					padding: 12upx 8upx;
					box-sizing: border-box;
					display: flex;
					flex-direction: column;
					align-items: center;
					justify-content: center;
					transition: all 0.2s ease;
					
					&:active {
						transform: scale(0.95);
					}
					
					&.gray-text{
						.date-number {
							color: #C7C7CC;
						}
						.date-info {
							color: #C7C7CC;
						}
					}
					
					&.active-text{
						color: #ffffff;
						border-radius: 16upx;
						
						.date-number {
							color: #ffffff !important;
							font-weight: 600;
						}
						.date-info {
							color: #ffffff !important;
						}
					}
					
					&.rest-day{
						border-radius: 12upx;
						
						.date-number {
							font-weight: 500;
						}
					}
					
					&.work-day{
						background: transparent;
						border-radius: 12upx;
					}
					
					&.legal-holiday{
						border-radius: 12upx;
						
						.date-number {
							font-weight: 500;
						}
					}
					
					&.legal-workday{
						border-radius: 12upx;
						
						.date-number {
							font-weight: 500;
						}
					}
					
					// 日期数字
					.date-number {
						width: 100%;
						height: 48upx;
						line-height: 48upx;
						text-align: center;
						font-size: 32upx;
						font-weight: 400;
						margin-bottom: 4upx;
					}
					
					// 下方文本区域
					.date-info {
						width: 100%;
						height: 32upx;
						display: flex;
						justify-content: center;
						align-items: center;
						font-size: 20upx;
						line-height: 1.2;
					}
					
					.dot{
						width: 12upx;
						height: 12upx;
						border-radius: 50%;
						position: absolute;
						top: 16upx;
						right: 16upx;
						box-shadow: 0 2upx 4upx rgba(0, 0, 0, 0.3);
					}
					
					.val{
						font-size: 20upx;
						font-weight: 400;
						text-align: center;
						line-height: 1.2;
					}
					
					.festival{
						font-size: 20upx;
						font-weight: 500;
						text-align: center;
						line-height: 1.2;
					}
					
					.solar-term{
						font-size: 20upx;
						font-weight: 400;
						text-align: center;
						line-height: 1.2;
					}
					
					.rest-mark, .work-mark{
						position: absolute;
						top: 12upx;
						right: 12upx;
						font-size: 18upx;
						width: 32upx;
						height: 32upx;
						border-radius: 50%;
						display: flex;
						align-items: center;
						justify-content: center;
						font-weight: 600;
						box-shadow: 0 2upx 8upx rgba(0, 0, 0, 0.1);
					}
					
					.rest-mark {
						color: #ffffff;
					}
				}
			}
		}
	}
</style>
