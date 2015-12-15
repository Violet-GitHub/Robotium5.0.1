package com.robotium.solo;

import java.util.ArrayList;
import com.robotium.solo.Solo.Config;
import junit.framework.Assert;
import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ScrollView;


/**
 * 滚动工具类，手指滑动，滚动条滚动，指定view在屏幕上滑动
 * Contains scroll methods. Examples are scrollDown(), scrollUpList(),
 * scrollToSide().
 *
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

class Scroller {
	//向下
	public static final int DOWN = 0;
	//向上
	public static final int UP = 1;
	//左右枚举
	public enum Side {LEFT, RIGHT}
	//是否可以拖动
	private boolean canScroll = false;
	//事件发送器
	private final Instrumentation inst;
	//试图查找工具
	private final ViewFetcher viewFetcher;
	//休眠工具
	private final Sleeper sleeper;
	//robotium属性配置类
	private final Config config;


	/**
	 * Constructs this object.
	 *
	 * @param inst the {@code Instrumentation} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param sleeper the {@code Sleeper} instance
	 */

	public Scroller(Config config, Instrumentation inst, ViewFetcher viewFetcher, Sleeper sleeper) {
		this.config = config;
		this.inst = inst;
		this.viewFetcher = viewFetcher;
		this.sleeper = sleeper;
	}


	/**
	 * 按住并且拖动到指定位置
	 * fromx  屏幕上，点击点的x坐标；
	 * fromy  屏幕上，点击点的y坐标；
	 * tox 屏幕上，点击点的x坐标；
	 * toy 屏幕上，点击点的y坐标；
	 * stepCount 指定绘画的步数；
	 * Simulate touching a specific location and dragging to a new location.
	 *
	 * This method was copied from {@code TouchUtils.java} in the Android Open Source Project, and modified here.
	 *
	 * @param fromX X coordinate of the initial touch, in screen coordinates
	 * @param toX Xcoordinate of the drag destination, in screen coordinates
	 * @param fromY X coordinate of the initial touch, in screen coordinates
	 * @param toY Y coordinate of the drag destination, in screen coordinates
	 * @param stepCount How many move steps to include in the drag
	 */

	public void drag(float fromX, float toX, float fromY, float toY,
			int stepCount) {
		//获取当前时间
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis();
		//fromx,fromy
		float y = fromY;
		float x = fromX;
		//计算每次增加的Y坐标
		float yStep = (toY - fromY) / stepCount;
		//计算每次增加的X坐标
		float xStep = (toX - fromX) / stepCount;
		// 构造MotionEvent,先按住
		MotionEvent event = MotionEvent.obtain(downTime, eventTime,MotionEvent.ACTION_DOWN, fromX, fromY, 0);
		try {
			// 通过Instrument发送按住事件
			inst.sendPointerSync(event);
		} catch (SecurityException ignored) {}
		// 按照设置的步数，发送Move事件
		for (int i = 0; i < stepCount; ++i) {
			y += yStep;
			x += xStep;
			eventTime = SystemClock.uptimeMillis();
			//构造Move事件
			event = MotionEvent.obtain(downTime, eventTime,MotionEvent.ACTION_MOVE, x, y, 0);
			try {
				//通过Instrument发送Move事件
				inst.sendPointerSync(event);
			} catch (SecurityException ignored) {}
		}
		//获取当前时间
		eventTime = SystemClock.uptimeMillis();
		// 构造松开事件
		event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP,toX, toY, 0);
		try {
			//通过Instrument发送松开事件
			inst.sendPointerSync(event);
		} catch (SecurityException ignored) {}
	}


	/**
	 * 按照设定的方法拖动滚动条,已经处于顶部的，调用拖动到顶部无效
	 * view 被拖动的view
	 * direction view被拖动到的方向
	 * Scrolls a ScrollView.
	 *
	 * @param direction the direction to be scrolled
	 * @return {@code true} if scrolling occurred, false if it did not
	 */

	private boolean scrollView(final View view, int direction){
		//null值判断
		if(view == null){
			return false;
		}
		//获取view的高度
		int height = view.getHeight();
		// 高度减小一个像素
		height--;
		//
		int scrollTo = -1;
		// 向上拉动，设置成滚动条的高度,拉到顶部
		if (direction == DOWN) {
			scrollTo = height;
		}
		// 向下拉动，设置成负值,拉到底部
		else if (direction == UP) {
			scrollTo = -height;
		}
		//获取当前滚动的高度位置
		int originalY = view.getScrollY();
		final int scrollAmount = scrollTo;
		inst.runOnMainSync(new Runnable(){
			public void run(){
				//滚动到指定位置
				view.scrollBy(0, scrollAmount);
			}
		});
		// 滚动条坐标未变化，标识本次拖动动作失败.已经处于顶端了，触发无效果
		if (originalY == view.getScrollY()) {
			return false;
		}
		else{
			return true;
		}
	}

	/**
	 * 滚动条滑到底部或者顶部，已经处于顶部，调用该方法拖动到顶部将引发死循环
	 * Scrolls a ScrollView to top or bottom.
	 *
	 * @param direction the direction to be scrolled
	 */

	private void scrollViewAllTheWay(final View view, final int direction) {
		while(scrollView(view, direction));
	}

	/**
	 * 滚动条滑到底部或者顶部，已经处于顶部，调用该方法拖动到顶部将引发死循环
	 * Scrolls up or down.
	 *
	 * @param direction the direction in which to scroll
	 * @return {@code true} if more scrolling can be done
	 */

	public boolean scroll(int direction) {
		return scroll(direction, false);
	}

	/**
	 * 滑动到底部
	 * Scrolls down.
	 *
	 * @return {@code true} if more scrolling can be done
	 */

	public boolean scrollDown() {
		if(!config.shouldScroll) {
			return false;
		}
		return scroll(Scroller.DOWN);
	}

	/**
	 * 拖动当前页面最新的view控件，看是否可拖动
	 * direction  0拖动到顶部,1拖动到底部
	 * Scrolls up and down.
	 *
	 * @param direction the direction in which to scroll
	 * @param allTheWay <code>true</code> if the view should be scrolled to the beginning or end,
	 *                  <code>false</code> to scroll one page up or down.
	 * @return {@code true} if more scrolling can be done
	 */

	@SuppressWarnings("unchecked")
	public boolean scroll(int direction, boolean allTheWay) {
		//获取当前activity的所有可见view
		ArrayList<View> viewList = RobotiumUtils.
				removeInvisibleViews(viewFetcher.getAllViews(true));
		//在viewList中过滤出指定类型的view，并返回
		ArrayList<View> views = RobotiumUtils.filterViewsToSet(new Class[] { ListView.class,
				ScrollView.class, GridView.class, WebView.class}, viewList);
		//从views中获取当前最新的view
		View view = viewFetcher.getFreshestView(views);
		//如果找不到可滑动的view，则返回false
		if (view == null) {
				//尝试查找5次
				view = viewFetcher.getRecyclerView(0, 5);
				//循环5次未找到，返回false
				if(view == null){
					return false;
				}
		}
		//如果view属于absListView，按照AbsListView移动方法，判断view是否可移动
		if (view instanceof AbsListView) {
			//返回是否可以移动
			return scrollList((AbsListView)view, direction, allTheWay);
		}
		//如果view属于WebView，按照WebView移动方法，判断view是否可移动
		if(view instanceof WebView){
			//返回是否可以移动
			return scrollWebView((WebView)view, direction, allTheWay);
		}
		if (allTheWay) {
			//滚动条滑到底部或者顶部，已经处于顶部，调用该方法拖动到顶部将引发死循环
			scrollViewAllTheWay(view, direction);
			return false;
		} else {
			//按照设定的方法拖动滚动条,已经处于顶部的，调用拖动到顶部无效
			return scrollView(view, direction);
		}
	}
	
	/**
	 * 滚动WebView，看是否可拖动
	 * webView 被滚动的webView
	 * direction 方向
	 * allTheWay
	 * Scrolls a WebView.
	 * 
	 * @param webView the WebView to scroll
	 * @param direction the direction to scroll
	 * @param allTheWay {@code true} to scroll the view all the way up or down, {@code false} to scroll one page up or down                          or down.
	 * @return {@code true} if more scrolling can be done
	 */
	
	public boolean scrollWebView(final WebView webView, int direction, final boolean allTheWay){
		//向下移动
		if (direction == DOWN) {
			//启动一个子线程
			inst.runOnMainSync(new Runnable(){
				public void run(){
					//是否成功向下移动一半距离
					canScroll =  webView.pageDown(allTheWay);
				}
			});
		}
		//向上移动
		if(direction == UP){
			inst.runOnMainSync(new Runnable(){
				public void run(){
					//是否成功向上移动一半距离
					canScroll =  webView.pageUp(allTheWay);
				}
			});
		}
		//返回是否移动
		return canScroll;
	}

	/**
	 * 拖动一个列表，看是否可拖动
	 * Scrolls a list.
	 * absListView AbsListView类型的，即列表类控件
	 * direction   拖动方向0最顶部，1最底部
	 *
	 * @param absListView the list to be scrolled
	 * @param direction the direction to be scrolled
	 * @param allTheWay {@code true} to scroll the view all the way up or down, {@code false} to scroll one page up or down
	 * @return {@code true} if more scrolling can be done
	 */

	public <T extends AbsListView> boolean scrollList(T absListView, int direction, boolean allTheWay) {
		//null判断
		if(absListView == null){
			return false;
		}
		//向下移动
		if (direction == DOWN) {
			//获取ListView中的view数量
			int listCount = absListView.getCount();
			//获取最新ListView的有效位置
			int lastVisiblePosition = absListView.getLastVisiblePosition();
			//如果是直接拖动到底部的模式
			if (allTheWay) {
				//拖动到最大号的行,因总数据数，会大于可视行数，因此调用此方法，永久返回false
				scrollListToLine(absListView, listCount-1);
				return false;
			}
			//当指定行数比可见行数大，拖动到可见行数底部，返回false.
			if (lastVisiblePosition >= listCount - 1) {
				if(lastVisiblePosition > 0){
					scrollListToLine(absListView, lastVisiblePosition);
				}
				return false;
			}
			//获取第一行
			int firstVisiblePosition = absListView.getFirstVisiblePosition();
			
			//当可见的不只是一行时，拖动到最后一行
			if(firstVisiblePosition != lastVisiblePosition)
				scrollListToLine(absListView, lastVisiblePosition);
			// 当可见的只有一行时，拖动到下面一行
			else
				scrollListToLine(absListView, firstVisiblePosition + 1);
		//向上移动
		} else if (direction == UP) {
			//获取第一行
			int firstVisiblePosition = absListView.getFirstVisiblePosition();
			//可见行数少于1行时，直接划到第0行
			if (allTheWay || firstVisiblePosition < 2) {
				scrollListToLine(absListView, 0);
				return false;
			}
			//获取最后一行
			int lastVisiblePosition = absListView.getLastVisiblePosition();
			//计算显示的行数.没必要设置成final,又不是子类中使用
			final int lines = lastVisiblePosition - firstVisiblePosition;
			//计算出移动到第一行需要的行数
			int lineToScrollTo = firstVisiblePosition - lines;
			//如果正好可以显示行数与当前底部位置一致,则移动到当前位置
			if(lineToScrollTo == lastVisiblePosition)
				lineToScrollTo--;
			// 如果计算位置为负值，那么直接滑到顶部
			if(lineToScrollTo < 0)
				lineToScrollTo = 0;
			
			scrollListToLine(absListView, lineToScrollTo);
		}
		sleeper.sleep();
		return true;
	}


	/**
	 * 滚动list到指定行
	 * Scroll the list to a given line
	 *
	 * @param view the {@link AbsListView} to scroll
	 * @param line the line to scroll to
	 */

	public <T extends AbsListView> void scrollListToLine(final T view, final int line){
		//非null校验，如果为空，则记录日志，并退出；
		if(view == null)
			Assert.fail("AbsListView is null!");

		final int lineToMoveTo;
		// 如果是gridview类型的，带标题，因此行数+1
		if(view instanceof GridView) {
			lineToMoveTo = line+1;
		}
		else {
			lineToMoveTo = line;
		}
		// 发送拖动事件，拖动到指定行
		inst.runOnMainSync(new Runnable(){
			public void run(){
				//选定指定view行
				view.setSelection(lineToMoveTo);
			}
		});
	}


	/**
	 * 横向拖动
	 * side           指定拖动方向
	 * scrollPosition 拖动百分比0-1.
	 * Scrolls horizontally.
	 *
	 * @param side the side to which to scroll; {@link Side#RIGHT} or {@link Side#LEFT}
	 * @param scrollPosition the position to scroll to, from 0 to 1 where 1 is all the way. Example is: 0.55.
	 * @param stepCount how many move steps to include in the scroll. Less steps results in a faster scroll
	 */

	@SuppressWarnings("deprecation")
	public void scrollToSide(Side side, float scrollPosition, int stepCount) {
		//Windows管理器
		WindowManager windowManager = (WindowManager) 
				inst.getTargetContext().getSystemService(Context.WINDOW_SERVICE);
		//获取屏幕窗口的高度
		int screenHeight = windowManager.getDefaultDisplay()
				.getHeight();
		//获取屏幕窗口的宽度
		int screenWidth = windowManager.getDefaultDisplay()
				.getWidth();
		//按照宽度计算总的距离
		float x = screenWidth * scrollPosition;
		//拖动选择屏幕正中间
		float y = screenHeight / 2.0f;
		//向左移动，绘制
		if (side == Side.LEFT)
			drag(70, x, y, y, stepCount);
		//向右移动
		else if (side == Side.RIGHT)
			drag(x, 0, y, y, stepCount);
	}

	/**
	 * 对给定控件进行向左或向右拖动操作
	 * Scrolls view horizontally.
	 *
	 * @param view the view to scroll
	 * @param side the side to which to scroll; {@link Side#RIGHT} or {@link Side#LEFT}
	 * @param scrollPosition the position to scroll to, from 0 to 1 where 1 is all the way. Example is: 0.55.
	 * @param stepCount how many move steps to include in the scroll. Less steps results in a faster scroll
	 */

	public void scrollViewToSide(View view, Side side, float scrollPosition, int stepCount) {
		int[] corners = new int[2];
		//获取view当前在屏幕上的位置
		view.getLocationOnScreen(corners);
		//获取view的高
		int viewHeight = view.getHeight();
		//获取view的宽
		int viewWidth = view.getWidth();
		//计算出滚动的距离
		float x = corners[0] + viewWidth * scrollPosition;
		//拖动选择屏幕正中间
		float y = corners[1] + viewHeight / 2.0f;
		//向左移动
		if (side == Side.LEFT)
			drag(corners[0], x, y, y, stepCount);
		//向右移动
		else if (side == Side.RIGHT)
			drag(x, corners[0], y, y, stepCount);
	}
}
