package com.robotium.solo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import junit.framework.Assert;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;


/**
 * 等待工具类
 * Contains various wait methods. Examples are: waitForText(),
 * waitForView().
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class Waiter {
	//Activity工具类
	private final ActivityUtils activityUtils;
	//视图获取工具类
	private final ViewFetcher viewFetcher;
	//视图查找工具类
	private final Searcher searcher;
	//滚动工具类
	private final Scroller scroller;
	//休息工具类
	private final Sleeper sleeper;
	//Android源码下的Instrumentation基类，时间发送器
	private final Instrumentation instrumentation;


	/**
	 * Constructs this object.
	 *
	 * @param instrumentation the {@code Instrumentation} object
	 * @param activityUtils the {@code ActivityUtils} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param searcher the {@code Searcher} instance
	 * @param scroller the {@code Scroller} instance
	 * @param sleeper the {@code Sleeper} instance
	 */

	public Waiter(Instrumentation instrumentation, ActivityUtils activityUtils, ViewFetcher viewFetcher, Searcher searcher, Scroller scroller, Sleeper sleeper){
		this.instrumentation = instrumentation;
		this.activityUtils = activityUtils;
		this.viewFetcher = viewFetcher;
		this.searcher = searcher;
		this.scroller = scroller;
		this.sleeper = sleeper;		
	}

	/**
	 * 等待这个名字的Activity出现，等待时间是10s
	 * Waits for the given {@link Activity}.
	 *
	 * @param name the name of the {@code Activity} to wait for e.g. {@code "MyActivity"}
	 * @return {@code true} if {@code Activity} appears before the timeout and {@code false} if it does not
	 *
	 */

	public boolean waitForActivity(String name){
		return waitForActivity(name, Timeout.getSmallTimeout());
	}

	/**
	 * 等待给定Activity名的Activity在给定时间内出现，如果出现则返回true，如果未出现则返回false
	 * Waits for the given {@link Activity}.
	 *
	 * @param name the name of the {@code Activity} to wait for e.g. {@code "MyActivity"}
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if {@code Activity} appears before the timeout and {@code false} if it does not
	 *
	 */

	public boolean waitForActivity(String name, int timeout){
		//通过isActivityMatching方法比较Activity名是否一致
		if(isActivityMatching(activityUtils.getCurrentActivity(false, false), name)){
			return true;
		}
		
		boolean foundActivity = false;
		//Activity监视器
		ActivityMonitor activityMonitor = getActivityMonitor();
		//从系统获取当前时间
		long currentTime = SystemClock.uptimeMillis();
		//当前时间+给定时间=最后时间；
		final long endTime = currentTime + timeout;
		//当前时间<小余最后时间，一直循环
		while(currentTime < endTime){
			//通过activityMonitor实例的waitForActivityWithTimeout方法获取当前Activity
			Activity currentActivity = activityMonitor.waitForActivityWithTimeout(endTime - currentTime);
			//比较Activity名是否一致
			if(isActivityMatching(currentActivity, name)){
				foundActivity = true;
				break;
			}	
			currentTime = SystemClock.uptimeMillis();
		}
		//删除activityMonitor
		removeMonitor(activityMonitor);
		return foundActivity;
	}
	
	/**
	 * 比较当前activity的名字和指定字符串是否相等。
	 * Compares Activity names.
	 * 
	 * @param currentActivity the Activity that is currently active
	 * @param name the name to compare
	 * 
	 * @return true if the Activity names match
	 */
	private boolean isActivityMatching(Activity currentActivity, String name){
		if(currentActivity != null && currentActivity.getClass().getSimpleName().equals(name)) {
			return true;
		}
		return false;
	}
	

	/**
	 * 等待给予的Activity类出现
	 * Waits for the given {@link Activity}.
	 *
	 * @param activityClass the class of the {@code Activity} to wait for
	 * @return {@code true} if {@code Activity} appears before the timeout and {@code false} if it does not
	 *
	 */

	public boolean waitForActivity(Class<? extends Activity> activityClass){
		//等待给予的Activity类出现，并设置10s时间
		return waitForActivity(activityClass, Timeout.getSmallTimeout());
	}

	/**
	 * 等待给予的Activity类在给予的时间内出现
	 * Waits for the given {@link Activity}.
	 *
	 * @param activityClass the class of the {@code Activity} to wait for
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if {@code Activity} appears before the timeout and {@code false} if it does not
	 *
	 */

	public boolean waitForActivity(Class<? extends Activity> activityClass, int timeout){
		//先匹配给予的activity类和获取的当前Activity类是否一致，一致则返回true，即为找到
		if(isActivityMatching(activityClass, activityUtils.getCurrentActivity(false, false))){
			return true;
		}
		
		//如果没找到，继续寻找
		//定义找Activity的Boolean型变量，默认值false
		boolean foundActivity = false;
		//新建一个ActivityMonitor实例，Activity监视器
		ActivityMonitor activityMonitor = getActivityMonitor();
		//获取系统当前时间
		long currentTime = SystemClock.uptimeMillis();
		//算出结束时间
		final long endTime = currentTime + timeout;
		//在规定时间内继续寻找
		while(currentTime < endTime){
			//通过ActivityMonitor工具在规定时间内获取当前堆栈顶端的Activity
			Activity currentActivity = activityMonitor.waitForActivityWithTimeout(endTime - currentTime);
			//如果当前堆栈顶端的Activity不为空，和参数ActivityClass实例相等，返回true，并终止当前的循环
			if(currentActivity != null && currentActivity.getClass().equals(activityClass)) {
				foundActivity = true;
				break;
			}
			//重新赋值当前时间
			currentTime = SystemClock.uptimeMillis();
		}
		//删除activityMonitor监视器
		removeMonitor(activityMonitor);
		return foundActivity;
	}
	
	/**
	 * 比较当前activity是否等于给定activity
	 * Compares Activity classes.
	 * 
	 * @param activityClass the Activity class to compare	
	 * @param currentActivity the Activity that is currently active
	 * 
	 * @return true if Activity classes match
	 */
	private boolean isActivityMatching(Class<? extends Activity> activityClass, Activity currentActivity){
		if(currentActivity != null && currentActivity.getClass().equals(activityClass)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 新创造一个Activity监视器，并返回该实例
	 * Creates a new ActivityMonitor and returns it
	 * 
	 * @return an ActivityMonitor
	 */
	private ActivityMonitor getActivityMonitor(){
		//意图过滤器
		IntentFilter filter = null;
		//创造Activity监视器实例
		ActivityMonitor activityMonitor = instrumentation.addMonitor(filter, null, false);
		return activityMonitor;
	}
	
	
	/**
	 * 删除这个Activity监视器
	 * Removes the AcitivityMonitor
	 * 
	 * @param activityMonitor the ActivityMonitor to remove
	 */
	
	private void removeMonitor(ActivityMonitor activityMonitor){
		try{
			instrumentation.removeMonitor(activityMonitor);	
		}catch (Exception ignored) {}
	}

	/**
	 * 等待指定类型的 view是否出现,出现返回true,未出现返回false.该方法未带超时参数，易导致死循环，建议使用带超时参数的
	 * viewClass  view的 class类型
	 * index      期望view类型的数量
	 * sleep      true 等待500ms后查找,false  立即查找
	 * scroll     true 对于可滑动控件，未找到时滑动下刷新内容，false  不滑动
	 * Waits for a view to be shown.
	 * 
	 * @param viewClass the {@code View} class to wait for
	 * @param index the index of the view that is expected to be shown
	 * @param sleep true if should sleep
	 * @param scroll {@code true} if scrolling should be performed
	 * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
	 */

	public <T extends View> boolean waitForView(final Class<T> viewClass, final int index, boolean sleep, boolean scroll){
		// 临时views缓存
		Set<T> uniqueViews = new HashSet<T>();
		boolean foundMatchingView;
		//如果设置了 scroll 为true 直接调用改方法，无法找到则容易进入死循环
		while(true){
			//如果需要休息，则休眠500ms
			if(sleep)
				sleeper.sleep();
			//检查该查询条件是否可以检索到,未符合为 false，符合为true
			foundMatchingView = searcher.searchFor(uniqueViews, viewClass, index);
			//如果相等，返回true
			if(foundMatchingView)
				return true;
			//如果支持滚动，但滚动失败，返回false
			if(scroll && !scroller.scrollDown())
				return false;
			//如果不支持滚动，返回false
			if(!scroll)
				return false;
		}
	}

	/**
	 * 等待指定类型的 view是否出现,出现返回true,未出现返回false.该方法带超时参数
	 * Waits for a view to be shown.
	 * 
	 * @param viewClass the {@code View} class to wait for
	 * @param index the index of the view that is expected to be shown. 
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
	 */

	public <T extends View> boolean waitForView(final Class<T> viewClass, final int index, final int timeout, final boolean scroll){
		//views缓存容器
		Set<T> uniqueViews = new HashSet<T>();
		//技术出超时时间
		final long endTime = SystemClock.uptimeMillis() + timeout;
		//设置view找到标识
		boolean foundMatchingView;
		//在规定时间内循环，查找指定view类型的数量值是否和期望值相等
		while (SystemClock.uptimeMillis() < endTime) {
			sleeper.sleep();
			//找到指定VeiwClas的view，并计算出非重复view数量值是否和预期index值相等
			foundMatchingView =  searcher.searchFor(uniqueViews, viewClass, index);
			//如果找到并相等，返回true
			if(foundMatchingView)
				return true;
			//如果支持滑动，滑动下拉刷新
			if(scroll) 
				scroller.scrollDown();
		}
		//时间到后，未找到返回false
		return false;
	}



	/**
	 * 等待一组class类型中的任一class类型的view出现,超时为10s.
	 * Waits for two views to be shown.
	 *
	 * @param scrollMethod {@code true} if it's a method used for scrolling
	 * @param classes the classes to wait for 
	 * @return {@code true} if any of the views are shown and {@code false} if none of the views are shown before the timeout
	 */

	public <T extends View> boolean  waitForViews(boolean scrollMethod, Class<? extends T>... classes) {
		//计算出超时时间
		final long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();
		//在规定时间内循环
		while (SystemClock.uptimeMillis() < endTime) {
			//在给定的classes中，如果找到指定的view类型出现，则返回true
			for (Class<? extends T> classToWaitFor : classes) {
				if (waitForView(classToWaitFor, 0, false, false)) {
					return true;
				}
			}
			//如果可以滚动，则将滚动条滚动到最底部
			if(scrollMethod){
				scroller.scroll(Scroller.DOWN);
			}
			else {
				scroller.scrollDown();
			}
			sleeper.sleep();
		}
		return false;
	}


	/**
	 * 等地指定view出现，默认时间是20s
	 * Waits for a given view. Default timeout is 20 seconds.
	 * 
	 * @param view the view to wait for
	 * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
	 */

	public boolean waitForView(View view){
		//等待指定view出现
		View viewToWaitFor = waitForView(view, Timeout.getLargeTimeout(), true, true);
		//null判断
		if(viewToWaitFor != null) {
			return true;
		}
		
		return false;
		
	}

	/**
	 * 等待指定view出现
	 * view 指定等待的view
	 * timeout 超时时间
	 * Waits for a given view. 
	 * 
	 * @param view the view to wait for
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
	 */

	public View waitForView(View view, int timeout){
		return waitForView(view, timeout, true, true);
	}

	/**
	 * 等待指定的view出现
	 * view 指定的view
	 * timeout 超时时间,单位 ms
	 * scroll true需要拖动刷新可拖动控件,false 不拖动
 	 * checkIsShown true 调用view.isShown()检查是否为true ,false  不调用
	 * Waits for a given view.
	 * 
	 * @param view the view to wait for
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @param checkIsShown {@code true} if view.isShown() should be used
	 * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
	 */

	public View waitForView(View view, int timeout, boolean scroll, boolean checkIsShown){
		//计算超时时间
		long endTime = SystemClock.uptimeMillis() + timeout;
		//标记
		int retry = 0;
		//指定view非null判断
		if(view == null)
			return null;
		//在规定时间内循环
		while (SystemClock.uptimeMillis() < endTime) {
			//判断是否能寻找到匹配的指定view
			final boolean foundAnyMatchingView = searcher.searchFor(view);
			//如果view未出现,尝试5次以上后，找一个和view一样的view，返回
			if(checkIsShown && foundAnyMatchingView && !view.isShown()){
				//休息一下
				sleeper.sleepMini();
				retry++;
				//寻找一个和view相等的view
				View identicalView = viewFetcher.getIdenticalView(view);
				//如果view不是identicalView，把identicalView赋值给view
				if(identicalView != null && !view.equals(identicalView)){
					view = identicalView;
				}
				//返回view
				if(retry > 5){
					return view;
				}
				continue;
			}
			//如果view出现，返回view
			if (foundAnyMatchingView){
				return view;
			}
			//如果支持屏幕滚动，滚动屏幕
			if(scroll) {
				scroller.scrollDown();
			}

			sleeper.sleep();

		}
		return view;
	}

	/**
	 * 获取指定id，指定数量的view出现，可设置超时时间,如果超时时间设置为0,则默认改成10s,设置为负值则直接返回null
	 * Waits for a certain view.
	 * 
	 * @param view the id of the view to wait for
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @param timeout the timeout in milliseconds
	 * @return the specified View
	 */

	public View waitForView(int id, int index, int timeout){
		//时间为零，则默认超时最段时间
		if(timeout == 0){
			timeout = Timeout.getSmallTimeout();
		}
		//返回
		return waitForView(id, index, timeout, false);
	}

	/**
	 * 获取指定id指定index的view出现.可设置超时和是否可拖动刷新
	 * Waits for a certain view.
	 * 
	 * @param view the id of the view to wait for
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @return the specified View
	 */

	public View waitForView(int id, int index, int timeout, boolean scroll){
		//构建一个存放view的容器
		Set<View> uniqueViewsMatchingId = new HashSet<View>();
		//计算超时时间
		long endTime = SystemClock.uptimeMillis() + timeout;
		//在规定时间内循环
		while (SystemClock.uptimeMillis() <= endTime) {
			sleeper.sleep();
			// 遍历当前所有的view
			for (View view : viewFetcher.getAllViews(false)) {
				// 检查id,符合条件加入views缓存
				Integer idOfView = Integer.valueOf(view.getId());
				
				if (idOfView.equals(id)) {
					uniqueViewsMatchingId.add(view);
					// 已找到需求的index,返回当前的view
					if(uniqueViewsMatchingId.size() > index) {
						return view;
					}
				}
			}
			// 如果设置了拖动，调用拖动方法刷新控件内容
			if(scroll) 
				scroller.scrollDown();
		}
		// 未满足条件，返回false
		return null;
	}

	/**
	 * 等待指定view出现
	 * Waits for a certain view.
	 *
	 * @param tag the tag of the view to wait for
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @param timeout the timeout in milliseconds
	 * @return the specified View
	 */

	public View waitForView(Object tag, int index, int timeout){
		//时间为0，默认等待时间10s
		if(timeout == 0){
			timeout = Timeout.getSmallTimeout();
		}
		return waitForView(tag, index, timeout, false);
	}

	/**
	 * 等待指定view出现，并返回
	 * Waits for a certain view.
	 *
	 * @param tag the tag of the view to wait for
	 * @param index the index of the {@link View}. {@code 0} if only one is available
	 * @return the specified View
	 */

	public View waitForView(Object tag, int index, int timeout, boolean scroll){
		
		//Because https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/View.java#L17005-L17007
		if(tag == null) {
			return null;
		}
		//构建一个存储非重复view的容器
		Set<View> uniqueViewsMatchingId = new HashSet<View>();
		long endTime = SystemClock.uptimeMillis() + timeout;
		//规定时间内，循环
		while (SystemClock.uptimeMillis() <= endTime) {
			sleeper.sleep();
			//遍历所有view
			for (View view : viewFetcher.getAllViews(false)) {
				// 检查tag,符合条件加入views缓存
				if (tag.equals(view.getTag())) {
					uniqueViewsMatchingId.add(view);
					// 已找到需求的index,返回当前的view
					if(uniqueViewsMatchingId.size() > index) {
						return view;
					}
				}
			}
			//如果支持滑动，滑动屏幕刷新view内容
			if(scroll) {
				scroller.scrollDown();
			}
		}
		//如果没有符合条件的，返回null值
		return null;
	}

	/**
	 * 按照给定的By条件，查找满足条件的第minimumNumberOfMatches个WebElement,可设置超时时间和是否需要拖动滚动条刷新WebView内容
	 * Waits for a web element.
	 * 
	 * @param by the By object. Examples are By.id("id") and By.name("name")
	 * @param minimumNumberOfMatches the minimum number of matches that are expected to be shown. {@code 0} means any number of matches
	 * @param timeout the the amount of time in milliseconds to wait 
	 * @param scroll {@code true} if scrolling should be performed 
	 */

	public WebElement waitForWebElement(final By by, int minimumNumberOfMatches, int timeout, boolean scroll){
		//计算超时时间
		final long endTime = SystemClock.uptimeMillis() + timeout;
		//进入循环模式
		while (true) {	
			//判断是否超时
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			//超时
			if (timedOut){
				//打印非重复view的数量和web元素的数量日志，并退出
				searcher.logMatchesFound(by.getValue());
				return null;
			}
			sleeper.sleep();
			//查找到指定id的web元素
			WebElement webElementToReturn = searcher.searchForWebElement(by, minimumNumberOfMatches); 
			//非null值，则返回
			if(webElementToReturn != null)
				return webElementToReturn;

			if(scroll) {
				scroller.scrollDown();
			}
		}
	}


	/**
	 * 设置自定义的判定条件做等待,可设置超时时间
	 * Waits for a condition to be satisfied.
	 * 
	 * @param condition the condition to wait for
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if condition is satisfied and {@code false} if it is not satisfied before the timeout
	 */
	public boolean waitForCondition(Condition condition, int timeout){
		//计算出超时时间
		final long endTime = SystemClock.uptimeMillis() + timeout;
		//无线循环，直到规定时间到
		while (true) {
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			if (timedOut){
				return false;
			}
			
			sleeper.sleep();
			//如果条件满足，返回true
			if (condition.isSatisfied()){
				return true;
			}
		}
	}

	/**
	 * 获取指定text的TextView类型元素出现.默认超时20s,超时时间内未出现返回null,出现则返回对应的TextView
	 * Waits for a text to be shown. Default timeout is 20 seconds.
	 *
	 * @param text the text that needs to be shown, specified as a regular expression
	 * @return {@code true} if text is found and {@code false} if it is not found before the timeout
	 */

	public TextView waitForText(String text) {
		//等待text出现，默认时间20s
		return waitForText(text, 0, Timeout.getLargeTimeout(), true);
	}

	/**
	 * 获取指定 text的第expectedMinimumNumberOfMatches个 TextView出现并返回该TextView,可设置超时时间，如设置时间内未出现返回null
	 * Waits for a text to be shown.
	 *
	 * @param text the text that needs to be shown, specified as a regular expression
	 * @param expectedMinimumNumberOfMatches the minimum number of matches of text that must be shown. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if text is found and {@code false} if it is not found before the timeout
	 */

	public TextView waitForText(String text, int expectedMinimumNumberOfMatches, long timeout)
	{
		return waitForText(text, expectedMinimumNumberOfMatches, timeout, true);
	}

	/**
	 * 获取指定text的第expectedMinimumNumberOfMatches个TextView，可设置超时时间，是否支持滚动
	 * Waits for a text to be shown.
	 *
	 * @param text the text that needs to be shown, specified as a regular expression
	 * @param expectedMinimumNumberOfMatches the minimum number of matches of text that must be shown. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @return {@code true} if text is found and {@code false} if it is not found before the timeout
	 */

	public TextView waitForText(String text, int expectedMinimumNumberOfMatches, long timeout, boolean scroll) {
		return waitForText(TextView.class, text, expectedMinimumNumberOfMatches, timeout, scroll, false, true);	
	}

	/**
	 * 获取指定class类型，指定text内容的第expectedMinimumNumberOfMatches个 TextView，可以设置超时，是否支持滚动
	 * Waits for a text to be shown.
	 *
	 * @param classToFilterBy the class to filter by
	 * @param text the text that needs to be shown, specified as a regular expression
	 * @param expectedMinimumNumberOfMatches the minimum number of matches of text that must be shown. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @return {@code true} if text is found and {@code false} if it is not found before the timeout
	 */

	public <T extends TextView> T waitForText(Class<T> classToFilterBy, String text, int expectedMinimumNumberOfMatches, long timeout, boolean scroll) {
		return waitForText(classToFilterBy, text, expectedMinimumNumberOfMatches, timeout, scroll, false, true);	
	}

	/**
	 * 获取指定text的第expectedMinimumNumberOfMatches个TextView,可指定超时时间，是否需要拖动
	 * 是否过滤非可见view,是否需要超时后立马退出
	 * Waits for a text to be shown.
	 *
	 * @param text the text that needs to be shown, specified as a regular expression.
	 * @param expectedMinimumNumberOfMatches the minimum number of matches of text that must be shown. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @param onlyVisible {@code true} if only visible text views should be waited for
	 * @param hardStoppage {@code true} if search is to be stopped when timeout expires
	 * @return {@code true} if text is found and {@code false} if it is not found before the timeout
	 */

	public TextView waitForText(String text, int expectedMinimumNumberOfMatches, long timeout, boolean scroll, boolean onlyVisible, boolean hardStoppage) {
		return waitForText(TextView.class, text, expectedMinimumNumberOfMatches, timeout, scroll, onlyVisible, hardStoppage);
	}

	/**
	 * 获取指定clas类型和text的第expectedMinimumNumberOfMatches个view.
	 * classToFilterBy                  指定的class类型
	 * text                             指定的text内容
	 * expectedMinimumNumberOfMatches   view的index
	 * timeout                          超时时间，单位 ms
	 * scroll                           true对于可拖动控件拖动刷新，false 不拖动刷新
	 * onlyVisible                      true 过滤掉非可见的,false  不做过滤
	 * hardStoppage                     true 等所有操作完成后返回,false 超时后强制停止相关操作，立即返回
	 * Waits for a text to be shown.
	 *
	 * @param classToFilterBy the class to filter by
	 * @param text the text that needs to be shown, specified as a regular expression.
	 * @param expectedMinimumNumberOfMatches the minimum number of matches of text that must be shown. {@code 0} means any number of matches
	 * @param timeout the amount of time in milliseconds to wait
	 * @param scroll {@code true} if scrolling should be performed
	 * @param onlyVisible {@code true} if only visible text views should be waited for
	 * @param hardStoppage {@code true} if search is to be stopped when timeout expires
	 * @return {@code true} if text is found and {@code false} if it is not found before the timeout
	 */

	public <T extends TextView> T waitForText(Class<T> classToFilterBy, String text, int expectedMinimumNumberOfMatches, long timeout, boolean scroll, boolean onlyVisible, boolean hardStoppage) {
		//计算出超时的时间
		final long endTime = SystemClock.uptimeMillis() + timeout;
		//
		while (true) {
			//是否超时判断
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			if (timedOut){
				return null;
			}
			
			sleeper.sleep();
			// true  searcher方法调用中循环，直到超时退出，false  searcher方法中不循环执行只做一次判断
			if(!hardStoppage)
				timeout = 0;
			//寻找符合条件的textView
			final T textViewToReturn = searcher.searchFor(classToFilterBy, text, expectedMinimumNumberOfMatches, timeout, scroll, onlyVisible);
			//如果textView不为空，则返回
			if (textViewToReturn != null ){
				return textViewToReturn;
			}
		}
	}

	/**
	 * 等待并返回指定类型的第index个对象
	 * Waits for and returns a View.
	 * 
	 * @param index the index of the view
	 * @param classToFilterby the class to filter
	 * @return the specified View
	 */

	public <T extends View> T waitForAndGetView(int index, Class<T> classToFilterBy){
		//默认时间是？
		long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();
		//规定时间内找到第index个view
		while (SystemClock.uptimeMillis() <= endTime && !waitForView(classToFilterBy, index, true, true));
		//获取当前屏幕的非重复view数量
		int numberOfUniqueViews = searcher.getNumberOfUniqueViews();
		//去除看不见的view
		ArrayList<T> views = RobotiumUtils.removeInvisibleViews(viewFetcher.getCurrentViews(classToFilterBy, true));
		//计算出index的值
		if(views.size() < numberOfUniqueViews){
			int newIndex = index - (numberOfUniqueViews - views.size());
			if(newIndex >= 0)
				index = newIndex;
		}
		//获取指定index的view
		T view = null;
		try{
			view = views.get(index);
		}catch (IndexOutOfBoundsException exception) {
			//如果报异常，则记录日志并退出
			int match = index + 1;
			if(match > 1) {
				Assert.fail(match + " " + classToFilterBy.getSimpleName() +"s" + " are not found!");
			}
			else {
				Assert.fail(classToFilterBy.getSimpleName() + " is not found!");
			}
		}
		views = null;
		//返回
		return view;
	}

	/**
	 * 等待一个给定标签或ID的fragment出现，可设置超时时间
	 * 优先查找android.support.v4.app.Fragment
	 * 未找到再查找 android.app.Fragment
	 * 都未找到返回null
	 * Waits for a Fragment with a given tag or id to appear.
	 * 
	 * @param tag the name of the tag or null if no tag	
	 * @param id the id of the tag
	 * @param timeout the amount of time in milliseconds to wait
	 * @return true if fragment appears and false if it does not appear before the timeout
	 */

	public boolean waitForFragment(String tag, int id, int timeout){
		//计算出超时时间
		long endTime = SystemClock.uptimeMillis() + timeout;
		//规定时间内循环
		while (SystemClock.uptimeMillis() <= endTime) {
			// 查找 android.support.v4.app.Fragment ，找到返回 android.support.v4.app.Fragment ,未找到继续查找 android.app.Fragment
			if(getSupportFragment(tag, id) != null)
				return true;
			// 查找 android.app.Fragment
			if(getFragment(tag, id) != null)
				return true;
		}
		return false;
	}

	/**
	 * 返回一个指定tag或id的supportFragment，并返回
	 * Returns a SupportFragment with a given tag or id.
	 * 
	 * @param tag the tag of the SupportFragment or null if no tag
	 * @param id the id of the SupportFragment
	 * @return a SupportFragment with a given tag or id
	 */

	private Fragment getSupportFragment(String tag, int id){
		//申明一个fragemnetActivity
		FragmentActivity fragmentActivity = null;
		//通过activityUtils工具获取当前的activity
		try{
			fragmentActivity = (FragmentActivity) activityUtils.getCurrentActivity(false);
		}catch (Throwable ignored) {}
		//如果找到，则通过fragemneActivity工具获取fragement
		if(fragmentActivity != null){
			try{
				if(tag == null)
					return fragmentActivity.getSupportFragmentManager().findFragmentById(id);
				else
					return fragmentActivity.getSupportFragmentManager().findFragmentByTag(tag);
			}catch (NoSuchMethodError ignored) {}
		}
		//如果未找到，返回null
		return null;
	}

	/**
	 * 指定的日志信息是否在指定超时时间内打印
	 * logMessage   期望出现的日志信息
	 * timeout      超时时间，单位 ms
	 * Waits for a log message to appear.
	 * Requires read logs permission (android.permission.READ_LOGS) in AndroidManifest.xml of the application under test.
	 * 
	 * @param logMessage the log message to wait for
	 * @param timeout the amount of time in milliseconds to wait
	 * @return true if log message appears and false if it does not appear before the timeout
	 */

	public boolean waitForLogMessage(String logMessage, int timeout){
		StringBuilder stringBuilder = new StringBuilder();
		//计算出超时时间
		long endTime = SystemClock.uptimeMillis() + timeout;
		//在规定时间里循环
		while (SystemClock.uptimeMillis() <= endTime) {
			// 读取logcat内容检查指定内容是否出现
			if(getLog(stringBuilder).lastIndexOf(logMessage) != -1){
				return true;
			}
			sleeper.sleep();
		}
		//指定时间未找到，返回false
		return false;
	}

	/**
	 * 获取当前logcat 
	 * Returns the log in the given stringBuilder. 
	 * 
	 * @param stringBuilder the StringBuilder object to return the log in
	 * @return the log
	 */

	private StringBuilder getLog(StringBuilder stringBuilder) {
		Process p = null;
		BufferedReader reader = null;
		String line = null;  

		try {
			// read output from logcat
			p = Runtime.getRuntime().exec("logcat -d");
			reader = new BufferedReader(  
					new InputStreamReader(p.getInputStream())); 

			stringBuilder.setLength(0);
			while ((line = reader.readLine()) != null) {  
				stringBuilder.append(line); 
			}
			reader.close();

			// read error from logcat
			StringBuilder errorLog = new StringBuilder();
			reader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			errorLog.append("logcat returns error: ");
			while ((line = reader.readLine()) != null) {
				errorLog.append(line);
			}
			reader.close();

			// Exception would be thrown if we get exitValue without waiting for the process
			// to finish
			p.waitFor();

			// if exit value of logcat is non-zero, it means error
			if (p.exitValue() != 0) {
				destroy(p, reader);

				throw new Exception(errorLog.toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		destroy(p, reader);
		return stringBuilder;
	}

	/**
	 * 清空log
	 * Clears the log.
	 */

	public void clearLog(){
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("logcat -c");
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * 销毁进程，并关闭reader
	 * p      需要销毁的进程
	 * reader 需要关闭的BufferedReader
	 * Destroys the process and closes the BufferedReader.
	 * 
	 * @param p the process to destroy
	 * @param reader the BufferedReader to close
	 */

	private void destroy(Process p, BufferedReader reader){
		p.destroy();
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 查找指定tag id的 android.app.Fragment,未找到则返回null
	 * Returns a Fragment with a given tag or id.
	 * 
	 * @param tag the tag of the Fragment or null if no tag
	 * @param id the id of the Fragment
	 * @return a SupportFragment with a given tag or id
	 */

	private android.app.Fragment getFragment(String tag, int id){

		try{
			if(tag == null)
				return activityUtils.getCurrentActivity().getFragmentManager().findFragmentById(id);
			else
				return activityUtils.getCurrentActivity().getFragmentManager().findFragmentByTag(tag);
		}catch (Throwable ignored) {}

		return null;
	}
}
