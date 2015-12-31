package com.robotium.solo;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Solo.Config.ScreenshotFileType;
import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

/**
 * 截图工具类
 * Contains screenshot methods like: takeScreenshot(final View, final String name), startScreenshotSequence(final String name, final int quality, final int frameDelay, final int maxFrames), 
 * stopScreenshotSequence().
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class ScreenshotTaker {
	//时间
	private static final long TIMEOUT_SCREENSHOT_MUTEX = TimeUnit.SECONDS.toMillis(2);
	//
	private final Object screenshotMutex = new Object();
	//robotium配置类
	private final Config config;
	//事件发送器
	private final Instrumentation instrumentation;
	//Activity工具类
	private final ActivityUtils activityUtils;
	//log标志
	private final String LOG_TAG = "Robotium";
	//连续截图线程
	private ScreenshotSequenceThread screenshotSequenceThread = null;
	//图片存储处理线程
	private HandlerThread screenShotSaverThread = null;
	//图片保存工具类
	private ScreenShotSaver screenShotSaver = null;
	//view查找工具类
	private final ViewFetcher viewFetcher;
	//休息工具类
	private final Sleeper sleeper;


	/**
	 * Constructs this object.
	 * 
	 * @param config the {@code Config} instance
	 * @param instrumentation the {@code Instrumentation} instance.
	 * @param activityUtils the {@code ActivityUtils} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param sleeper the {@code Sleeper} instance
	 * 
	 */
	ScreenshotTaker(Config config, Instrumentation instrumentation, ActivityUtils activityUtils, ViewFetcher viewFetcher, Sleeper sleeper) {
		this.config = config;
		this.instrumentation = instrumentation;
		this.activityUtils = activityUtils;
		this.viewFetcher = viewFetcher;
		this.sleeper = sleeper;
	}

	/**
	 * 截图并保存在指定配置的路径
	 * Takes a screenshot and saves it in the {@link Config} objects save path.  
	 * Requires write permission (android.permission.WRITE_EXTERNAL_STORAGE) in AndroidManifest.xml of the application under test.
	 * 
	 * @param name the name to give the screenshot image
	 * @param quality the compression rate. From 0 (compress for lowest size) to 100 (compress for maximum quality).
	 */
	public void takeScreenshot(final String name, final int quality) {
		//获取当前的界面显示view,并做一些Robotium定制化的操作
		View decorView = getScreenshotView();
		if(decorView == null) 
			return;
		//初始化图片保存工具类
		initScreenShotSaver();
		//构造截图线程
		ScreenshotRunnable runnable = new ScreenshotRunnable(decorView, name, quality);
		//执行线程截图
		synchronized (screenshotMutex) {
			//获取当前Activity，通过Activity调用UI线程来执行
			Activity activity = activityUtils.getCurrentActivity(false);
			if(activity != null)
				activity.runOnUiThread(runnable);
			else
				//如果Activity获取失败，采用事件发送器来执行
				instrumentation.runOnMainSync(runnable);
			//截图超时控制
			try {
				screenshotMutex.wait(TIMEOUT_SCREENSHOT_MUTEX);
			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * 连接截图
	 * name    		截图保存的图片名.会追加_0---maxFrames-1
	 * quality 		截图质量0-100
	 * frameDelay   每次截图时间间隔
	 * maxFrames    截图数量 
	 * Takes a screenshot sequence and saves the images with the name prefix in the {@link Config} objects save path.  
	 *
	 * The name prefix is appended with "_" + sequence_number for each image in the sequence,
	 * where numbering starts at 0.  
	 *
	 * Requires write permission (android.permission.WRITE_EXTERNAL_STORAGE) in the 
	 * AndroidManifest.xml of the application under test.
	 *
	 * Taking a screenshot will take on the order of 40-100 milliseconds of time on the 
	 * main UI thread.  Therefore it is possible to mess up the timing of tests if
	 * the frameDelay value is set too small.
	 *
	 * At present multiple simultaneous screenshot sequences are not supported.  
	 * This method will throw an exception if stopScreenshotSequence() has not been
	 * called to finish any prior sequences.
	 *
	 * @param name the name prefix to give the screenshot
	 * @param quality the compression rate. From 0 (compress for lowest size) to 100 (compress for maximum quality)
	 * @param frameDelay the time in milliseconds to wait between each frame
	 * @param maxFrames the maximum number of frames that will comprise this sequence
	 *
	 */
	public void startScreenshotSequence(final String name, final int quality, final int frameDelay, final int maxFrames) {
		//初始化图片保存工具类
		initScreenShotSaver();
		// 禁止同时执行多个连续截图，当有连续截图在执行时抛出异常
		if(screenshotSequenceThread != null) {
			throw new RuntimeException("only one screenshot sequence is supported at a time");
		}
		// 构造一个连续截图线程
		screenshotSequenceThread = new ScreenshotSequenceThread(name, quality, frameDelay, maxFrames);
		// 开始连续截图
		screenshotSequenceThread.start();
	}

	/**
	 * 结束连续截图
	 * Causes a screenshot sequence to end.
	 * 
	 * If this method is not called to end a sequence and a prior sequence is still in 
	 * progress, startScreenshotSequence() will throw an exception.
	 */
	public void stopScreenshotSequence() {
		// 当连续截图线程非空时，停止连续截图
		if(screenshotSequenceThread != null) {
			// 停止连续截图
			screenshotSequenceThread.interrupt();
			// 释放线程对象
			screenshotSequenceThread = null;
		}
	}

	/**
	 * 获取当前的界面显示view,并做一些Robotium定制化的操作
	 * Gets the proper view to use for a screenshot.  
	 */
	private View getScreenshotView() {
		//获取当前的DecorView
		View decorView = viewFetcher.getRecentDecorView(viewFetcher.getWindowDecorViews());
		//计算出超时时间
		final long endTime = SystemClock.uptimeMillis() + Timeout.getSmallTimeout();
		//null值判断，为空，则在规定时间内再进行查找
		while (decorView == null) {	
			//每次循环都判断是否在限制时间内
			final boolean timedOut = SystemClock.uptimeMillis() > endTime;
			//如果超时，未找到则返回null值
			if (timedOut){
				return null;
			}
			//休息一下
			sleeper.sleepMini();
			//获取当前的DecorView
			decorView = viewFetcher.getRecentDecorView(viewFetcher.getWindowDecorViews());
		}
		//修改view的Render
		wrapAllGLViews(decorView);
		//返回修改后的DecorView
		return decorView;
	}

	/**
	 * 修改 View的Render,用Robotium自定义的替换
	 * Extract and wrap the all OpenGL ES Renderer.
	 */
	private void wrapAllGLViews(View decorView) {
		//获取DecorView中所有的view，添加入数组
		ArrayList<GLSurfaceView> currentViews = viewFetcher.getCurrentViews(GLSurfaceView.class, true, decorView);
		//锁住当前线程，避免并发引发问题
		final CountDownLatch latch = new CountDownLatch(currentViews.size());
		//遍历所有view进行替换render
		for (GLSurfaceView glView : currentViews) {
			//反射获取属性
			Object renderContainer = new Reflect(glView).field("mGLThread").type(GLSurfaceView.class).out(Object.class);
			//获取原始的renderer
			Renderer renderer = new Reflect(renderContainer).field("mRenderer").out(Renderer.class);
			// 如果获取失败，则尝试直接获取glView的属性
			if (renderer == null) {
				renderer = new Reflect(glView).field("mRenderer").out(Renderer.class);
				renderContainer = glView;
			}  
			// 如果无法获取，则跳过当前，处理下一个
			if (renderer == null) {
				//计数器减一
				latch.countDown();
				// 结束当前循环，进入下一个循环
				continue;
			}
			// 按照render类型进行操作,如果已经是Robotium修改过的render,那么重置下相关属性即可
			if (renderer instanceof GLRenderWrapper) {
				// 类型转成Robotium的
				GLRenderWrapper wrapper = (GLRenderWrapper) renderer;
				//设置截图模式
				wrapper.setTakeScreenshot();
				//设置并发控制计数器
				wrapper.setLatch(latch); 
			//如果还不是robotium修改过的，那么就重新构造一个，并且替换原有属性
			} else {
				//构造一个robotium修改过的Render
				GLRenderWrapper wrapper = new GLRenderWrapper(glView, renderer, latch);
				// 通过反射修改属性为定制的render
				new Reflect(renderContainer).field("mRenderer").in(wrapper);
			}
		}
		// 等待操作完成
		try {
			latch.await();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}


	/**
	 * 获取webView的位图
	 * Returns a bitmap of a given WebView.
	 *  
	 * @param webView the webView to save a bitmap from
	 * @return a bitmap of the given web view
	 * 
	 */

	private Bitmap getBitmapOfWebView(final WebView webView){
		// 获取WebView图形内容
		Picture picture = webView.capturePicture();
		//构造Bitmap对象
		Bitmap b = Bitmap.createBitmap( picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
		// 构造Canvas
		Canvas c = new Canvas(b);
		// 把图片绘制到canvas.就是把内容搞到Bitmap中，即b中
		picture.draw(c);
		
		return b;
	}

	/**
	 * 获取View的BitMap格式文件内容
	 * Returns a bitmap of a given View.
	 * 
	 * @param view the view to save a bitmap from
	 * @return a bitmap of the given view
	 * 
	 */

	private Bitmap getBitmapOfView(final View view){
		//清空原有内容
		view.destroyDrawingCache();
		//初始化缓冲
		view.buildDrawingCache(false);
		//获取Bitmap内容
		Bitmap orig = view.getDrawingCache();
		Bitmap.Config config = null;
		// 如果获取内容为null,直接返回null
		if(orig == null) {
			return null;
		}
		// 获取配置信息
		config = orig.getConfig();
		// 如果图片类型无法获取，则默认使用ARGB_8888
		if(config == null) {
			config = Bitmap.Config.ARGB_8888;
		}
		// 构造BitMap内容
		Bitmap b = orig.copy(config, false);
		// 清空绘图缓存
		orig.recycle();
		view.destroyDrawingCache();
		return b; 
	}

	/**
	 * 按照传入文件名，构造完整文件名,未传入则默认以时间格式构造完整文件名
	 * Returns a proper filename depending on if name is given or not.
	 * 
	 * @param name the given name
	 * @return a proper filename depedning on if a name is given or not
	 * 
	 */

	private String getFileName(final String name){
		// 构造日期格式
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy-hhmmss");
		String fileName = null;
		// 如果未传入名字，那么默认构造一个
		if(name == null){
			// 按照"配置"构造图片类型jpg png
			if(config.screenshotFileType == ScreenshotFileType.JPEG){
				fileName = sdf.format( new Date()).toString()+ ".jpg";
			}
			else{
				fileName = sdf.format( new Date()).toString()+ ".png";	
			}
		}
		//如已传入文件名字，那么拼接文件类型后缀
		else {
			if(config.screenshotFileType == ScreenshotFileType.JPEG){
				fileName = name + ".jpg";
			}
			else {
				fileName = name + ".png";	
			}
		}
		return fileName;
	}

	/**
	 * 初始化图片存储相关资源
	 * This method initializes the aysnc screenshot saving logic
	 */
	private void initScreenShotSaver() {
		// 如果当前存储线程未初始化，则进行初始化
		if(screenShotSaverThread == null || screenShotSaver == null) {
			// 初始化一个处理线程
			screenShotSaverThread = new HandlerThread("ScreenShotSaver");
			// 开始运行线程
			screenShotSaverThread.start();
			// 初始化一个存储类
			screenShotSaver = new ScreenShotSaver(screenShotSaverThread);
		}
	}

	/** 
	 * 连续截图线程类
	 * This is the thread which causes a screenshot sequence to happen
	 * in parallel with testing.
	 */
	private class ScreenshotSequenceThread extends Thread {
		//截图标识
		private int seqno = 0;
		//申明一个name值
		private String name;
		//申明一个间隔时间quality
		private int quality;
		//申明一个间隔时间frameDelay
		private int frameDelay;
		//最多截图数
		private int maxFrames;
		//是否还在运行判断
		private boolean keepRunning = true;
		//构造方法
		public ScreenshotSequenceThread(String _name, int _quality, int _frameDelay, int _maxFrames) {
			name = _name;
			quality = _quality; 
			frameDelay = _frameDelay;
			maxFrames = _maxFrames;
		}
		//重写run方法
		public void run() {
			//循环截取maxFrames张图
			while(seqno < maxFrames) {
				//不符合条件停止循环
				if(!keepRunning || Thread.interrupted()) break;
				//截一张图
				doScreenshot();
				//截图数量加一
				seqno++;
				try {
					//休息截图间隔时间
					Thread.sleep(frameDelay);
				} catch (InterruptedException e) {
				}
			}
			screenshotSequenceThread = null;
		}
		
		//截一张图
		public void doScreenshot() {
			//获取当前界面的DecorView
			View v = getScreenshotView();
			if(v == null) keepRunning = false;
			//给图片取名
			String final_name = name+"_"+seqno;
			//循环截图并保存到图片保存工具类中
			ScreenshotRunnable r = new ScreenshotRunnable(v, final_name, quality);
			Log.d(LOG_TAG, "taking screenshot "+final_name);
			//获取当前的Activity
			Activity activity = activityUtils.getCurrentActivity(false);
			if(activity != null){
				//通过Activity调用UI线程执行操作
				activity.runOnUiThread(r);
			}
			else {
				//通过事件发送器调用UI线程，执行操作
				instrumentation.runOnMainSync(r);
			}
		}
		
		//终止线程
		public void interrupt() {
			keepRunning = false;
			super.interrupt();
		}
	}

	/**
	 * 抓取当前屏幕并发送给对应图片处理器进行相关图片处理和保存
	 * Here we have a Runnable which is responsible for taking the actual screenshot,
	 * and then posting the bitmap to a Handler which will save it.
	 *
	 * This Runnable is run on the UI thread.
	 */
	private class ScreenshotRunnable implements Runnable {
		//申明一个view
		private View view;
		//申明一个name
		private String name;
		//申明一个间隔时间
		private int quality;
		
		//通过构造方法赋值申明的内部类全局变量
		public ScreenshotRunnable(final View _view, final String _name, final int _quality) {
			view = _view;
			name = _name;
			quality = _quality;
		}
		
		//实现父类run方法
		public void run() {
			//view的null值判断
			if(view !=null){
				//new一个位图类
				Bitmap  b;
				//判断view并截图
				if(view instanceof WebView){
					//如果是webView，获取webView的截图
					b = getBitmapOfWebView((WebView) view);
				}
				else{
					//如果不是，就获取view的截图
					b = getBitmapOfView(view);
				}
				//对接到的图进行null值判断
				if(b != null) {
					//通过图片保存工具类，保存位图
					screenShotSaver.saveBitmap(b, name, quality);
					//清空b
					b = null;
					// Return here so that the screenshotMutex is not unlocked,
					// since this is handled by save bitmap
					return;
				}
				else
					Log.d(LOG_TAG, "NULL BITMAP!!");
			}
			//确定screenshotMutex是解锁的
			// Make sure the screenshotMutex is unlocked
			synchronized (screenshotMutex) {
				screenshotMutex.notify();
			}
		}
	}

	/**
	 * 保存图片，通过异步线程完成
	 * This class is a Handler which deals with saving the screenshots on a separate thread.
	 *
	 * The screenshot logic by necessity has to run on the ui thread.  However, in practice
	 * it seems that saving a screenshot (with quality 100) takes approx twice as long
	 * as taking it in the first place. 
	 *
	 * Saving the screenshots in a separate thread like this will thus make the screenshot
	 * process approx 3x faster as far as the main thread is concerned.
	 *
	 */
	private class ScreenShotSaver extends Handler {
		//构造方法
		public ScreenShotSaver(HandlerThread thread) {
			super(thread.getLooper());
		}

		/**
		 * 保存图片,通过消息推送
		 * bitmap  要保存的图片
		 * name    图片名
		 * quality 图片质量0-100
		 * This method posts a Bitmap with meta-data to the Handler queue.
		 *
		 * @param bitmap the bitmap to save
		 * @param name the name of the file
		 * @param quality the compression rate. From 0 (compress for lowest size) to 100 (compress for maximum quality).
		 */
		public void saveBitmap(Bitmap bitmap, String name, int quality) {
			// 初始化构造一个消息
			Message message = this.obtainMessage();
			// 初始化消息属性
			message.arg1 = quality;
			message.obj = bitmap;
			message.getData().putString("name", name);
			// 发送消息，等待处理器处理
			this.sendMessage(message);
		}

		/**
		 * 处理收到的消息
		 * Here we process the Handler queue and save the bitmaps.
		 *
		 * @param message A Message containing the bitmap to save, and some metadata.
		 */
		public void handleMessage(Message message) {
			//
			synchronized (screenshotMutex) {
				//获取message中name的值，并赋值给局部变量name
				String name = message.getData().getString("name");
				//获取图片质量，并赋值给局部变量quality
				int quality = message.arg1;
				//获取图片，并赋值给局部变量bitmap
				Bitmap b = (Bitmap)message.obj;
				//b的null值判断
				if(b != null) {
					//保存文件
					saveFile(name, b, quality);
					// 释放图片缓存
					b.recycle();
				}
				// 如果图片无内容，则打印日志信息
				else {
					Log.d(LOG_TAG, "NULL BITMAP!!");
				}
				screenshotMutex.notify();
			}
		}

		/**
		 * 保存文件
		 * Saves a file.
		 * 
		 * @param name the name of the file
		 * @param b the bitmap to save
		 * @param quality the compression rate. From 0 (compress for lowest size) to 100 (compress for maximum quality).
		 * 
		 */
		private void saveFile(String name, Bitmap b, int quality){
			//文件输出流
			FileOutputStream fos = null;
			//根据传入的字符串返回指定的文件名
			String fileName = getFileName(name);
			//获取配置的文件保存路径
			File directory = new File(config.screenshotSavePath);
			// 创建目录
			directory.mkdir();
			// 获取文件对象
			File fileToSave = new File(directory,fileName);
			try {
				//构建一个输出流
				fos = new FileOutputStream(fileToSave);
				//判断文件类型是否一致
				if(config.screenshotFileType == ScreenshotFileType.JPEG){
					// 图片内容按照指定格式压缩，并写入指定文件，如出现异常，打印异常日志
					if (b.compress(Bitmap.CompressFormat.JPEG, quality, fos) == false){
						Log.d(LOG_TAG, "Compress/Write failed");
					}
				}
				else{
					// 图片内容按照指定格式压缩，并写入指定文件，如出现异常，打印异常日志
					if (b.compress(Bitmap.CompressFormat.PNG, quality, fos) == false){
						Log.d(LOG_TAG, "Compress/Write failed");
					}
				}
				// 关闭写文件流
				fos.flush();
				fos.close();
			} catch (Exception e) {
				// 日常记录logcat日志，并打印异常堆栈
				Log.d(LOG_TAG, "Can't save the screenshot! Requires write permission (android.permission.WRITE_EXTERNAL_STORAGE) in AndroidManifest.xml of the application under test.");
				e.printStackTrace();
			}
		}
	}
}
