package com.robotium.solo;

import android.app.Activity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.TimePicker;


/**
 * 包含设置方法
 * Contains set methods. Examples are setDatePicker(),
 * setTimePicker().
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class Setter{
	//关闭
	private final int CLOSED = 0;
	//打开
	private final int OPENED = 1;
	//Activity工具类
	private final ActivityUtils activityUtils;
	//获取工具类
	private final Getter getter;
	//点击工具类
	private final Clicker clicker;
	//等待工具类
	private final Waiter waiter;

	/**
	 * 
	 * Constructs this object.
	 *
	 * @param activityUtils the {@code ActivityUtils} instance
	 * @param getter the {@code Getter} instance
	 * @param clicker the {@code Clicker} instance
	 * @param waiter the {@code Waiter} instance
	 */

	public Setter(ActivityUtils activityUtils, Getter getter, Clicker clicker, Waiter waiter) {
		this.activityUtils = activityUtils;
		this.getter = getter;
		this.clicker = clicker;
		this.waiter = waiter;
	}


	/**
	 * 设置控件日期
	 * datePicker   需要设置日期的控件
	 * year         年
	 * monthOfYear  月
	 * dayOfMonth   日
	 * Sets the date in a given {@link DatePicker}.
	 *
	 * @param datePicker the {@code DatePicker} object.
	 * @param year the year e.g. 2011
	 * @param monthOfYear the month which is starting from zero e.g. 03
	 * @param dayOfMonth the day e.g. 10
	 */

	public void setDatePicker(final DatePicker datePicker, final int year, final int monthOfYear, final int dayOfMonth) {
		//null判断
		if(datePicker != null){
			//获取当前Activity
			Activity activity = activityUtils.getCurrentActivity(false);
			//Activitynull判断
			if(activity != null){
				// 在当前activity的Ui线程中执行,直接调用会引发跨线程权限异常
				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
						try{
							//给控件设置年月日
							datePicker.updateDate(year, monthOfYear, dayOfMonth);
						}catch (Exception ignored){}
					}
				});
			}
		}
	}


	/**
	 * 设置时间控件的，时分
	 * Sets the time in a given {@link TimePicker}.
	 *
	 * @param timePicker the {@code TimePicker} object.
	 * @param hour the hour e.g. 15
	 * @param minute the minute e.g. 30
	 */

	public void setTimePicker(final TimePicker timePicker, final int hour, final int minute) {
		//null值判断
		if(timePicker != null){
			//获取当前Activity
			Activity activity = activityUtils.getCurrentActivity(false);
			//Activity的null判断
			if(activity != null){
				// 在当前activity的Ui线程中执行,直接调用会引发跨线程权限异常
				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
						try{
							//设置时
							timePicker.setCurrentHour(hour);
							//设置分
							timePicker.setCurrentMinute(minute);
						}catch (Exception ignored){}
					}
				});
			}
		}
	}


	/**
	 * 设置进度条控件属性
	 * progressBar   需要设置的进度条
	 * progress      设置的值
	 * Sets the progress of a given {@link ProgressBar}. Examples are SeekBar and RatingBar.
	 * @param progressBar the {@code ProgressBar}
	 * @param progress the progress that the {@code ProgressBar} should be set to
	 */

	public void setProgressBar(final ProgressBar progressBar,final int progress) {
		//null值判断
		if(progressBar != null){
			//获取当前Activity
			Activity activity = activityUtils.getCurrentActivity(false);
			if(activity != null){
				//在当前Activity的UI线程中运行，直接调用会引发跨线程权限异常
				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
						try{
							progressBar.setProgress(progress);
						}catch (Exception ignored){}
					}
				});
			}
		}
	}


	/**
	 * 设置选择开关属性，开 关 
	 * slidingDrawer   需要设置的选择开关
	 * Sets the status of a given SlidingDrawer. Examples are Solo.CLOSED and Solo.OPENED.
	 *
	 * @param slidingDrawer the {@link SlidingDrawer}
	 * @param status the status that the {@link SlidingDrawer} should be set to
	 */

	public void setSlidingDrawer(final SlidingDrawer slidingDrawer, final int status){
		if(slidingDrawer != null){
			//获取当前的Activity
			Activity activity = activityUtils.getCurrentActivity(false);
			if(activity != null){
				//在当前的Activity的UI线程中运行，直接调用会引发线程权限异常
				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
						try{
							switch (status) {
							case CLOSED:
								slidingDrawer.close();
								break;
							case OPENED:
								slidingDrawer.open();
								break;
							}
						}catch (Exception ignored){}
					}
				});
			}
		}
	}

	/**
	 * 设置NavigationDrawer的开关状态
	 * Sets the status of the NavigationDrawer. Examples are Solo.CLOSED and Solo.OPENED.
	 *
	 * @param status the status that the {@link NavigationDrawer} should be set to
	 */

	public void setNavigationDrawer(final int status){
		//获取homeView试图
		final View homeView = getter.getView("home", 0);
		//获取leftDrawer试图
		final View leftDrawer = getter.getView("left_drawer", 0);
		
		try{
			switch (status) {
			
			case CLOSED:
				if(leftDrawer != null && homeView != null && leftDrawer.isShown()){
					//点击homeView试图
					clicker.clickOnScreen(homeView);
				}
				break;
				
			case OPENED:
				if(leftDrawer != null && homeView != null &&  !leftDrawer.isShown()){
					//点击homeView试图
					clicker.clickOnScreen(homeView);
					//判断leftDrawer是否可见
					Condition condition = new Condition() {
						//重写他的isSatisfied方法，返回leftDrawer是否可见
						@Override
						public boolean isSatisfied() {
							return leftDrawer.isShown();
						}
					};
					//在规定时间内得等condition出现
					waiter.waitForCondition(condition, Timeout.getSmallTimeout());
				}
				break;
			}
		}catch (Exception ignored){}
	}
}
