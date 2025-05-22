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
					>
						{{legalWorkdayText}}
					</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
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
				default:'#4198f8'
			},
			// 默认文字颜色
			defColor:{
				type:String,
				default:'#333'
			},
			// 上个月文字
			lastText:{
				type:String,
				default:'<'
			},
			// 下个月文字
			nextText:{
				type:String,
				default:'>'
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
				default:false
			},
			// 工作日背景色
			workDayBgColor:{
				type:String,
				default:'transparent'
			},
			// 休息日背景色
			restDayBgColor:{
				type:String,
				default:'rgba(250, 250, 250, 0.5)'
			},
			// 休息日文字颜色
			restDayColor:{
				type:String,
				default:'#ff4500'
			},
			// 休息日显示标记
			showRestMark:{
				type:Boolean,
				default:false
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
				default:'rgba(255, 235, 235, 0.8)'
			},
			// 法定假日文字颜色
			legalHolidayColor:{
				type:String,
				default:'#ff0000'
			},
			// 法定补班日背景色
			legalWorkdayBgColor:{
				type:String,
				default:'rgba(235, 255, 235, 0.8)'
			},
			// 法定补班日文字颜色
			legalWorkdayColor:{
				type:String,
				default:'#008000'
			},
			// 节日数据 [{date:'2023-1-1',name:'元旦'}]
			holidays:{
				type:Array,
				default:()=>{
					return [{date:'2025-5-23',name:'元旦/清明节'}]
				}
			},
			// 节日文字颜色
			holidayColor:{
				type:String,
				default:'#ff0000'
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
				default:'#008000'
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
				legalHolidaysMap: new Map() // 用于存储法定假日数据的 Map 结构
			};
		},
		created() {
			console.log('v-calendar 组件创建, extraData:', this.extraData?.length || 0);
			this.time = this.defaultTime == {} ? this.time : this.defaultTime;
			this.updateExtraDataMap(); // 初始化时更新 Map
			this.updateHolidaysMap(); // 初始化节日 Map
			this.updateSolarTermsMap(); // 初始化节气 Map
			this.updateLegalHolidaysMap(); // 初始化法定假日 Map
			this.createCalendar();
		},
		watch: {
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
			}
		},
		methods:{
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
				if(this.solarTerms && this.solarTerms.length > 0){
					this.solarTerms.forEach(item => {
						if (item.date) {
							const normalizedItemDate = item.date.replace(/\b0+/g, '');
							this.solarTermsMap.set(normalizedItemDate, item.name);
						}
					});
				}
				console.log('solarTermsMap 已更新:', this.solarTermsMap.size);
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
			// 获取日期所有信息 - 统一处理日期数据
			getDateInfo(date, type, dayIdx) {
				// 规范化日期格式
				const normalizedDate = date.replace(/\b0+/g, '');
				
				// 获取日期基本信息
				const dateNumber = parseInt(date.split('-')[2]);
				const isCurrentMonth = type === 'cur';
				
				// 获取各种日期状态
				const holiday = this.holidaysMap.get(normalizedDate) || '';
				const solarTerm = this.solarTermsMap.get(normalizedDate) || '';
				const customValue = this.getCustomValue(normalizedDate);
				const hasDot = this.extraDataMap.get(normalizedDate) || false;
				const isActive = this.selDate === date;
				const legalHoliday = this.legalHolidaysMap.has(normalizedDate) ? 
					this.legalHolidaysMap.get(normalizedDate) : null;
				
				// 计算是否为休息日
				const isWeekend = dayIdx === 0 || dayIdx === 6;
				
				// 样式计算
				const cssClass = [];
				if (type === 'last' || type === 'next') {
					cssClass.push('gray-text');
				}
				if (isActive && isCurrentMonth) {
					cssClass.push('active-text');
				} else if (this.showWorkRestDay && isCurrentMonth) {
					if (legalHoliday !== null) {
						cssClass.push(legalHoliday ? 'legal-holiday' : 'legal-workday');
					} else if (isWeekend) {
						cssClass.push('rest-day');
					} else {
						cssClass.push('work-day');
					}
				}
				
				// 背景颜色计算
				let bgColor = '';
				if (isActive && isCurrentMonth) {
					bgColor = this.selColor;
				} else if (this.showWorkRestDay && isCurrentMonth) {
					if (legalHoliday !== null) {
						bgColor = legalHoliday ? this.legalHolidayBgColor : this.legalWorkdayBgColor;
					} else if (isWeekend) {
						bgColor = this.restDayBgColor;
					} else {
						bgColor = this.workDayBgColor;
					}
				}
				
				// 文字颜色计算
				let textColor = '';
				if (type === 'last' || type === 'next') {
					textColor = '#C0C4CC';
				} else if (isActive && isCurrentMonth) {
					textColor = '#fff';
				} else if (holiday && isCurrentMonth) {
					textColor = this.holidayColor;
				} else if (legalHoliday !== null && isCurrentMonth) {
					textColor = legalHoliday ? this.legalHolidayColor : this.legalWorkdayColor;
				} else if (this.showWorkRestDay && isWeekend) {
					textColor = this.restDayColor;
				} else {
					textColor = this.defColor;
				}
				
				// 是否显示休息日标记
				const showRestMark = this.showRestMark && isCurrentMonth && !isActive && 
					(legalHoliday === true || (legalHoliday === null && isWeekend));
				
				// 是否显示补班日标记
				const showWorkMark = this.showRestMark && isCurrentMonth && !isActive && legalHoliday === false;
				
				// 计算要显示的文本（节日、节气或自定义值）
				let displayText = '';
				let displayTextColor = isActive ? '#fff' : this.defColor;
				
				if (holiday && isCurrentMonth) {
					displayText = holiday;
					displayTextColor = isActive ? '#fff' : this.holidayColor;
				} else if (solarTerm && isCurrentMonth) {
					displayText = solarTerm;
					displayTextColor = isActive ? '#fff' : this.solarTermColor;
				} else if (customValue && isCurrentMonth) {
					displayText = customValue;
				}
				
				// 返回统一的日期信息对象
				return {
					date: dateNumber,
					type: type,
					cssClass: cssClass.join(' '),
					bgColor: bgColor,
					textColor: textColor,
					holiday: holiday,
					solarTerm: solarTerm,
					value: customValue, 
					dot: hasDot,
					active: isActive,
					legalHoliday: legalHoliday,
					displayText: displayText,
					displayTextColor: displayTextColor,
					showRestMark: showRestMark,
					showWorkMark: showWorkMark,
					restMarkText: legalHoliday === true ? this.legalHolidayText : this.restMarkText,
					border: isActive && isCurrentMonth ? 'none' : ''
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
				let lastDate = this.time.year + '-' + (this.time.month-1) + '-'; //上月年月
				let nextDate = this.time.year + '-' + (this.time.month+1) + '-'; //下月年月
				
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
							this.date[i][j].bgColor = this.selColor;
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
				this.$emit('monthTap',this.time)
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
				this.$emit('monthTap',this.time)
				this.createCalendar('next')
			}
		}
	}
</script>

<style lang="scss" scoped>
	.calendar-content{
		width: 100%;
		display: flex;
		flex-direction: column;
		// padding: 0 30upx;
		box-sizing: border-box;
		background: #fff;
		color: #333;
		.calendar-header{
			width: 100%;
			height: 100upx;
			display: flex;
			align-items: center;
			justify-content: space-between;
			.last-month,.next-month{
				width: 100upx;
				height: 100upx;
				display: flex;
				align-items: center;
				justify-content: center;
				image{
					width: 30upx;
					height: 30upx;
				}
			}
			.header-time{
				font-size: 30upx;
			}
		}
		.calendar-week{
			width: 100%;
			display: flex;
			.week-day{
				width: calc(100% / 7);
				height: 80upx;
				display: flex;
				align-items: center;
				justify-content: center;
				font-size: 28upx;
			}
		}
		.calendar-board{
			width: 100%;
			display: flex;
			flex-direction: column;
			.board-row{
				width: 100%;
				display: flex;
				.row-col{
					width: calc(100% / 7);
					height: 100upx;
					position: relative;
					padding: 8upx 0;
					box-sizing: border-box;
					
					&.gray-text{
						color: #C0C4CC;
					}
					&.active-text{
						background: #4198f8;
						color: #fff;
						border-radius: 50%;
					}
					&.rest-day{
						// 休息日样式可由属性控制
						border-radius: 8upx;
					}
					&.work-day{
						// 工作日样式可由属性控制
					}
					&.legal-holiday{
						// 法定假日样式
						border-radius: 8upx;
					}
					&.legal-workday{
						// 法定补班日样式
						border-radius: 8upx;
					}
					
					// 日期数字
					.date-number {
						width: 100%;
						height: 40upx;
						line-height: 40upx;
						text-align: center;
						font-size: 28upx;
						position: relative;
						top: 4upx;
					}
					
					// 下方文本区域
					.date-info {
						width: 100%;
						height: 40upx;
						display: flex;
						justify-content: center;
						align-items: center;
					}
					
					.dot{
						width: 16upx;
						height: 16upx;
						border-radius: 50%;
						background: #ff4500;
						position: absolute;
						top: 0upx;
						right: 20upx;
					}
					.val{
						font-size: 20upx;
					}
					.festival{
						font-size: 20upx;
						font-weight: bold;
					}
					.solar-term{
						font-size: 20upx;
						font-style: italic;
					}
					.rest-mark, .work-mark{
						position: absolute;
						top: 8upx;
						right: 8upx;
						font-size: 16upx;
						width: 24upx;
						height: 24upx;
						border-radius: 50%;
						display: flex;
						align-items: center;
						justify-content: center;
						font-weight: bold;
					}
					.rest-mark{
						background: rgba(255, 69, 0, 0.1);
						color: #ff4500;
					}
					.work-mark{
						background: rgba(0, 128, 0, 0.1);
						color: #008000;
					}
				}
			}
		}
	}
</style>
