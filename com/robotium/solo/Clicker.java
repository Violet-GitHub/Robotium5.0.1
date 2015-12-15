package com.robotium.solo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import junit.framework.Assert;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.TextView;

/**
 * 包含一系列点击方法，列如：clickOn()
 * Contains various click methods. Examples are: clickOn(),
 * clickOnText(), clickOnScreen().
 *
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

class Clicker {
	//定义一个常量，用于日志打印，标记这是robotium的东西
	private final String LOG_TAG = "Robotium";
	//Activity工具类
	private final ActivityUtils activityUtils;
	//view查找类
	private final ViewFetcher viewFetcher;
	//instrumentation，用于发送各种事件
	private final Instrumentation inst;
	//按键信息发送工具类
	private final Sender sender;
	//休眠，等待
	private final Sleeper sleeper;
	//等待工具类，用来判断view，Activity是否出现；
	private final Waiter waiter;
	//WebUtils操作工具类
	private final WebUtils webUtils;
	//弹框(对话框)工具类
	private final DialogUtils dialogUtils;
	//定义最小等待时间,单位ms；
	private final int MINI_WAIT = 300;
	//定义最大等待时间,单位ms；
	private final int WAIT_TIME = 1500;


	/**
	 * 构造方法，给申明的对象实例化
	 * Constructs this object.
	 *
	 * @param activityUtils the {@code ActivityUtils} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param sender the {@code Sender} instance
	 * @param inst the {@code android.app.Instrumentation} instance
	 * @param sleeper the {@code Sleeper} instance
	 * @param waiter the {@code Waiter} instance
	 * @param webUtils the {@code WebUtils} instance
	 * @param dialogUtils the {@code DialogUtils} instance
	 */

	public Clicker(ActivityUtils activityUtils, ViewFetcher viewFetcher, Sender sender, Instrumentation inst, Sleeper sleeper, Waiter waiter, WebUtils webUtils, DialogUtils dialogUtils) {

		this.activityUtils = activityUtils;
		this.viewFetcher = viewFetcher;
		this.sender = sender;
		this.inst = inst;
		this.sleeper = sleeper;
		this.waiter = waiter;
		this.webUtils = webUtils;
		this.dialogUtils = dialogUtils;
	}

	/**
	 * 短按指定view
	 * Clicks on a given coordinate on the screen.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */

	public void clickOnScreen(float x, float y, View view) {
		//设置标记，标记点击事件还未发送成功
		boolean successfull = false;
		int retry = 0;
		SecurityException ex = null;
		
		while(!successfull && retry < 20) {
			//构造点击事件
			long downTime = SystemClock.uptimeMillis();
			long eventTime = SystemClock.uptimeMillis();
			//按下事件
			MotionEvent event = MotionEvent.obtain(downTime, eventTime,
					MotionEvent.ACTION_DOWN, x, y, 0);
			//起来事件
			MotionEvent event2 = MotionEvent.obtain(downTime, eventTime,
					MotionEvent.ACTION_UP, x, y, 0);
			//发送按下、起来事件
			try{
				inst.sendPointerSync(event);
				inst.sendPointerSync(event2);
				//事件发送成功，修改标记为成功；
				successfull = true;
			}catch(SecurityException e){
				//事件发送失败，抛出异常
				ex = e;
				//可能是软键盘的影响，关起软键盘
				dialogUtils.hideSoftKeyboard(null, false, true);
				sleeper.sleep(MINI_WAIT);
				//再尝试一次
				retry++;
				//获取一个与view相等的view
				View identicalView = viewFetcher.getIdenticalView(view);
				if(identicalView != null){
					//返回指定view的点击坐标
					float[] xyToClick = getClickCoordinates(identicalView);
					x = xyToClick[0]; 
					y = xyToClick[1];
				}
			}
		}
		//如果尝试20次，还未成功，则不再尝试，记录日志异常，退出；
		if(!successfull) {
			Assert.fail("Click at ("+x+", "+y+") can not be completed! ("+(ex != null ? ex.getClass().getName()+": "+ex.getMessage() : "null")+")");
		}
	}

	/**
	 * 长按指定view，可设置长按时间
	 * Long clicks a given coordinate on the screen.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param time the amount of time to long click
	 */

	public void clickLongOnScreen(float x, float y, int time, View view) {
		//设置标记，初始标记还未发送成功；
		boolean successfull = false;
		//设置重试计数器
		int retry = 0;
		//申明一个异常
		SecurityException ex = null;
		//构造一个按下事件
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis();
		MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
		//如果事件发送失败，失败次数<20次，继续发送
		while(!successfull && retry < 20) {
			try{
				//发送点击事件
				inst.sendPointerSync(event);
				//事件发送成功，未报异常，标记状态为成功
				successfull = true;
				//休息100ms
				sleeper.sleep(MINI_WAIT);
			}catch(SecurityException e){
				//异常处理
				ex = e;
				//关闭可能导致异常的软键盘，屏蔽软键盘影响，继续尝试
				dialogUtils.hideSoftKeyboard(null, false, true);
				//休息100ms
				sleeper.sleep(MINI_WAIT);
				//计数器+1
				retry++;
				//获取一个与view相等的view
				View identicalView = viewFetcher.getIdenticalView(view);
				if(identicalView != null){
					//返回指定view的点击坐标,并赋值给参数x,y
					float[] xyToClick = getClickCoordinates(identicalView);
					x = xyToClick[0];
					y = xyToClick[1];
				}
			}
		}
		//发送20次，还未发送成功，则不再发送，记录日志异常，退出
		if(!successfull) {
			Assert.fail("Long click at ("+x+", "+y+") can not be completed! ("+(ex != null ? ex.getClass().getName()+": "+ex.getMessage() : "null")+")");
		}
		//构造一个移动事件,相对原先按下坐标滑动1个像素
		eventTime = SystemClock.uptimeMillis();
		event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x + 1.0f, y + 1.0f, 0);
		inst.sendPointerSync(event);
		//如果设置了长按事件，且给定的时间大于0，那么等待相应时间
		if(time > 0)
			sleeper.sleep(time);
		//如果设置了长按事件，且给定的时间小于0，那么使用默认长按时间的2.5倍
		else
			sleeper.sleep((int)(ViewConfiguration.getLongPressTimeout() * 2.5f));
		//构造一个松开事件
		eventTime = SystemClock.uptimeMillis();
		event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
		inst.sendPointerSync(event);
		//等待500ms
		sleeper.sleep();
	}


	/**
	 * 点击指定view
	 * Clicks on a given {@link View}.
	 *
	 * @param view the view that should be clicked
	 */

	public void clickOnScreen(View view) {
		//设置为非长按点击
		clickOnScreen(view, false, 0);
	}

	/**
	 * 用来点击指定view的方法，可设置是否长按，及长按时间
	 * view 指定的view
	 * longClick 为true长按，为false短按
	 * time 长按事件
	 * Private method used to click on a given view.
	 *
	 * @param view the view that should be clicked
	 * @param longClick true if the click should be a long click
	 * @param time the amount of time to long click
	 */

	public void clickOnScreen(View view, boolean longClick, int time) {
		//null值判断。为空，则记录为空日志
		if(view == null)
			Assert.fail("View is null and can therefore not be clicked!");
		//返回指定view的点击坐标，并赋值给局部变量x,y
		float[] xyToClick = getClickCoordinates(view);
		float x = xyToClick[0];
		float y = xyToClick[1];
		
		//如果在原点
		if(x == 0 || y == 0){
			//休息10ms
			sleeper.sleepMini();
			try {
				//获取一个与view相等的view，并赋值给参数view
				view = viewFetcher.getIdenticalView(view);
			} catch (Exception ignored){}
			//null值判断，不为空，则获取view的x,y坐标
			if(view != null){
				xyToClick = getClickCoordinates(view);
				x = xyToClick[0];
				y = xyToClick[1];
			}
		}
		//长按指定坐标点
		if (longClick)
			clickLongOnScreen(x, y, time, view);
		//短按指定坐标点
		else
			clickOnScreen(x, y, view);
	}	

	/**
	 * 返回指定view的点击坐标
	 * Returns click coordinates for the specified view.
	 * 
	 * @param view the view to get click coordinates from
	 * @return click coordinates for a specified view
	 */

	private float[] getClickCoordinates(View view){
		//休息200ms
		sleeper.sleep(200);
		
		int[] xyLocation = new int[2];
		float[] xyToClick = new float[2];
		//获取指定view的最左下角xy位置，并赋值给xyLocation数组
		view.getLocationOnScreen(xyLocation);
		//获取view的宽度
		final int viewWidth = view.getWidth();
		//获取view的高度
		final int viewHeight = view.getHeight();
		//计算view中心坐标点的x值
		final float x = xyLocation[0] + (viewWidth / 2.0f);
		//计算view中心坐标点的y值
		float y = xyLocation[1] + (viewHeight / 2.0f);
		//赋值给xyToClick数组
		xyToClick[0] = x;
		xyToClick[1] = y;
		//返回view的点击坐标点
		return xyToClick;
	}
	
	


	/**
	 * 长按指定text内容的第1个View,等待弹框出现，向下 按键index次，再点击回车键.确认
	 * Long clicks on a specific {@link TextView} and then selects
	 * an item from the context menu that appears. Will automatically scroll when needed.
	 *
	 * @param text the text that should be clicked on. The parameter <strong>will</strong> be interpreted as a regular expression.
	 * @param index the index of the menu item that should be pressed
	 */

	public void clickLongOnTextAndPress(String text, int index){
		//长按指定text的textView的第1个item，支持滚动
		clickOnText(text, true, 0, true, 0);
		//等待弹框打开
		dialogUtils.waitForDialogToOpen(Timeout.getSmallTimeout(), true);
		//发送向下按下事件
		try{
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		}catch(SecurityException e){
			//发生异常，记录错误日志
			Assert.fail("Can not press the context menu!");
		}
		//发送指定次数的向下按事件
		for(int i = 0; i < index; i++)
		{	//休息300ms
			sleeper.sleepMini();
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		}
		//发送确认事件
		inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
	}

	/**
	 * 打开菜单，或等待它并打开它
	 * Opens the menu and waits for it to open.
	 */

	private void openMenu(){
		//休息300ms
		sleeper.sleepMini();
		//如果200ms内等待打开菜单失败
		if(!dialogUtils.waitForDialogToOpen(MINI_WAIT, false)) {
			try{
				//发送菜单事件
				sender.sendKeyCode(KeyEvent.KEYCODE_MENU);
				//等待打开菜单
				dialogUtils.waitForDialogToOpen(WAIT_TIME, true);
			}catch(SecurityException e){
				//发生异常，则记录日志
				Assert.fail("Can not open the menu!");
			}
		}
	}

	/**
	 * 点击菜单中指定text的项
	 * Clicks on a menu item with a given text.
	 *
	 * @param text the menu text that should be clicked on. The parameter <strong>will</strong> be interpreted as a regular expression.
	 */

	public void clickOnMenuItem(String text)
	{	//打开菜单
		openMenu();
		//短按第二个item，支持滚动
		clickOnText(text, false, 1, true, 0);
	}

	/**
	 * 点击菜单中指定text的项，subMenu为true则当主菜单没找到，查找子菜单
	 * Clicks on a menu item with a given text.
	 *
	 * @param text the menu text that should be clicked on. The parameter <strong>will</strong> be interpreted as a regular expression.
	 * @param subMenu true if the menu item could be located in a sub menu
	 */

	public void clickOnMenuItem(String text, boolean subMenu){
		//休息300ms
		sleeper.sleepMini();
		//
		TextView textMore = null;
		//xy坐标数组
		int [] xy = new int[2];
		int x = 0;
		int y = 0;
		//如果弹框未出现
		if(!dialogUtils.waitForDialogToOpen(MINI_WAIT, false)) {
			try{
				//发送菜单
				sender.sendKeyCode(KeyEvent.KEYCODE_MENU);
				//等待弹框出现
				dialogUtils.waitForDialogToOpen(WAIT_TIME, true);
			}catch(SecurityException e){
				//抛出异常，记录日志，并退出
				Assert.fail("Can not open the menu!");
			}
		}
		//等待显示指定text的view出现，并获取该view
		boolean textShown = waiter.waitForText(text, 1, WAIT_TIME, true) != null;
		//如果设置了子级目录，那么继续查找子级菜单,不关注指定内容，只按照数量大于5,那么找出最右边的菜单点击
		if(subMenu && (viewFetcher.getCurrentViews(TextView.class, true).size() > 5) && !textShown){
			
			for(TextView textView : viewFetcher.getCurrentViews(TextView.class, true)){
				x = xy[0];
				y = xy[1];
				textView.getLocationOnScreen(xy);
				if(xy[0] > x || xy[1] > y)
					textMore = textView;
			}
		}
		//如果找到，发送点击事件
		if(textMore != null)
			//点击view
			clickOnScreen(textMore);
		// 菜单中未找到，可能展示在其他控件中，尝试发送点击事件。可能用户误操作api,猜测用户意图，给予修正
		clickOnText(text, false, 1, true, 0);
	}

	/**
	 * 点击actionBar，按照id查找
	 * Clicks on an ActionBar item with a given resource id
	 *
	 * @param resourceId the R.id of the ActionBar item
	 */

	public void clickOnActionBarItem(int resourceId){
		sleeper.sleep();
		Activity activity = activityUtils.getCurrentActivity();
		if(activity != null){
			//发送点击事件
			inst.invokeMenuActionSync(activity, resourceId, 0);
		}
	}

	/**
	 * 点击ActionBar的huome或者up键
	 * Clicks on an ActionBar Home/Up button.
	 */

	public void clickOnActionBarHomeButton() {
		//获取当前Activity
		Activity activity = activityUtils.getCurrentActivity();
		MenuItem homeMenuItem = null;
		//通过反射，构造menuItem对象
		try {
			Class<?> cls = Class.forName("com.android.internal.view.menu.ActionMenuItem");
			Class<?> partypes[] = new Class[6];
			partypes[0] = Context.class;
			partypes[1] = Integer.TYPE;
			partypes[2] = Integer.TYPE;
			partypes[3] = Integer.TYPE;
			partypes[4] = Integer.TYPE;
			partypes[5] = CharSequence.class;
			Constructor<?> ct = cls.getConstructor(partypes);
			Object argList[] = new Object[6];
			argList[0] = activity;
			argList[1] = 0;
			argList[2] = android.R.id.home;
			argList[3] = 0;
			argList[4] = 0;
			argList[5] = "";
			//构造ActionBar的home
			homeMenuItem = (MenuItem) ct.newInstance(argList);
		} catch (Exception ex) {
			Log.d(LOG_TAG, "Can not find methods to invoke Home button!");
		}
		//发送home的事件
		if (homeMenuItem != null) {
			try{
				activity.getWindow().getCallback().onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, homeMenuItem);
			}catch(Exception ignored) {}
		}
	}

	/**
	 * 点击指定条件的第match个WebElement,可以设置是否使用js发送点击事件.设置焦点到对应的WebElement
	 * by 指定条件
	 * match web元素中的第几个
	 * scroll 是否支持滚动
	 * useJavaScriptToClick 是否可以用JavaScript发送事件
	 * Clicks on a web element using the given By method.
	 *
	 * @param by the By object e.g. By.id("id");
	 * @param match if multiple objects match, this determines which one will be clicked
	 * @param scroll true if scrolling should be performed
	 * @param useJavaScriptToClick true if click should be perfomed through JavaScript
	 */

	public void clickOnWebElement(By by, int match, boolean scroll, boolean useJavaScriptToClick){
		WebElement webElement = null;
		//为true，则 设置了js点击，那么调用js点击
		if(useJavaScriptToClick){
			//查找指定的webElement
			webElement = waiter.waitForWebElement(by, match, Timeout.getSmallTimeout(), false);
			//未找到，提示异常
			if(webElement == null){
				Assert.fail("WebElement with " + webUtils.splitNameByUpperCase(by.getClass().getSimpleName()) + ": '" + by.getValue() + "' is not found!");
			}
			//通过JavaScript发送点击事件
			webUtils.executeJavaScript(by, true);
			return;
		}
		//查找指定的webView
		WebElement webElementToClick = waiter.waitForWebElement(by, match, Timeout.getSmallTimeout(), scroll);
		//未找到，提示异常
		if(webElementToClick == null){
			if(match > 1) {
				Assert.fail(match + " WebElements with " + webUtils.splitNameByUpperCase(by.getClass().getSimpleName()) + ": '" + by.getValue() + "' are not found!");
			}
			else {
				Assert.fail("WebElement with " + webUtils.splitNameByUpperCase(by.getClass().getSimpleName()) + ": '" + by.getValue() + "' is not found!");
			}
		}
		//点击获取到的web元素
		clickOnScreen(webElementToClick.getLocationX(), webElementToClick.getLocationY(), null);
	}


	/**
	 * 点击一个展示给定text的textView，可以设置是否长按，长按时间，符合条件的第几个
	 * regex 需要被点击的文字
	 * longClick 是否长按
	 * match 第几个
	 * scroll 是否支持滚动
	 * time 长按时间
	 * 
	 * Clicks on a specific {@link TextView} displaying a given text.
	 *
	 * @param regex the text that should be clicked on. The parameter <strong>will</strong> be interpreted as a regular expression.
	 * @param longClick {@code true} if the click should be a long click
	 * @param match the regex match that should be clicked on
	 * @param scroll true if scrolling should be performed
	 * @param time the amount of time to long click
	 */

	public void clickOnText(String regex, boolean longClick, int match, boolean scroll, int time) {
		//等待并获取指定条件的textToClick
		TextView textToClick = waiter.waitForText(regex, match, Timeout.getSmallTimeout(), scroll, true, false);
		//如果找到，长按指定条件的textToClick
		if (textToClick != null) {
			clickOnScreen(textToClick, longClick, time);
		}
		//如果未找到
		else {
			//设置match>1,那么提示异常，并退出
			if(match > 1){
				Assert.fail(match + " matches of text string: '" + regex +  "' are not found!");
			}
			//如果match<=1,则打印出当前所有的TextView类控件信息,并退出
			else{
				ArrayList<TextView> allTextViews = RobotiumUtils.removeInvisibleViews(viewFetcher.getCurrentViews(TextView.class, true));
				allTextViews.addAll((Collection<? extends TextView>) webUtils.getTextViewsFromWebView());

				for (TextView textView : allTextViews) {
					Log.d(LOG_TAG, "'" + regex + "' not found. Have found: '" + textView.getText() + "'");
				}
				allTextViews = null;
				Assert.fail("Text string: '" + regex + "' is not found!");
			}
		}
	}


	/**
	 * 点击一个指定类型和文本的view
	 * Clicks on a {@code View} of a specific class, with a given text.
	 *
	 * @param viewClass what kind of {@code View} to click, e.g. {@code Button.class} or {@code TextView.class}
	 * @param nameRegex the name of the view presented to the user. The parameter <strong>will</strong> be interpreted as a regular expression.
	 */

	public <T extends TextView> void clickOn(Class<T> viewClass, String nameRegex) {
		//等待并获取指定文本和类型的view
		T viewToClick = (T) waiter.waitForText(viewClass, nameRegex, 0, Timeout.getSmallTimeout(), true, true, false);
		//如果找到，点击找到的view
		if (viewToClick != null) {
			clickOnScreen(viewToClick);
		//如果未找到，打印日志，记录当前所有textview，并退出；
		} else {
			ArrayList <T> allTextViews = RobotiumUtils.removeInvisibleViews(viewFetcher.getCurrentViews(viewClass, true));

			for (T view : allTextViews) {
				Log.d(LOG_TAG, "'" + nameRegex + "' not found. Have found: '" + view.getText() + "'");
			}
			Assert.fail(viewClass.getSimpleName() + " with text: '" + nameRegex + "' is not found!");
		}
	}

	/**
	 * 点击指定类型的第index个view
	 * Clicks on a {@code View} of a specific class, with a certain index.
	 *
	 * @param viewClass what kind of {@code View} to click, e.g. {@code Button.class} or {@code ImageView.class}
	 * @param index the index of the {@code View} to be clicked, within {@code View}s of the specified class
	 */

	public <T extends View> void clickOn(Class<T> viewClass, int index) {
		//获取指定类型的第index个view，并点击该view
		clickOnScreen(waiter.waitForAndGetView(index, viewClass));
	}


	/**
	 * 点击列表第一行，并返回这一行中所有的textView
	 * Clicks on a certain list line and returns the {@link TextView}s that
	 * the list line is showing. Will use the first list it finds.
	 *
	 * @param line the line that should be clicked
	 * @return a {@code List} of the {@code TextView}s located in the list line
	 */

	public ArrayList<TextView> clickInList(int line) {
		//返回第一行的textView
		return clickInList(line, 0, false, 0);
	}

	/**
	 * 点击指定列表的指定列表行，并返回这个列表行所有的textView类型的view
	 * Clicks on a certain list line on a specified List and
	 * returns the {@link TextView}s that the list line is showing.
	 *
	 * @param line the line that should be clicked
	 * @param index the index of the list. E.g. Index 1 if two lists are available
	 * @return an {@code ArrayList} of the {@code TextView}s located in the list line
	 */

	public ArrayList<TextView> clickInList(int line, int index, boolean longClick, int time) {
		final long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();

		int lineIndex = line - 1;
		if(lineIndex < 0)
			lineIndex = 0;

		ArrayList<View> views = new ArrayList<View>();
		//等待并获取absListView对象
		final AbsListView absListView = waiter.waitForAndGetView(index, AbsListView.class);
		//null值判断
		if(absListView == null)
			Assert.fail("AbsListView is null!");
		//下坐标值index是否超过判断
		failIfIndexHigherThenChildCount(absListView, lineIndex, endTime);
		//返回absListView对象中指定列表行中的指定view
		View viewOnLine = getViewOnAbsListLine(absListView, index, lineIndex);
		//null判断
		if(viewOnLine != null){
			//返回view中的所有view
			views = viewFetcher.getViews(viewOnLine, true);
			//去掉view中的隐藏view
			views = RobotiumUtils.removeInvisibleViews(views);
			//长按viewOnLine
			clickOnScreen(viewOnLine, longClick, time);
		}
		//过滤出views中的所有TextView，并返回
		return RobotiumUtils.filterViews(TextView.class, views);
	}
	
	/**
	 * 点击获取到的第一个列表行中的所有TextView
	 * Clicks on a certain list line and returns the {@link TextView}s that
	 * the list line is showing. Will use the first list it finds.
	 *
	 * @param line the line that should be clicked
	 * @return a {@code List} of the {@code TextView}s located in the list line
	 */

	public ArrayList<TextView> clickInRecyclerView(int line) {
		return clickInRecyclerView(line, 0, false, 0);
	}

	
	/**
	 * 在指定列表中点击指定列表行，并返回该列表行中的所有textView
	 * Clicks on a certain list line on a specified List and
	 * returns the {@link TextView}s that the list line is showing.
	 *
	 * @param itemIndex the item index that should be clicked
	 * @param recyclerViewIndex the index of the RecyclerView. E.g. Index 1 if two RecyclerViews are available
	 * @return an {@code ArrayList} of the {@code TextView}s located in the list line
	 */

	public ArrayList<TextView> clickInRecyclerView(int itemIndex, int recyclerViewIndex, boolean longClick, int time) {
		View viewOnLine = null;
		final long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();

		if(itemIndex < 0)
			itemIndex = 0;
		
		ArrayList<View> views = new ArrayList<View>();
		//获取所有viewGroup
		ViewGroup recyclerView = viewFetcher.getRecyclerView(recyclerViewIndex, Timeout.getSmallTimeout());
		
		if(recyclerView == null){
			Assert.fail("RecyclerView is not found!");
		}
		else{
			//判断itemIndex的数值是否大于recyclerView的子试图值
			failIfIndexHigherThenChildCount(recyclerView, itemIndex, endTime);
			//获取指定位置的view
			viewOnLine = getViewOnRecyclerItemIndex((ViewGroup) recyclerView, recyclerViewIndex, itemIndex);
		}
		
		if(viewOnLine != null){
			//获取viewGroup中的所有子试图，包括自己
			views = viewFetcher.getViews(viewOnLine, true);
			//去掉隐藏view
			views = RobotiumUtils.removeInvisibleViews(views);
			//点击指定view
			clickOnScreen(viewOnLine, longClick, time);
		}
		//并返回该列表行中的所有textView
		return RobotiumUtils.filterViews(TextView.class, views);
	}
	
	//下坐标index值是否大于view总数的判断，并在时间到后，打印日志
	private void failIfIndexHigherThenChildCount(ViewGroup viewGroup, int index, long endTime){
		//当下坐标>view总数，在等到时间到了，再打印日志
		while(index > viewGroup.getChildCount()){
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			if (timedOut){
				int numberOfLines = viewGroup.getChildCount();
				Assert.fail("Can not click on line number " + index + " as there are only " + numberOfLines + " lines available");
			}
			sleeper.sleep();
		}
	}
	

	/**
	 * 返回ABSListView的指定行所有view
	 * Returns the view in the specified list line
	 * 
	 * @param absListView the ListView to use
	 * @param index the index of the list. E.g. Index 1 if two lists are available
	 * @param lineIndex the line index of the View
	 * @return the View located at a specified list line
	 */

	private View getViewOnAbsListLine(AbsListView absListView, int index, int lineIndex){
		final long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();
		//获取指定下坐标位置的子试图
		View view = absListView.getChildAt(lineIndex);
		//null判断
		while(view == null){
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			//如果超时，则打印日志，并退出
			if (timedOut){
				Assert.fail("View is null and can therefore not be clicked!");
			}
			sleeper.sleep();
			//获取等同absListView的试图
			absListView = (AbsListView) viewFetcher.getIdenticalView(absListView);
			//null判断，等待absListView出现
			if(absListView == null){
				absListView = waiter.waitForAndGetView(index, AbsListView.class);
			}
			//获取指定位置的view
			view = absListView.getChildAt(lineIndex);
		}
		return view;
	}
	
	/**
	 * 返回指定位置的view
	 * Returns the view in the specified item index
	 * 
	 * @param recyclerView the RecyclerView to use
	 * @param itemIndex the item index of the View
	 * @return the View located at a specified item index
	 */

	private View getViewOnRecyclerItemIndex(ViewGroup recyclerView, int recyclerViewIndex, int itemIndex){
		final long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();
		View view = recyclerView.getChildAt(itemIndex);

		while(view == null){
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			if (timedOut){
				Assert.fail("View is null and can therefore not be clicked!");
			}

			sleeper.sleep();
			recyclerView = (ViewGroup) viewFetcher.getIdenticalView(recyclerView);

			if(recyclerView == null){
				recyclerView = (ViewGroup) viewFetcher.getRecyclerView(false, recyclerViewIndex);
			}

			if(recyclerView != null){
				view = recyclerView.getChildAt(itemIndex);
			}
		}
		return view;
	}
	
	
}
