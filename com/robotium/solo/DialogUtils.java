package com.robotium.solo;


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


/**
 * 弹框工具类，
 * Contains the waitForDialogToClose() method.
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class DialogUtils {
	//事件发送器
	private final Instrumentation instrumentation;
	//Activity工具类
	private final ActivityUtils activityUtils;
	//视图获取工具类
	private final ViewFetcher viewFetcher;
	//休眠工具类
	private final Sleeper sleeper;
	//超时对话框自动关闭时间
	private final static int TIMEOUT_DIALOG_TO_CLOSE = 1000;
	//最小休眠时间
	private final int MINISLEEP = 200;

	/**
	 * Constructs this object.
	 * 
	 * @param activityUtils the {@code ActivityUtils} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param sleeper the {@code Sleeper} instance
	 */

	public DialogUtils(Instrumentation instrumentation, ActivityUtils activityUtils, ViewFetcher viewFetcher, Sleeper sleeper) {
		this.instrumentation = instrumentation;
		this.activityUtils = activityUtils;
		this.viewFetcher = viewFetcher;
		this.sleeper = sleeper;
	}


	/**
	 * 在规定时间内，检查弹框是否关闭了
	 * Waits for a {@link android.app.Dialog} to close.
	 *
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if the {@code Dialog} is closed before the timeout and {@code false} if it is not closed
	 */

	public boolean waitForDialogToClose(long timeout) {
		//在固定时间里等待对话框打开
		waitForDialogToOpen(TIMEOUT_DIALOG_TO_CLOSE, false);
		//得出超时时间
		final long endTime = SystemClock.uptimeMillis() + timeout;
		//在规定时间内，检查弹框是否关闭了
		while (SystemClock.uptimeMillis() < endTime) {

			if(!isDialogOpen()){
				return true;
			}
			sleeper.sleep(MINISLEEP);
		}
		return false;
	}



	/**
	 * 等待一个弹框被打开
	 * timeout 超时时间
	 * sleepFirst 是否需要先等待500ms，再做检查
	 * Waits for a {@link android.app.Dialog} to open.
	 *
	 * @param timeout the amount of time in milliseconds to wait
	 * @return {@code true} if the {@code Dialog} is opened before the timeout and {@code false} if it is not opened
	 */

	public boolean waitForDialogToOpen(long timeout, boolean sleepFirst) {
		//计算出超时时间
		final long endTime = SystemClock.uptimeMillis() + timeout;
		//判断是否已打开
		boolean dialogIsOpen = isDialogOpen();
		//是否需要先等待500ms
		if(sleepFirst)
			sleeper.sleep();
		//检查对话框如果已打开，返回true
		if(dialogIsOpen){
			return true;
		}
		//在规定时间内，循环检查对话框是否已打开
		while (SystemClock.uptimeMillis() < endTime) {

			if(isDialogOpen()){
				return true;
			}
			sleeper.sleepMini();
		}
		return false;
	}

	/**
	 * 检查一个对话框是否已打开
	 * Checks if a dialog is open. 
	 * 
	 * @return true if dialog is open
	 */

	private boolean isDialogOpen(){
		//获取当前Activity
		final Activity activity = activityUtils.getCurrentActivity(false);
		//获取当前Activity上的所有试图
		final View[] views = viewFetcher.getWindowDecorViews();
		//获取views中的DecorView对象，DecorView是根
		View view = viewFetcher.getRecentDecorView(views);	
		// 遍历检查当前Activity上是否有打开的弹框，但这个弹框不是DecorView
		if(!isDialog(activity, view)){
			for(View v : views){
				if(isDialog(activity, v)){
					return true;
				}
			}
		}
		else {
			return true;
		}
		return false;
	}
	
	/**
	 * 判断decorView是否是给定activity的，即检查弹框是否是当前activity的
	 * Checks that the specified DecorView and the Activity DecorView are not equal.
	 * 
	 * @param activity the activity which DecorView is to be compared
	 * @param decorView the DecorView to compare
	 * @return true if not equal
	 */
	
	private boolean isDialog(Activity activity, View decorView){
		//null判断，检查decorView是都可见的，不可见直接返回false
		if(decorView == null || !decorView.isShown() || activity == null){
			return false;
		}
		// 获取Context
		Context viewContext = null;
		if(decorView != null){
			viewContext = decorView.getContext();
		}
		// 获取需要的基础Context
		if (viewContext instanceof ContextThemeWrapper) {
			ContextThemeWrapper ctw = (ContextThemeWrapper) viewContext;
			viewContext = ctw.getBaseContext();
		}
		// 获取activity对应的Context
		Context activityContext = activity;
		Context activityBaseContext = activity.getBaseContext();
		// 检查Context 是否是一致的,并且 activity不是在弹框中的
		return (activityContext.equals(viewContext) || activityBaseContext.equals(viewContext)) && (decorView != activity.getWindow().getDecorView());
	}

	/**
	 * 隐藏软键盘
	 * editText 指定的编辑框
	 * shouldSleepFirst 是否要先等待500ms再操作
	 * shouldSleepAfter 执行完后是否要等待500ms再返回,仅对传入editText非null有效
	 * Hides the soft keyboard
	 * 
	 * @param shouldSleepFirst whether to sleep a default pause first
	 * @param shouldSleepAfter whether to sleep a default pause after
	 */

	public void hideSoftKeyboard(EditText editText, boolean shouldSleepFirst, boolean shouldSleepAfter) {
		InputMethodManager inputMethodManager;
		//获取当前activity
		Activity activity = activityUtils.getCurrentActivity(shouldSleepFirst);
		//为空，则通过事件发送器获取控制管理类
		if(activity == null){
			inputMethodManager = (InputMethodManager)instrumentation.getTargetContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		}
		//不为空，则通过activity获取当前控制管理服务
		else {
			inputMethodManager = (InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		}
		//如果editText不为空，则通过当前控制管理类调用隐藏软键盘方法，并退出
		if(editText != null) {
			inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
			return;
		}
		//如果EditText为空，则获取当前的焦点View
		View focusedView = activity.getCurrentFocus();
		// 如果获取的 View不是EditText，则在当前activity上找到最新EditTextview，并赋值给焦点view
		if(!(focusedView instanceof EditText)) {
			//获取最新可见试图，并赋值给焦点view
			EditText freshestEditText = viewFetcher.getFreshestView(viewFetcher.getCurrentViews(EditText.class, true));
			if(freshestEditText != null){
				focusedView = freshestEditText;
			}
		}
		//确认焦点view非空，则通过事件发送器隐藏软键盘
		if(focusedView != null) {
			inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
		}
		// 如果设置了等待，那么等待500ms后返回
		if(shouldSleepAfter){
			sleeper.sleep();
		}
	}
}
