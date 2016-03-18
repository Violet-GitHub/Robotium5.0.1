package com.robotium.solo;

import junit.framework.Assert;
import android.app.Activity;
import android.app.ActivityManager;

/**
 * 断言工具类，判断Activity，内存等
 * Contains assert methods examples are assertActivity() and assertLowMemory().
 * 
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

class Asserter {
	//Activity操作工具类；
	private final ActivityUtils activityUtils;
	//等待工具类；
	private final Waiter waiter;

	/**
	 * 构造类->构造方法
	 * Constructs this object.
	 *
	 *ActivityUtils实例和Waiter实例参数
	 * @param activityUtils the {@code ActivityUtils} instance.
	 * @param waiter the {@code Waiter} instance.
	 */

	public Asserter(ActivityUtils activityUtils, Waiter waiter) {
		this.activityUtils = activityUtils;
		this.waiter = waiter;
	}

	/**
	 * 断言判断当前Activity是不是预期想要的
	 * message 当前Activity名字与name名不一致，给出断言提示
	 * name 期望Activity名
	 * 
	 * Asserts that an expected {@link Activity} is currently active one.
	 * @param message the message that should be displayed if the assert fails
	 * @param name the name of the {@code Activity} that is expected to be active e.g. {@code "MyActivity"}
	 */

	public void assertCurrentActivity(String message, String name) {
		//使用wait工具等待期望的Activity出现，直接获取堆栈栈顶的Activity，默认等待时间10s
		boolean foundActivity = waiter.waitForActivity(name);
		//如果未出现，则给出message提示
		if(!foundActivity){
			//通过ActivityUtils工具类获取当前的Activity
			Activity activity = activityUtils.getCurrentActivity();
			//对Activity进行null判断
			if(activity != null){
				//通过assert比较Activity的名字和name值，是否相同。
				Assert.assertEquals(message, name, activity.getClass().getSimpleName());	
			}
			//如果未找到任何的Activity，则提示No Activity found
			else{
				Assert.assertEquals(message, name, "No actvity found");
			}
		}
	}

	/**
	 * 通过Activity实例对象，来判断栈顶的Activity是不是预期想要的
	 * message 如果判断失败，给出断言提示
	 * expectedClass 期望的Activity类
	 * 
	 * Asserts that an expected {@link Activity} is currently active one.
	 * @param message the message that should be displayed if the assert fails
	 * @param expectedClass the {@code Class} object that is expected to be active e.g. {@code MyActivity.class}
	 */

	public void assertCurrentActivity(String message, Class<? extends Activity> expectedClass) {
		//null值判断
		if(expectedClass == null){
			Assert.fail("The specified Activity is null!");
		}
		
		//通过waiter工具类，等待期望的Activity类出现，默认时间是10s
		boolean foundActivity = waiter.waitForActivity(expectedClass);
		//如果没找到期望的Activity，给出断言提示
		if(!foundActivity) {		
			Activity activity = activityUtils.getCurrentActivity();
			//没找到，存在两种情况，将给出不同的提示
			if(activity != null){
				//但不是期望的Activity，给出断言提示
				Assert.assertEquals(message, expectedClass.getName(), activity.getClass().getName());
			}		
			//否则，不存在Activity，给出不存在Activity的提示
			else{
				Assert.assertEquals(message, expectedClass.getName(), "No activity found");
			}
		}
	}

	/**
	 * 判断当前class类名是否为预期的Activity的类名
	 * message 断言判断失败后，给出message中的提示
	 * name activity的名字
	 * isNewInstance 为true则等待最新出现的activity,为false则直接获取activity堆栈的栈顶activity做比较
	 * Asserts that an expected {@link Activity} is currently active one, with the possibility to
	 * verify that the expected {@code Activity} is a new instance of the {@code Activity}.
	 * 
	 * @param message the message that should be displayed if the assert fails
	 * @param name the name of the {@code Activity} that is expected to be active e.g. {@code "MyActivity"}
	 * @param isNewInstance {@code true} if the expected {@code Activity} is a new instance of the {@code Activity}
	 */

	public void assertCurrentActivity(String message, String name, boolean isNewInstance) {
		//通过Activity名判断当前Activity是不是预期的
		assertCurrentActivity(message, name);
		//通过ActivityUtils工具获取当前Activity；
		Activity activity = activityUtils.getCurrentActivity();
		//如果获取结果不为空值
		if(activity != null){
			//判断当前class类是否是期望的Activity
			assertCurrentActivity(message, activity.getClass(),
					isNewInstance);	
		}
	}

	/**
	 * 判断当前class类是否是期望的Activity
	 * message 断言判断失败后，给出message中的提示；
	 * expectedClass 预期Activity实例
	 * isNewInstance 为true则等待最新出现的activity,为false则直接获取activity堆栈的栈顶activity做比较
	 * 
	 * Asserts that an expected {@link Activity} is currently active one, with the possibility to
	 * verify that the expected {@code Activity} is a new instance of the {@code Activity}.
	 * 
	 * @param message the message that should be displayed if the assert fails
	 * @param expectedClass the {@code Class} object that is expected to be active e.g. {@code MyActivity.class}
	 * @param isNewInstance {@code true} if the expected {@code Activity} is a new instance of the {@code Activity}
	 */

	public void assertCurrentActivity(String message, Class<? extends Activity> expectedClass,
			boolean isNewInstance) {
		boolean found = false;
		//通过Activity实例判断栈顶的Activity是不是预期想要的
		assertCurrentActivity(message, expectedClass);
		//通过ActivityUtils工具获取栈顶的Activity
		Activity activity = activityUtils.getCurrentActivity(false);
		//如果当前Activity实例对象为空
		if(activity == null){
			// 断言判断当前的类，不是期望的类
			Assert.assertNotSame(message, isNewInstance, false);
			return;
		}
		//判断当前所有打开的Activity中是否存在期望值
		for (int i = 0; i < activityUtils.getAllOpenedActivities().size() - 1; i++) {
			String instanceString = activityUtils.getAllOpenedActivities().get(i).toString();
			if (instanceString.equals(activity.toString()))
				found = true;
		}
		//断言提示是否找到
		Assert.assertNotSame(message, isNewInstance, found);
	}

	/**
	 * 检查系统可用内存是否过低
	 * Asserts that the available memory is not considered low by the system.
	 */

	public void assertMemoryNotLow() {
		//构建内存信息对象
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		//获取当前Activity的内存信息
		((ActivityManager)activityUtils.getCurrentActivity().getSystemService("activity")).getMemoryInfo(mi);
		//通过lowMemory判断内存是否过低
		Assert.assertFalse("Low memory available: " + mi.availMem + " bytes!", mi.lowMemory);
	}

}
