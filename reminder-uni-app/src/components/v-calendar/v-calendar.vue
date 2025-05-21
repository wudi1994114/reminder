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
			<view class="week-day" v-for="(item,index) in week" :key="index" :style="{color:defColor}">{{item}}</view>
		</view>
		<view class="calendar-board">
			<view class="board-row" v-for="(item,index) in date" :key="index">
				<view 
				class="row-col" 
				v-for="(itm,idx) in item" 
				:key="idx" 
				:class="[itm.type == 'last' || itm.type == 'next' ? 'gray-text' : '', itm.active && itm.type == 'cur' ? 'active-text' : '']" 
				:style="{background:itm.active && itm.type == 'cur' ? selColor : '',border:itm.active && itm.type == 'cur' ? 'none' : ''}"
				@tap="dateTap(itm,index,idx)">
					<text :style="{color:itm.type == 'last' || itm.type == 'next' ? '#C0C4CC' : itm.active && itm.type == 'cur' ? '#fff' : defColor }">{{itm.date}}</text>
					<text class="dot" v-if="itm.dot && itm.type == 'cur'" :style="{background:itm.active ? '#fff' : selColor}"></text>
					<text class="val" v-if="itm.value != '' && itm.type == 'cur'" :style="{color:itm.active ? '#fff' : defColor}">{{itm.value}}</text>
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
				selDate:''
			};
		},
		created() {
			this.time = this.defaultTime == {} ? this.time : this.defaultTime
			this.createCalendar()
		},
		methods:{
			// 渲染日历
			createCalendar(type='cur'){
				let date = []
				let days = new Date(this.time.year,this.time.month,0).getDate() //当月总天数
				let lastDays = new Date(this.time.year,this.time.month-1,0).getDate() //上月总天数
				let firstDay = new Date(this.time.year,this.time.month-1,1).getDay() // 当月第一天星期几
				let sum = days + firstDay //当月总天数 + 前面空的天数 = 当月总格子数
				let line = Math.ceil(sum / 7) //当月总行数
				let curDate = this.time.year + '-' + this.time.month + '-' //当前年月
				let lastDate = this.time.year + '-' + (this.time.month-1) + '-' //上月年月
				let nextDate = this.time.year + '-' + (this.time.month+1) + '-' //下月年月
				// 赋值当前选中日期
				this.selDate = type == 'last' ? lastDate + this.selDate.split('-')[2] : type == 'next' ? nextDate + this.selDate.split('-')[2] : this.selDate == '' ? curDate + new Date().getDate() : this.selDate
				let d = 1
				for(let i=1;i<=line;i++){
					let arr = []
					for(let j=1;j<=7;j++){
						let obj = {}
						if(i == 1 && j <= firstDay){ //第一行上月数据
							let day = lastDays - firstDay + j
							obj = {
								date:day,
								type:'last',
								value:this.getValue(lastDate+day),
								dot:this.getDot(lastDate+day),
								active:this.getActive(lastDate+day)
							}
						}else if(d > days){ // 最后一行下月数据
							let day = d - days
							obj = {
								date:day,
								type:'next',
								value:this.getValue(nextDate+day),
								dot:this.getDot(nextDate+day),
								active:this.getActive(nextDate+day)
							}
							d++
						}else{ // 当月数据
							obj = {
								date:d,
								type:'cur',
								value:this.getValue(curDate+d),
								dot:this.getDot(curDate+d),
								active:this.getActive(curDate+d)
							}
							d++
						}
						arr.push(obj)
					}
					date.push(arr)
				}
				this.date = date
			},
			// 获取value值
			getValue(date){
				if(!this.showText){
					return ''
				}
				let val = ''
				this.extraData.forEach(item=>{
					if(item.date == date){
						val = item.value
					}
				})
				return val
			},
			// 获取点
			getDot(date){
				if(!this.showDot){
					return false
				}
				let dot = false
				this.extraData.forEach(item=>{
					if(item.date == date){
						dot = item.dot
					}
				})
				return dot
			},
			// 获取选中
			getActive(date){
				let active = false
				this.extraData.forEach(item=>{
					if(item.date == date && item.active){
						active = item.active
						this.selDate = item.date
					}
				})
				if(!active){
					active = this.selDate == date ? true : false
				}
				return active
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
				this.date.forEach(item=>{ //清除所有选中active
					item.forEach(itm1=>{
						itm1.active = false
					})
				})
				this.date[index][idx].active = true //添加当前选中active
				this.$emit('calendarTap',this.selDate)
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
					display: flex;
					flex-direction: column;
					align-items: center;
					justify-content: center;
					font-size: 28upx;
					position: relative;
					&.gray-text{
						color: #C0C4CC;
					}
					&.active-text{
						background: #4198f8;
						color: #fff;
						border-radius: 50%;
					}
					.dot{
						width: 10upx;
						height: 10upx;
						border-radius: 50%;
						background: #4198f8;
						position: absolute;
						bottom: 10upx;
						left: 50%;
						transform: translateX(-50%);
					}
					.val{
						font-size: 20upx;
						margin-top: 5upx;
					}
				}
			}
		}
	}
</style>
