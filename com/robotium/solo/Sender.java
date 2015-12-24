package com.robotium.solo;


import junit.framework.Assert;
import android.app.Instrumentation;
import android.view.KeyEvent;

/**
 * 按键事件工具类
 * Contains send key event methods. Examples are:
 * sendKeyCode(), goBack()
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class Sender {
	//事件发送器
	private final Instrumentation inst;
	//休息工具类
	private final Sleeper sleeper;

	/**
	 * Constructs this object.
	 * 
	 * @param inst the {@code Instrumentation} instance
	 * @param sleeper the {@code Sleeper} instance
	 */

	Sender(Instrumentation inst, Sleeper sleeper) {
		this.inst = inst;
		this.sleeper = sleeper;
	}

	/**
	 * 通过instrumentation发送各类按键事件.
	 * Tells Robotium to send a key code: Right, Left, Up, Down, Enter or other.
	 * 
	 * @param keycode the key code to be sent. Use {@link KeyEvent#KEYCODE_ENTER}, {@link KeyEvent#KEYCODE_MENU}, {@link KeyEvent#KEYCODE_DEL}, {@link KeyEvent#KEYCODE_DPAD_RIGHT} and so on
	 */

	public void sendKeyCode(int keycode)
	{	// 等待500ms
		sleeper.sleep();
		try{
			//发送各类按键事件
			inst.sendCharacterSync(keycode);
			// 捕获可能遇到的权限问题
		}catch(SecurityException e){
			// 日志提醒，该操作无权和相关错误日志
			Assert.fail("Can not complete action! ("+(e != null ? e.getClass().getName()+": "+e.getMessage() : "null")+")");
		}
	}

	/**
	 * 模拟点击实体返回按键
	 * Simulates pressing the hardware back key.
	 */

	public void goBack() {
		sleeper.sleep();
		try {
			//发送返回事件
			inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
			sleeper.sleep();
		} catch (Throwable ignored) {}
	}
}
