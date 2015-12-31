package com.robotium.solo;

import android.widget.EditText;
import android.widget.Spinner;
import junit.framework.Assert;
import android.app.Instrumentation;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

/**
 * Contains press methods. Examples are pressMenuItem(),
 * pressSpinnerItem().
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class Presser{
	//点击工具类
	private final Clicker clicker;
	//事件发送器
	private final Instrumentation inst;
	//等待工具类
	private final Sleeper sleeper;
	//等待试图工具类
	private final Waiter waiter;
	//对话框处理工具类
	private final DialogUtils dialogUtils;
	//试图工具类，查找试图
	private final ViewFetcher viewFetcher;


	/**
	 * Constructs this object.
	 *
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param clicker the {@code Clicker} instance
	 * @param inst the {@code Instrumentation} instance
	 * @param sleeper the {@code Sleeper} instance
	 * @param waiter the {@code Waiter} instance
	 * @param dialogUtils the {@code DialogUtils} instance
	 */

	public Presser(ViewFetcher viewFetcher, Clicker clicker, Instrumentation inst, Sleeper sleeper, Waiter waiter, DialogUtils dialogUtils) {
		this.viewFetcher = viewFetcher;
		this.clicker = clicker;
		this.inst = inst;
		this.sleeper = sleeper;
		this.waiter = waiter;
		this.dialogUtils = dialogUtils;
	}


	/**
	 * 点击指定菜单项
	 * 菜单按照上下左右排列菜单，每行3个菜单项，根据给定的index在每行中查找菜单项，并点击找到的菜单项
	 * Presses a {@link android.view.MenuItem} with a given index. Index {@code 0} is the first item in the
	 * first row, Index {@code 3} is the first item in the second row and
	 * index {@code 5} is the first item in the third row.
	 *
	 * @param index the index of the {@code MenuItem} to be pressed
	 */

	public void pressMenuItem(int index){
		pressMenuItem(index, 3);
	}

	/**
	 * 按给定index的item，菜单按照上下左右排列菜单，每行3个菜单项，根据给定的index在每行中查找菜单项，并点击找到的菜单项
	 * index 要查找的菜单项的下坐标
	 * itemsPerRow 每行菜单个数
	 * Presses a {@link android.view.MenuItem} with a given index. Supports three rows with a given amount
	 * of items. If itemsPerRow equals 5 then index 0 is the first item in the first row, 
	 * index 5 is the first item in the second row and index 10 is the first item in the third row.
	 * 
	 * @param index the index of the {@code MenuItem} to be pressed
	 * @param itemsPerRow the amount of menu items there are per row.   
	 */

	public void pressMenuItem(int index, int itemsPerRow) {	
		//Item缓存，存储4行的每行开头序号
		int[] row = new int[4];
		//初始化每行开头的序号，分别为0 3 6 9
		for(int i = 1; i <=3; i++)
			row[i] = itemsPerRow*i;

		sleeper.sleep();
		try{
			//点击menu菜单
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
			//等待菜单出现
			dialogUtils.waitForDialogToOpen(Timeout.getSmallTimeout(), true);
			//点击2次上方向键.Item位置回到第一个
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
		}catch(SecurityException e){
			Assert.fail("Can not press the menu!");
		}
		//如果index在第一行，则在第一行向右移动，知道找到指定的item
		if (index < row[1]) {
			for (int i = 0; i <index; i++) {
				sleeper.sleepMini();
				//向右移动
				inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
		//如果index在第二行，则先向下移动到第二行，然后在第二行向右移动查找指定index的item
		} else if (index >= row[1] && index < row[2]) {
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);	

			for (int i = row[1]; i < index; i++) {
				sleeper.sleepMini();
				inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
		//如果index在第三行，或者之后的行，则先向下移动2步到第三行，然后在第三行向右移动查找指定index的item
		} else if (index >= row[2]) {
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);	
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);	

			for (int i = row[2]; i < index; i++) {
				sleeper.sleepMini();
				inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
		}
		//点击确认
		try{
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
		}catch (SecurityException ignored) {}
	}
	
	/**
	 * 点击软件盘上的搜索按钮或者下一个按钮
	 * search 不可修改的Boolean值，为true应该点击下一个按钮
	 * Presses the soft keyboard search/next button.
	 * 
	 * @param search true if search button should be pressed otherwise next is pressed
	 *  
	 */

	public void pressSoftKeyboardSearchOrNextButton(final boolean search){
		//不可修改的freshestEditText=最新的EditText试图(返回当前所有EditTextView试图)；
		final EditText freshestEditText = viewFetcher.getFreshestView(viewFetcher.getCurrentViews(EditText.class, true));
		if(freshestEditText != null){
			//启动一个多线程
			inst.runOnMainSync(new Runnable()
			{
				public void run()
				{	
					//为true，点击搜索按钮
					if(search){
						freshestEditText.onEditorAction(EditorInfo.IME_ACTION_SEARCH); 
					}
					//否则，点击下一个按钮
					else {
						freshestEditText.onEditorAction(EditorInfo.IME_ACTION_NEXT); 	
					}
				}
			});
		}
	}

	/**
	 * 点击第spinnerIndex个 Spinner的第itemIndex个Item
	 * spinnerIndex     指定的Spinner顺序
	 * itemIndex        指定的Item顺序,如果是正值，那么往下移动，负值往上移动
	 * Presses on a {@link android.widget.Spinner} (drop-down menu) item.
	 *
	 * @param spinnerIndex the index of the {@code Spinner} menu to be used
	 * @param itemIndex the index of the {@code Spinner} item to be pressed relative to the currently selected item.
	 * A Negative number moves up on the {@code Spinner}, positive moves down
	 */

	public void pressSpinnerItem(int spinnerIndex, int itemIndex)
	{	//在屏幕上点击一个指定下拉列表项
		clicker.clickOnScreen(waiter.waitForAndGetView(spinnerIndex, Spinner.class));
		//等待对话框打开
		dialogUtils.waitForDialogToOpen(Timeout.getSmallTimeout(), true);
		//向下移动一步
		try{
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		}catch(SecurityException ignored){}
		//如果指定的的index为正值，则向下移动
		boolean countingUp = true;
		//转换itemIndex为正数
		if(itemIndex < 0){
			//如果指定的的index为负值，则向上移动
			countingUp = false;
			itemIndex *= -1;
		}
		//移动到itemIndex所在位置
		for(int i = 0; i < itemIndex; i++)
		{
			sleeper.sleepMini();
			//向下移动
			if(countingUp){
				try{
					inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
				}catch(SecurityException ignored){}
			//向上移动
			}else{
				try{
					inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
				}catch(SecurityException ignored){}
			}
		}
		//点击确定
		try{
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
		}catch(SecurityException ignored){}
	}
}
