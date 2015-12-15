package com.robotium.solo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.WebView;

/**
 * Views的操作获取工具类，提供大量操作获取Views的方法
 * Contains view methods. Examples are getViews(),
 * getCurrentTextViews(), getCurrentImageViews().
 *
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

class ViewFetcher {
	// 存储windowManager管理类的名字
	private String windowManagerString;
	//Android源码下的Instrumentation基类
	private Instrumentation instrumentation;
	private Sleeper sleeper;

	/**
	 * ViewFetcher的构造方法
	 * Constructs this object.
	 *
	 * @param instrumentation the {@code Instrumentation} instance.
	 *
	 */

	public ViewFetcher(Instrumentation instrumentation, Sleeper sleeper) {
		this.instrumentation = instrumentation;
		this.sleeper = sleeper;
		//判断Android版本多对应的windowManager的字段名，并设置window管理类的名字
		setWindowManagerString();
	}


	/**
	 * 获取View最顶部的父类
	 * Returns the absolute top parent {@code View} in for a given {@code View}.
	 *
	 * @param view the {@code View} whose top parent is requested
	 * @return the top parent {@code View}
	 */

	public View getTopParent(View view) {
		//获取当前view的父类
		final ViewParent viewParent = view.getParent();
		//当前view存在父类，递归找到最顶部的view并返回
		if (viewParent != null
				&& viewParent instanceof android.view.View) {
			return getTopParent((View) viewParent);
		} else {
			return view;
		}
	}


	/**
	 * 返回列表或者滚动条的mParent属性,即View的宿主容器
	 * 一般为AbsListView ScrollView WebView
	 * 主要为了确定控件类型 
	 * Returns the scroll or list parent view
	 *
	 * @param view the view who's parent should be returned
	 * @return the parent scroll view, list view or null
	 */

	public View getScrollOrListParent(View view) {
		//view不同时继承自ABSListView,ScrollView,WebView,则继续递归
		if (!(view instanceof android.widget.AbsListView) && !(view instanceof android.widget.ScrollView) && !(view instanceof WebView)) {
			try{
				return getScrollOrListParent((View) view.getParent());
			}catch(Exception e){
				return null;
			}
		} else {
			return view;
		}
	}

	/**
	 * 返回所有可见的装饰类
	 * Returns views from the shown DecorViews.
	 *
	 * @param onlySufficientlyVisible if only sufficiently visible views should be returned
	 * @return all the views contained in the DecorViews
	 */

	public ArrayList<View> getAllViews(boolean onlySufficientlyVisible) {
		//final 修饰的变量一旦被赋值后，不可再次赋值    获取屏幕上的所有DecorViews类的数组，并赋值给views数组
		final View[] views = getWindowDecorViews();
		//新建一个arrayList
		final ArrayList<View> allViews = new ArrayList<View>();
		//返回所有非装饰类
		final View[] nonDecorViews = getNonDecorViews(views);
		View view = null;
		//获取所有非DecorViews类的数组
		if(nonDecorViews != null){
			for(int i = 0; i < nonDecorViews.length; i++){
				view = nonDecorViews[i];
				try {
					//遍历获取所有的View
					addChildren(allViews, (ViewGroup)view, onlySufficientlyVisible);
				} catch (Exception ignored) {}
				if(view != null) allViews.add(view);
			}
		}
		//获取所有DecorViews数组包含的view
		if (views != null && views.length > 0) {
			view = getRecentDecorView(views);
			try {
				//遍历获取所有的view
				addChildren(allViews, (ViewGroup)view, onlySufficientlyVisible);
			} catch (Exception ignored) {}
			if(view != null) allViews.add(view);
		}

		return allViews;
	}

	/**
	 * 过滤出Views中的DecorView类
	 * Returns the most recent DecorView
	 *
	 * @param views the views to check
	 * @return the most recent DecorView
	 */

	public final View getRecentDecorView(View[] views) {
		if(views == null)
			return null;

		final View[] decorViews = new View[views.length];
		int i = 0;
		View view;

		for (int j = 0; j < views.length; j++) {
			view = views[j];
			//获取出DecorViews  
			if (view != null){ 
				String nameOfClass = view.getClass().getName();
				if(nameOfClass.equals("com.android.internal.policy.impl.PhoneWindow$DecorView") || nameOfClass
						.equals("com.android.internal.policy.impl.MultiPhoneWindow$MultiPhoneDecorView")) {
					decorViews[i] = view;
					i++;
				}
			}
		}
		return getRecentContainer(decorViews);
	}

	/**
	 * 获取当前的焦点view
	 * Returns the most recent view container
	 *
	 * @param views the views to check
	 * @return the most recent view container
	 */

	private final View getRecentContainer(View[] views) {
		View container = null;
		long drawingTime = 0;
		View view;

		for(int i = 0; i < views.length; i++){
			view = views[i];
			//按照控件是否选中和绘制时间判断是否最新
			if (view != null && view.isShown() && view.hasWindowFocus() && view.getDrawingTime() > drawingTime) {
				//临时变量赋值
				container = view;
				//更改临时变量值
				drawingTime = view.getDrawingTime();
			}
		}
		return container;
	}

	/**
	 * 返回所有非装饰试图
	 * Returns all views that are non DecorViews
	 *
	 * @param views the views to check
	 * @return the non DecorViews
	 */

	private final View[] getNonDecorViews(View[] views) {
		//申明一个装饰试图容器
		View[] decorViews = null;
		//null判断
		if(views != null) {
			//构建一个views数组长度的装饰类容器，并赋值给装饰试图容器
			decorViews = new View[views.length];
			
			int i = 0;
			View view;
			// 类名不是DecorView的则加入返回数组
			for (int j = 0; j < views.length; j++) {
				view = views[j];
				if (view != null && !(view.getClass().getName()
						.equals("com.android.internal.policy.impl.PhoneWindow$DecorView"))) {
					decorViews[i] = view;
					i++;
				}
			}
		}
		return decorViews;
	}



	/**
	 * 返回所给view中包含的所有view，包括所给view自己
	 * parent 为空则返回当前界面所有view
	 * onlySufficientlyVisible 为true则返回所有可clicker的view，为false则不进行过滤，全部返回
	 * Extracts all {@code View}s located in the currently active {@code Activity}, recursively.
	 *
	 * @param parent the {@code View} whose children should be returned, or {@code null} for all
	 * @param onlySufficientlyVisible if only sufficiently visible views should be returned
	 * @return all {@code View}s located in the currently active {@code Activity}, never {@code null}
	 */

	public ArrayList<View> getViews(View parent, boolean onlySufficientlyVisible) {
		final ArrayList<View> views = new ArrayList<View>();
		final View parentToUse;
		//传入的值为空，按照当前界面操作
		if (parent == null){
			return getAllViews(onlySufficientlyVisible);
		}else{
			parentToUse = parent;
			//先把自己给添加了
			views.add(parentToUse);
			//如果自己是viewGroup(自己是View，并且包含其view)
			if (parentToUse instanceof ViewGroup) {
				addChildren(views, (ViewGroup) parentToUse, onlySufficientlyVisible);
			}
		}
		return views;
	}

	/**
	 * 遍历ViewGroup中的所有view
	 * onlySufficientlyVisible 为true则返回所有可点击的试图，为false则不过滤，返回所有遍历到的view
	 * Adds all children of {@code viewGroup} (recursively) into {@code views}.
	 *
	 * @param views an {@code ArrayList} of {@code View}s
	 * @param viewGroup the {@code ViewGroup} to extract children from
	 * @param onlySufficientlyVisible if only sufficiently visible views should be returned
	 */

	private void addChildren(ArrayList<View> views, ViewGroup viewGroup, boolean onlySufficientlyVisible) {
		//null判断
		if(viewGroup != null){
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				//申明临时符合型变量，并赋值
				final View child = viewGroup.getChildAt(i);
				//添加可点击，并且可见的view
				if(onlySufficientlyVisible && isViewSufficientlyShown(child))
					views.add(child);
				//添加所有view，不管是否可点击
				else if(!onlySufficientlyVisible)
					views.add(child);
				//如果view是viewGroup，继续迭代
				if (child instanceof ViewGroup) {
					addChildren(views, (ViewGroup) child, onlySufficientlyVisible);
				}
			}
		}
	}

	/**
	 * 如果view可见返回true，否则返回false
	 * Returns true if the view is sufficiently shown
	 *
	 * @param view the view to check
	 * @return true if the view is sufficiently shown
	 */

	public final boolean isViewSufficientlyShown(View view){
		//申明一个int类型的数组，放视图的xy坐标值
		final int[] xyView = new int[2];
		//申明一个int类型的数组，放父视图的xy坐标值
		final int[] xyParent = new int[2];
		
		//null值判断
		if(view == null)
			return false;
		//获取view的高度
		final float viewHeight = view.getHeight();
		//获取view的宿主容器
		final View parent = getScrollOrListParent(view);
		//获取view的xy坐标值
		view.getLocationOnScreen(xyView);
		//如果无宿主容器，那么宿主容器的坐标为0
		if(parent == null){
			xyParent[1] = 0;
		}else{
			//如果有宿主容器，获取宿主容器的xy坐标
			parent.getLocationOnScreen(xyParent);
		}
		//如果视图的Y坐标值+视图高度的一半>视图滚动条或者父视图的高度，则视图不可见，返回false
		if(xyView[1] + (viewHeight/2.0f) > getScrollListWindowHeight(view))
			return false;
		//如果视图的Y坐标值+视图高度的一半<视图滚动条或者父视图的一半，则视图可见，返回true
		else if(xyView[1] + (viewHeight/2.0f) < xyParent[1])
			return false;

		return true;
	}

	/**
	 * 返回滚动条或者父视图列表的高度
	 * Returns the height of the scroll or list view parent
	 * @param view the view who's parents height should be returned
	 * @return the height of the scroll or list view parent
	 */

	@SuppressWarnings("deprecation")
	public float getScrollListWindowHeight(View view) {
		//申明xy坐标变量
		final int[] xyParent = new int[2];
		//获取view的宿主容器
		View parent = getScrollOrListParent(view);
		//申明一个浮点型变量
		final float windowHeight;
		//如果view没有宿主容器，直接获取当前Activity的高度
		if(parent == null){
			WindowManager windowManager = (WindowManager) 
					instrumentation.getTargetContext().getSystemService(Context.WINDOW_SERVICE);
			windowHeight = windowManager.getDefaultDisplay().getHeight();
		}
		//获取宿主容器的高度
		else{
			parent.getLocationOnScreen(xyParent);
			windowHeight = xyParent[1] + parent.getHeight();
		}
		parent = null;
		return windowHeight;
	}


	/**
	 * 根据给定过滤类型，返回所有该类型的试图
	 * classToFilterBy 给定过滤类型
	 * 
	 * Returns an {@code ArrayList} of {@code View}s of the specified {@code Class} located in the current
	 * {@code Activity}.
	 *
	 * @param classToFilterBy return all instances of this class, e.g. {@code Button.class} or {@code GridView.class}
	 * @param includeSubclasses include instances of the subclasses in the {@code ArrayList} that will be returned
	 * @return an {@code ArrayList} of {@code View}s of the specified {@code Class} located in the current {@code Activity}
	 */

	public <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, boolean includeSubclasses) {
		return getCurrentViews(classToFilterBy, includeSubclasses, null);
	}

	/**
	 * 
	 * Returns an {@code ArrayList} of {@code View}s of the specified {@code Class} located under the specified {@code parent}.
	 *
	 * @param classToFilterBy return all instances of this class, e.g. {@code Button.class} or {@code GridView.class}
	 * @param includeSubclasses include instances of subclasses in {@code ArrayList} that will be returned
	 * @param parent the parent {@code View} for where to start the traversal
	 * @return an {@code ArrayList} of {@code View}s of the specified {@code Class} located under the specified {@code parent}
	 */

	public <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, boolean includeSubclasses, View parent) {
		ArrayList<T> filteredViews = new ArrayList<T>();
		//给定类型中的所有View
		List<View> allViews = getViews(parent, true);
		for(View view : allViews){
			if (view == null) {
				continue;
			}
			//类型转换
			Class<? extends View> classOfView = view.getClass();
			//如果includeSubclasses为true，就把view本身转换成Class类型，并添加到试图列表中
			if (includeSubclasses && classToFilterBy.isAssignableFrom(classOfView) || !includeSubclasses && classToFilterBy == classOfView) {
				filteredViews.add(classToFilterBy.cast(view));
			}
		}
		//释放资源
		allViews = null;
		return filteredViews;
	}


	/**
	 * 返回views中的最新可见试图
	 * Tries to guess which view is the most likely to be interesting. Returns
	 * the most recently drawn view, which presumably will be the one that the
	 * user was most recently interacting with.
	 *
	 * @param views A list of potentially interesting views, likely a collection
	 *            of views from a set of types, such as [{@link Button},
	 *            {@link TextView}] or [{@link ScrollView}, {@link ListView}]
	 * @param index the index of the view
	 * @return most recently drawn view, or null if no views were passed 
	 */

	public final <T extends View> T getFreshestView(ArrayList<T> views){
		//临时变量，储存xy坐标
		final int[] locationOnScreen = new int[2];
		T viewToReturn = null;
		long drawingTime = 0;
		if(views == null){
			return null;
		}
		//根据view的坐标点，和绘制时间来判断view是否是最新view
		for(T view : views){
			//获取view的坐标
			view.getLocationOnScreen(locationOnScreen);
			//横坐标的负值判断
			if (locationOnScreen[0] < 0 ) 
				continue;
			//根据view的绘制时间，和view的高度来判断view是否是最新view
			if(view.getDrawingTime() > drawingTime && view.getHeight() > 0){
				drawingTime = view.getDrawingTime();
				viewToReturn = view;
			}
		}
		//释放资源
		views = null;
		return viewToReturn;
	}
	
	/**
	 *  等待一个XX并返回它
	 * Waits for a RecyclerView and returns it.
	 * 
	 * @param recyclerViewIndex the index of the RecyclerView
	 * @return {@code ViewGroup} if RecycleView is displayed
	 */

	public <T extends View> ViewGroup getRecyclerView(int recyclerViewIndex, int timeOut) {
		final long endTime = SystemClock.uptimeMillis() + timeOut;

		while (SystemClock.uptimeMillis() < endTime) {
			//返回所有viewGroup
			View recyclerView = getRecyclerView(true, recyclerViewIndex);
			if(recyclerView != null){
					return (ViewGroup) recyclerView;
			}
		}
		return null;
	}
	
	
	/**
	 * 如果能找到XX返回它
	 * Returns a RecyclerView or null if none is found
	 * 
	 * @param viewList the list to check in 
	 * 
	 * @return a RecyclerView
	 */
	
	public View getRecyclerView(boolean shouldSleep, int recyclerViewIndex){
		Set<View> uniqueViews = new HashSet<View>();
		if(shouldSleep){
			sleeper.sleep();
		}

		@SuppressWarnings("unchecked")
		ArrayList<View> views = RobotiumUtils.filterViewsToSet(new Class[] {ViewGroup.class}, getAllViews(true));
		views = RobotiumUtils.removeInvisibleViews(views);

		for(View view : views){

			if(isViewType(view.getClass(), "widget.RecyclerView")){
				uniqueViews.add(view);
			}

			if(uniqueViews.size() > recyclerViewIndex) {
				return (ViewGroup) view;
			}
		}
		return null;
	}
	
	 //判断aClass的类名中是否包含typeName
	 private boolean isViewType(Class<?> aClass, String typeName) {
		   if (aClass.getName().contains(typeName)) {
		       return true;
		   }

		   if (aClass.getSuperclass() != null) {
		       return isViewType(aClass.getSuperclass(), typeName);
		   }

		   return false;
		}

	/**
	 * 返回相等的view以指定的view
	 * Returns an identical View to the one specified.
	 * 
	 * @param view the view to find
	 * @return identical view of the specified view
	 */

	public View getIdenticalView(View view) {
		if(view == null){
			return null;
		}
		View viewToReturn = null;
		//根据view类型，返回当前Activity的所有该试图，并去除view中的隐藏view
		List<? extends View> visibleViews = RobotiumUtils.removeInvisibleViews(getCurrentViews(view.getClass(), true));
		
		for(View v : visibleViews){
			//判断两个view是否相等；
			if(areViewsIdentical(v, view)){
				viewToReturn = v;
				break;
			}
		}
		//放回相等的view
		return viewToReturn;
	}

	/**
	 * 与指定view比较，如果一致，
	 * Compares if the specified views are identical. This is used instead of View.compare 
	 * as it always returns false in cases where the View tree is refreshed.  
	 * 
	 * @param firstView the first view
	 * @param secondView the second view
	 * @return true if views are equal
	 */

	private boolean areViewsIdentical(View firstView, View secondView){
		if(firstView.getId() != secondView.getId() || !firstView.getClass().isAssignableFrom(secondView.getClass())){
			return false;
		}

		if (firstView.getParent() != null && firstView.getParent() instanceof View && 
				secondView.getParent() != null && secondView.getParent() instanceof View) {
			
			return areViewsIdentical((View) firstView.getParent(), (View) secondView.getParent());
		} else {
			return true;
		}
	}

	private static Class<?> windowManager;
	static{
		try {
			String windowManagerClassName;
			if (android.os.Build.VERSION.SDK_INT >= 17) {
				windowManagerClassName = "android.view.WindowManagerGlobal";
			} else {
				windowManagerClassName = "android.view.WindowManagerImpl"; 
			}
			windowManager = Class.forName(windowManagerClassName);

		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 返回屏幕上的所有装饰类
	 * Returns the WindorDecorViews shown on the screen.
	 * 
	 * @return the WindorDecorViews shown on the screen
	 */

	@SuppressWarnings("unchecked")
	public View[] getWindowDecorViews()
	{

		Field viewsField;
		Field instanceField;
		try {
			//
			viewsField = windowManager.getDeclaredField("mViews");
			//
			instanceField = windowManager.getDeclaredField(windowManagerString);
			//
			viewsField.setAccessible(true);
			//
			instanceField.setAccessible(true);
			Object instance = instanceField.get(null);
			View[] result;
			if (android.os.Build.VERSION.SDK_INT >= 19) {
				result = ((ArrayList<View>) viewsField.get(instance)).toArray(new View[0]);
			} else {
				result = (View[]) viewsField.get(instance);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 判断当前Android版本对应的WindowManager对象字段名 ，并设置window管理类的名字
	 * Sets the window manager string.
	 */
	private void setWindowManagerString(){

		if (android.os.Build.VERSION.SDK_INT >= 17) {
			windowManagerString = "sDefaultWindowManager";

		} else if(android.os.Build.VERSION.SDK_INT >= 13) {
			windowManagerString = "sWindowManager";

		} else {
			windowManagerString = "mWindowManager";
		}
	}


}