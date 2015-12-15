package com.robotium.solo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.Timer;
import com.robotium.solo.Solo.Config;
import junit.framework.Assert;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;


/**
 * ActivityUtils工具类，Activity查找，监测，堆栈相关方法
 * Contains activity related methods. Examples are:
 * getCurrentActivity(), getActivityMonitor(), setActivityOrientation(int orientation).
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class ActivityUtils {
	//
	private final Config config;
	//Instrument 各种事件发送强大利器
	private final Instrumentation inst;
	//所有的activity变化都可以监控
	private ActivityMonitor activityMonitor;
	//Activity类
	private Activity activity;
	//休息工具类
	private final Sleeper sleeper;
	//log标识
	private final String LOG_TAG = "Robotium";
	//最小休眠时间100ms
	private final int MINISLEEP = 100;
	//activity堆栈，用于存放所有开启状态的activity,采用WeakReference,避免对GC产生影响
	private Stack<WeakReference<Activity>> activityStack;
	//Activity对象引用变量，使用WeakReference，避免对GC产生影响
	private WeakReference<Activity> weakActivityReference;
	//堆栈存储activity的名字
	private Stack<String> activitiesStoredInActivityStack;
	//定时器
	private Timer activitySyncTimer;
	//登记Activity是否被执行的标识
	private boolean registerActivities;
	//线程
	Thread activityThread;

	/**
	 * Constructs this object.
	 *
	 * @param config the {@code Config} instance	
	 * @param inst the {@code Instrumentation} instance.
	 * @param activity the start {@code Activity}
	 * @param sleeper the {@code Sleeper} instance
	 */

	public ActivityUtils(Config config, Instrumentation inst, Activity activity, Sleeper sleeper) {
		this.config = config;
		this.inst = inst;
		this.activity = activity;
		this.sleeper = sleeper;
		createStackAndPushStartActivity();
		activitySyncTimer = new Timer();
		activitiesStoredInActivityStack = new Stack<String>();
		setupActivityMonitor();
		setupActivityStackListener();
	}



	/**
	 * 创建一个堆栈，用于存放创建的activity.因为 activity创建了新的老的就在后面了，所以使用堆栈的先进后出功能
	 * Creates a new activity stack and pushes the start activity. 
	 */

	private void createStackAndPushStartActivity(){
		//初始化一个堆栈
		activityStack = new Stack<WeakReference<Activity>>();
		//如果构造函数传入的activity不为null，那么假如堆栈最为当前最新的activity
		if (activity != null && config.trackActivities){
			WeakReference<Activity> weakReference = new WeakReference<Activity>(activity);
			activity = null;
			activityStack.push(weakReference);
		}
	}
	

	/**
	 * 返回一个装有所有打开的活跃的Activities的list列表
	 * Returns a {@code List} of all the opened/active activities.
	 * 
	 * @return a {@code List} of all the opened/active activities
	 */

	public ArrayList<Activity> getAllOpenedActivities()
	{	
		//创建一个装activity的ArrayList数组列表
		ArrayList<Activity> activities = new ArrayList<Activity>();
		//申明一个遍历Activity的activityStackIterator遍历器，并将遍历的结果赋值给这个遍历器
		Iterator<WeakReference<Activity>> activityStackIterator = activityStack.iterator();
		//当遍历器中还存在下一个
		while(activityStackIterator.hasNext()){
			//申明一个Activity复合型变量，并将遍历中的Activity项赋值给Activity变量
			Activity  activity = activityStackIterator.next().get();
		    //确认Activity不为空值
			if(activity!=null)
				//把Activity实例添加到Activity的数组容器中
				activities.add(activity);
		}
		//返回这个装Activity的容器；
		return activities;
	}

	/**
	 * 通过instrument构造一个activityMonitor用于监控activity的创建
	 * This is were the activityMonitor is set up. The monitor will keep check
	 * for the currently active activity.
	 */

	private void setupActivityMonitor() {
		
		if(config.trackActivities){
			try {
				//为了addMonitor方法需要，创建一个null对象
				IntentFilter filter = null;
				//获取一个activityMonitor
				activityMonitor = inst.addMonitor(filter, null, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 设置登记Activity是否被执行
	 * Returns true if registration of Activites should be performed
	 * 
	 * @return true if registration of Activities should be performed
	 */
	
	public boolean shouldRegisterActivities() {
		return registerActivities;
	}


	/**
	 * 如果登记Activity被执行了，设置registerActivity为true
	 * Set true if registration of Activities should be performed
	 * @param registerActivities true if registration of Activities should be performed
	 */
	
	public void setRegisterActivities(boolean registerActivities) {
		this.registerActivities = registerActivities;
	}

	/**
	 * 堆栈监听器，
	 * This is were the activityStack listener is set up. The listener will keep track of the
	 * opened activities and their positions.
	 */

	private void setupActivityStackListener() {
		//null判断
		if(activityMonitor == null){
			return;
		}

		setRegisterActivities(true);
		//启动一个线程
		Runnable runnable = new Runnable() {
			public void run() {
				while (shouldRegisterActivities()) {
				    //获取监听的Activity
					Activity activity = activityMonitor.waitForActivity();

					if(activity != null){
						//从堆栈中删除Activity名
						if (activitiesStoredInActivityStack.remove(activity.toString())){
							//从堆栈中移除Activity对象
							removeActivityFromStack(activity);
						}
						//Activity未结束，添加到堆栈中(？？？不知道对不对)
						if(!activity.isFinishing()){
							addActivityToStack(activity);
						}
					}
				}
			}
		};
		activityThread = new Thread(runnable, "activityMonitorThread");
		activityThread.start();
	}

	/**
	 * 从堆栈中删除一个指定的Activity
	 * Removes a given activity from the activity stack
	 * 
	 * @param activity the activity to remove
	 */

	private void removeActivityFromStack(Activity activity){
		//遍历堆栈中的每一个Activity
		Iterator<WeakReference<Activity>> activityStackIterator = activityStack.iterator();
		while(activityStackIterator.hasNext()){
			//获取遍历器中的每一个activity
			Activity activityFromWeakReference = activityStackIterator.next().get();
			//如果发现堆栈中存在null对象，则移除
			if(activityFromWeakReference == null){
				activityStackIterator.remove();
			}
			//判断堆栈中的Activity与参数Activity是否相等，相等则删除；
			if(activity != null && activityFromWeakReference != null && activityFromWeakReference.equals(activity)){
				activityStackIterator.remove();
			}
		}
	}

	/**
	 * 用robotium返回ActivityMonitor
	 * Returns the ActivityMonitor used by Robotium.
	 * 
	 * @return the ActivityMonitor used by Robotium
	 */

	public ActivityMonitor getActivityMonitor(){
		return activityMonitor;
	}

	/**
	 * 设置屏幕方向，横或者纵
	 * Sets the Orientation (Landscape/Portrait) for the current activity.
	 * 
	 * @param orientation An orientation constant such as {@link android.content.pm.ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE} or {@link android.content.pm.ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}
	 */

	public void setActivityOrientation(int orientation)
	{
		Activity activity = getCurrentActivity();
		if(activity != null){
			//设置Activity的横纵方向
			activity.setRequestedOrientation(orientation);	
		}
	}

	/**
	 * 获取当前的activity，true标识需要等待500ms,false标识不需要等待500ms
	 * Returns the current {@code Activity}, after sleeping a default pause length.
	 *
	 * @param shouldSleepFirst whether to sleep a default pause first
	 * @return the current {@code Activity}
	 */

	public Activity getCurrentActivity(boolean shouldSleepFirst) {
		return getCurrentActivity(shouldSleepFirst, true);
	}

	/**
	 * 等待500ms，获取并返回当前activity
	 * Returns the current {@code Activity}, after sleeping a default pause length.
	 *
	 * @return the current {@code Activity}
	 */

	public Activity getCurrentActivity() {
		return getCurrentActivity(true, true);
	}

	/**
	 * 新添加一个Activity到stack
	 * Adds an activity to the stack
	 * 
	 * @param activity the activity to add
	 */

	private void addActivityToStack(Activity activity){
		//activity名加入堆栈
		activitiesStoredInActivityStack.push(activity.toString());
		weakActivityReference = new WeakReference<Activity>(activity);
		activity = null;
		//activity弱引用对象加入堆栈
		activityStack.push(weakActivityReference);
	}

	/**
	 * 一直等待，直到出现抓取到一个存活的activity，未找到存活activity则不断迭代循环，有概率导致无限死循环
	 * 可自行修改添加一个超时时间，避免引发无法循环
	 * Waits for an activity to be started if one is not provided
	 * by the constructor.
	 */

	private final void waitForActivityIfNotAvailable(){
		// 如果当前堆栈中的activity为空,当初始化时传入的activity为null，可导致该状态
		if(activityStack.isEmpty() || activityStack.peek().get() == null){
			// 不断尝试获取当前activity,直到获取到一个存活的activity
			if (activityMonitor != null) {
				//获取最新的Activity
				Activity activity = activityMonitor.getLastActivity();
				// 此处可能导致无限循环
				// activityMonitor初始化是为得到当前activity.应用又没有新打开页面，调用该方法就死循环了
				// 传入一个null的activity对象，在 初始化之后，没打开新的 activity就不断null,死循环了
				while (activity == null){
					// 等待300ms
					sleeper.sleepMini();
					// 获取最新activity
					activity = activityMonitor.getLastActivity();
				}
				// 非空对象加入堆栈
				addActivityToStack(activity);
			}
			//如果堆栈是活跃的
			else if(config.trackActivities){
				//沉睡300ms
				sleeper.sleepMini();
				// 初始化activityMonitor
				setupActivityMonitor();
				// 继续获取最新的activity
				waitForActivityIfNotAvailable();
			}
		}
	}
	
	/**
	 * 返回最新activity的名字
	 * Returns the name of the most recent Activity
	 *  
	 * @return the name of the current {@code Activity}
	 */
	
	public String getCurrentActivityName(){
		//如果堆栈中不为空
		if(!activitiesStoredInActivityStack.isEmpty()){
			//返回最新activity的名字
			return activitiesStoredInActivityStack.peek();
		}
		return "";
	}

	/**
	 * 获取当前最新的activity,shouldSleepFirst为true,那么等待500ms后在获取,
	 * waitForActivity为true那么尝试获取最新的activity,为false则不尝试获取最新的，直接从activity堆栈中获取栈顶的activity返回
	 * Returns the current {@code Activity}.
	 *
	 * @param shouldSleepFirst whether to sleep a default pause first
	 * @param waitForActivity whether to wait for the activity
	 * @return the current {@code Activity}
	 */

	public Activity getCurrentActivity(boolean shouldSleepFirst, boolean waitForActivity) {
		//是否需要休息
		if(shouldSleepFirst){
			sleeper.sleep();
		}
		//？？
		if(!config.trackActivities){
			return activity;
		}
		//是否需要获取最新的
		if(waitForActivity){
			waitForActivityIfNotAvailable();
		}
		//非空时，获取堆栈栈顶的activity
		if(!activityStack.isEmpty()){
			activity=activityStack.peek().get();
		}
		//返回最新activity
		return activity;
	}

	/**
	 * 检查Activity是否为空
	 * Check if activity stack is empty.
	 * 
	 * @return true if activity stack is empty
	 */
	
	public boolean isActivityStackEmpty() {
		return activityStack.isEmpty();
	}

	/**
	 * 根据Activity名找到指定Activity，并处于指定Activity界面
	 * Returns to the given {@link Activity}.
	 *
	 * @param name the name of the {@code Activity} to return to, e.g. {@code "MyActivity"}
	 */

	public void goBackToActivity(String name)
	{	//获取所有打开的Activity
		ArrayList<Activity> activitiesOpened = getAllOpenedActivities();
		//设置found标识
		boolean found = false;	
		//循环查找指定name的Activity
		for(int i = 0; i < activitiesOpened.size(); i++){
			if(activitiesOpened.get(i).getClass().getSimpleName().equals(name)){
				found = true;
				break;
			}
		}
		//如果找到
		if(found){
			//但不是当前Activity,退出当前Activity，直到当前Activity匹配指定Activity
			while(!getCurrentActivity().getClass().getSimpleName().equals(name))
			{
				try{
					//点击“返回”功能
					inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
				}catch(SecurityException ignored){}	
			}
		}
		//如果没找到，记录日志，并退出
		else{
			for (int i = 0; i < activitiesOpened.size(); i++){
				Log.d(LOG_TAG, "Activity priorly opened: "+ activitiesOpened.get(i).getClass().getSimpleName());
			}
			Assert.fail("No Activity named: '" + name + "' has been priorly opened");
		}
	}

	/**
	 * 在当前activity中按照id查询String
	 * Returns a localized string.
	 * 
	 * @param resId the resource ID for the string
	 * @return the localized string
	 */

	public String getString(int resId)
	{	//获取当前Activity
		Activity activity = getCurrentActivity(false);
		if(activity == null){
			return "";
		}
		//在当前activity中按照id查询String
		return activity.getString(resId);
	}

	/**
	 * solo生命周期结束，释放相关资源
	 * Finalizes the solo object.
	 */  

	@Override
	public void finalize() throws Throwable {
		// 停止activity监控定时任务
		activitySyncTimer.cancel();
		// 清理activityMonitor对象
		stopActivityMonitor();
		super.finalize();
	}
	
	/**
	 * 移除Activity监视器
	 * Removes the ActivityMonitor
	 */
	private void stopActivityMonitor(){
		try {
			//清理开始期间创建的Activity定时器
			// Remove the monitor added during startup
			if (activityMonitor != null) {
				inst.removeMonitor(activityMonitor);
				activityMonitor = null;
			}
		} catch (Exception ignored) {}

	}

	/**
	 * 关闭所有存活的Activity
	 * All activites that have been opened are finished.
	 */

	public void finishOpenedActivities(){
		//停止activity监控定时任务
		// Stops the activityStack listener
		activitySyncTimer.cancel();
		if(!config.trackActivities){
			//返回3次
			useGoBack(3);
			return;
		}
		//获取所有存活Activity
		ArrayList<Activity> activitiesOpened = getAllOpenedActivities();
		//关闭所有存活Activity
		// Finish all opened activities
		for (int i = activitiesOpened.size()-1; i >= 0; i--) {
			sleeper.sleep(MINISLEEP);
			//关闭Activity
			finishActivity(activitiesOpened.get(i));
		}
		activitiesOpened = null;
		sleeper.sleep(MINISLEEP);
		//关闭初始Activity，按返回键
		//Finish the initial activity, pressing Back for good measure
		finishActivity(getCurrentActivity(true, false));
		//移除Activity监视器
		stopActivityMonitor();
		//如果登记Activity被执行了，设置registerActivity为true
		setRegisterActivities(false);
		this.activity = null;
		sleeper.sleepMini();
		useGoBack(1);
		//清理堆栈
		clearActivityStack();
	}
	
	/**
	 * 返回指定次数
	 * Sends the back button command a given number of times
	 * 
	 * @param numberOfTimes the number of times to press "back"
	 */
	
	private void useGoBack(int numberOfTimes){
		for(int i = 0; i < numberOfTimes; i++){
			try {
				//发送返回事件
				inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
				//休息300ms
				sleeper.sleep(MINISLEEP);
				//发送返回事件
				inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
			} catch (Throwable ignored) {
				// Guard against lack of INJECT_EVENT permission
			}
		}
	}
	
	/**
	 * 清理Activity堆栈
	 * Clears the activity stack.
	 */

	private void clearActivityStack(){
		//堆栈清理
		activityStack.clear();
		//堆栈中的应用名清理
		activitiesStoredInActivityStack.clear();
	}

	/**
	 * 关闭一个指定Activity
	 * Finishes an activity.
	 * 
	 * @param activity the activity to finish
	 */

	private void finishActivity(Activity activity){
		if(activity != null) {
			try{
				//Activity关闭
				activity.finish();
			}catch(Throwable e){
				e.printStackTrace();
			}
		}
	}
}
