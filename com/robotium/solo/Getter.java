package com.robotium.solo;

import junit.framework.Assert;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.TextView;


/**
 * Contains various get methods. Examples are: getView(int id),
 * getView(Class<T> classToFilterBy, int index).
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class Getter {
	//事件发送器
	private final Instrumentation instrumentation;
	//Activity工具类
	private final ActivityUtils activityUtils;
	//等待工具类
	private final Waiter waiter;
	//超时1s
	private final int TIMEOUT = 1000;

	/**
	 * Constructs this object.
	 * 
	 * @param inst the {@code Instrumentation} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param waiter the {@code Waiter} instance
	 */

	public Getter(Instrumentation instrumentation, ActivityUtils activityUtils, Waiter waiter){
		this.instrumentation = instrumentation;
		this.activityUtils = activityUtils;
		this.waiter = waiter;
	}


	/**
	 * 返回指定类型中指定的view
	 * Returns a {@code View} with a certain index, from the list of current {@code View}s of the specified type.
	 *
	 * @param classToFilterBy which {@code View}s to choose from
	 * @param index choose among all instances of this type, e.g. {@code Button.class} or {@code EditText.class}
	 * @return a {@code View} with a certain index, from the list of current {@code View}s of the specified type
	 */

	public <T extends View> T getView(Class<T> classToFilterBy, int index) {
		//通过waiter工具类，找到指定类型中指定的view
		return waiter.waitForAndGetView(index, classToFilterBy);
	}

	/**
	 * 从指定类型view的列表中，返回显示指定字符串的view
	 * Returns a {@code View} that shows a given text, from the list of current {@code View}s of the specified type.
	 *
	 * @param classToFilterBy which {@code View}s to choose from //指定view类型
	 * @param text the text that the view shows     //指定字符串
	 * @param onlyVisible {@code true} if only visible texts on the screen should be returned  //如果为true只找屏幕上可见的，false找所有的
	 * @return a {@code View} showing a given text, from the list of current {@code View}s of the specified type
	 */

	public <T extends TextView> T getView(Class<T> classToFilterBy, String text, boolean onlyVisible) {
		//通过waiter工具类，在1s内找到指定类型view
		T viewToReturn = (T) waiter.waitForText(classToFilterBy, text, 0, Timeout.getSmallTimeout(), false, onlyVisible, false);
		//没找到，则记录日志并退出
		if(viewToReturn == null)
			Assert.fail(classToFilterBy.getSimpleName() + " with text: '" + text + "' is not found!");
		//找到，返回
		return viewToReturn;
	}

	/**
	 * 按照指定资源id，获取当前activity中的 String
	 * Returns a localized string
	 * 
	 * @param id the resource ID for the string
	 * @return the localized string
	 */

	public String getString(int id)
	{
		//获取当前的Activity
		Activity activity = activityUtils.getCurrentActivity(false);
		if(activity == null){
			return "";
		}
		return activity.getString(id);
	}

	/**
	 * 根据指定string id，获取Activity中的string
	 * Returns a localized string
	 * 
	 * @param id the resource ID for the string
	 * @return the localized string
	 */

	public String getString(String id)
	{	//获取context
		Context targetContext = instrumentation.getTargetContext(); 
		//通过context获取包名
		String packageName = targetContext.getPackageName(); 
		// 按照String类型id在当前应用中查询对应的int id
		int viewId = targetContext.getResources().getIdentifier(id, "string", packageName);
		//如果找不到，整个android中查
		if(viewId == 0){
			viewId = targetContext.getResources().getIdentifier(id, "string", "android");
		}
		// 按照指定资源id，获取当前activity中的 String
		return getString(viewId);		
	}
	
	/**
	 * 返回指定id的view
	 * Returns a {@code View} with a given id.
	 * 
	 * @param id the R.id of the {@code View} to be returned
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @param timeout the timeout in milliseconds
	 * @return a {@code View} with a given id
	 */

	public View getView(int id, int index, int timeout){
		//通过waiter工具类根据指定id，在规定时间里，找到指定index的view
		return waiter.waitForView(id, index, timeout);
	}

	/**
	 * 获取指定id的第index个 View.默认超时10s
	 * Returns a {@code View} with a given id.
	 * 
	 * @param id the R.id of the {@code View} to be returned
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @return a {@code View} with a given id
	 */

	public View getView(int id, int index){
		return getView(id, index, 0);
	}

	/**
	 * 获取指定id的第index个 View，如设置的index小于1，那么返 回当前activity中id为 0的view.
	 * Returns a {@code View} with a given id.
	 * 
	 * @param id the id of the {@link View} to return
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @return a {@code View} with a given id
	 */

	public View getView(String id, int index){
		View viewToReturn = null;
		Context targetContext = instrumentation.getTargetContext(); 
		//获取程序名
		String packageName = targetContext.getPackageName(); 
		//获取字符串id的view的正数id
		int viewId = targetContext.getResources().getIdentifier(id, "id", packageName);
		//null判断
		if(viewId != 0){
			//获取指定id的view
			viewToReturn = getView(viewId, index, TIMEOUT); 
		}
		//为null，在整个Android环境中查找，并获取指定id的view
		if(viewToReturn == null){
			int androidViewId = targetContext.getResources().getIdentifier(id, "id", "android");
			if(androidViewId != 0){
				viewToReturn = getView(androidViewId, index, TIMEOUT);
			}
		}
		//不为null，则返回找到的view
		if(viewToReturn != null){
			return viewToReturn;
		}
		return getView(viewId, index); 
	}

	/**
	 * 根据给定的tag(标签)，返回view
	 * Returns a {@code View} with a given tag.
	 *
	 * @param tag the <code>tag</code> of the {@code View} to be returned
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @param timeout the timeout in milliseconds
	 * @return a {@code View} with a given tag if available, <code>null</code> otherwise
	 */

	public View getView(Object tag, int index, int timeout){
		//Because https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/View.java#L17005-L17007
		if(tag == null) {
			return null;
		}

		final Activity activity = activityUtils.getCurrentActivity(false);
		View viewToReturn = null;

		if(index < 1){
			index = 0;
			if(activity != null){
				//Using https://github.com/android/platform_frameworks_base/blob/master/core/java/android/app/Activity.java#L2070-L2072
				Window window = activity.getWindow();
				if(window != null) {
					View decorView = window.getDecorView();
					if(decorView != null) {
						viewToReturn = decorView.findViewWithTag(tag);
					}
				}
			}
		}

		if (viewToReturn != null) {
			return viewToReturn;
		}

		return waiter.waitForView(tag, index, timeout);
	}

	/**
	 * Returns a {@code View} with a given tag.
	 *
	 * @param tag the <code>tag</code> of the {@code View} to be returned
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @return a {@code View} with a given tag if available, <code>null</code> otherwise
	 */

	public View getView(Object tag, int index){
		return getView(tag, index, 0);
	}
}
