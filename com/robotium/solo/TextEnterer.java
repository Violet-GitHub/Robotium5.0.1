package com.robotium.solo;

import junit.framework.Assert;
import android.app.Instrumentation;
import android.text.InputType;
import android.widget.EditText;


/**
 * 文本编辑工具类，包括edittext文本录入或文本追加方法
 * Contains setEditText() to enter text into text fields.
 * 
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

class TextEnterer{
	//事件发送器
	private final Instrumentation inst;
	//点击工具类
	private final Clicker clicker;
	//弹框工具类
	private final DialogUtils dialogUtils;

	/**
	 * Constructs this object.
	 * 
	 * @param inst the {@code Instrumentation} instance
	 * @param clicker the {@code Clicker} instance
	 * @param dialogUtils the {@code DialogUtils} instance
	 * 
	 */

	public TextEnterer(Instrumentation inst, Clicker clicker, DialogUtils dialogUtils) {
		this.inst = inst;
		this.clicker = clicker;
		this.dialogUtils = dialogUtils;
	}

	/**
	 * 设置EditText内容,如设置文本不为空，则在原有内容上追加，空则清空原有内容
	 * Sets an {@code EditText} text
	 * 
	 * @param index the index of the {@code EditText} 
	 * @param text the text that should be set
	 */

	public void setEditText(final EditText editText, final String text) {
		//null判断
		if(editText != null){
			//获取editText中的字符串
			final String previousText = editText.getText().toString();
			// 在主线程中执行，避免跨线程报错
			inst.runOnMainSync(new Runnable()
			{
				public void run()
				{	// 清空原有内容
					editText.setInputType(InputType.TYPE_NULL); 
					// 把焦点切换到editText
					editText.performClick();
					//隐藏软键盘
					dialogUtils.hideSoftKeyboard(editText, false, false);
					//如果text为空字符，则清空原内容
					if(text.equals(""))
						editText.setText(text);
					//如果非空，在在原内容上追加
					else{
						editText.setText(previousText + text);
						//移除焦点；
						editText.setCursorVisible(false);
					}
				}
			});
		}
	}
	
	/**
	 * editText文本录入
	 * Types text in an {@code EditText} 
	 * 
	 * @param index the index of the {@code EditText} 
	 * @param text the text that should be typed
	 */

	public void typeText(final EditText editText, final String text){
		//null判断
		if(editText != null){
			//开启一个子线程清空EditText中的文本内容
			inst.runOnMainSync(new Runnable()
			{
				public void run()
				{	//清空EditText中的原内容
					editText.setInputType(InputType.TYPE_NULL);
				}
			});
			// 把焦点切换到editText
			clicker.clickOnScreen(editText, false, 0);
			//隐藏软键盘
			dialogUtils.hideSoftKeyboard(editText, true, true);
			//录入状态标识，初始为false
			boolean successfull = false;
			int retry = 0;
			//尝试10次
			while(!successfull && retry < 10) {
				try{
					//开始文本录入
					inst.sendStringSync(text);
					successfull = true;
				}catch(SecurityException e){
					//如果失败，可能由软键盘影响，隐藏软键盘，继续重试
					dialogUtils.hideSoftKeyboard(editText, true, true);
					retry++;
				}
			}
			//如果没有成功，则先记录日志后退出
			if(!successfull) {
				Assert.fail("Text can not be typed!");
			}
		}
	}
}
