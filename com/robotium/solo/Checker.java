package com.robotium.solo;

import java.util.ArrayList;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * 检查工具类，提供各种检查方法，检查Button，view是否被选中
 * Contains various check methods. Examples are: isButtonChecked(),
 * isSpinnerTextSelected.
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class Checker {
	//试图获取工具类
	private final ViewFetcher viewFetcher;
	//等待工具类，用于判断各种view，text是否出现
	private final Waiter waiter;

	/**
	 * 构造方法
	 * Constructs this object.
	 * 
	 * @param viewFetcher the {@code ViewFetcher} instance
     * @param waiter the {@code Waiter} instance
	 */
	
	public Checker(ViewFetcher viewFetcher, Waiter waiter){
		this.viewFetcher = viewFetcher;
		this.waiter = waiter;
	}

	
	/**
	 * 检查给定类型的第index个CompoundButton对象，是否被选中
	 * Checks if a {@link CompoundButton} with a given index is checked.
	 *
	 * @param expectedClass the expected class, e.g. {@code CheckBox.class} or {@code RadioButton.class}
	 * @param index of the {@code CompoundButton} to check. {@code 0} if only one is available
	 * @return {@code true} if {@code CompoundButton} is checked and {@code false} if it is not checked
	 */
	
	public <T extends CompoundButton> boolean isButtonChecked(Class<T> expectedClass, int index)
	{	//通过waiter工具在默认时间内找到指定类型的第index个CompoundButton，检查是否被选中
		return (waiter.waitForAndGetView(index, expectedClass).isChecked());
	}
	
	/**
	 * 检查给定字符串的CompoundButton是否被选中
	 * Checks if a {@link CompoundButton} with a given text is checked.
	 *
	 * @param expectedClass the expected class, e.g. {@code CheckBox.class} or {@code RadioButton.class}
	 * @param text the text that is expected to be checked
	 * @return {@code true} if {@code CompoundButton} is checked and {@code false} if it is not checked
	 */

	public <T extends CompoundButton> boolean isButtonChecked(Class<T> expectedClass, String text)
	{	
		//子给定的时间内找到指定字符的button
		T button = waiter.waitForText(expectedClass, text, 0, Timeout.getSmallTimeout(), true);
		//判断button是否被选中
		if(button != null && button.isChecked()){
			return true;
		}
		return false;
	}

	/**
	 * 检查指定text的第一个CheckedTextView类型控件，是否被选中
	 * Checks if a {@link CheckedTextView} with a given text is checked.
	 *
	 * @param checkedTextView the {@code CheckedTextView} object
	 * @param text the text that is expected to be checked
	 * @return {@code true} if {@code CheckedTextView} is checked and {@code false} if it is not checked
	 */

	public boolean isCheckedTextChecked(String text)
	{	
		//在规定时间里，获取给定text的view
		CheckedTextView checkedTextView = waiter.waitForText(CheckedTextView.class, text, 0, Timeout.getSmallTimeout(), true);
		//判断该view是否已被选中
		if(checkedTextView != null && checkedTextView.isChecked()) {
			return true;
		}
		return false;
	}

	
	/**
	 * 检查当前屏幕上的下拉列表中，给定的text是否被选中
	 * Checks if a given text is selected in any {@link Spinner} located on the current screen.
	 * 
	 * @param text the text that is expected to be selected
	 * @return {@code true} if the given text is selected in any {@code Spinner} and false if it is not
	 */
	
	public boolean isSpinnerTextSelected(String text)
	{	//等待并返回下拉列表视图Spinner
		waiter.waitForAndGetView(0, Spinner.class);
		//获取当前的下拉列表Spinner
		ArrayList<Spinner> spinnerList = viewFetcher.getCurrentViews(Spinner.class, true);
		//循环查找判断给定text是否被选中
		for(int i = 0; i < spinnerList.size(); i++){
			if(isSpinnerTextSelected(i, text))
					return true;
		}
		return false;
	}
	
	/**
	 * 检查在指定的Spinner中，给定的text选项是否被选中
	 * Checks if a given text is selected in a given {@link Spinner} 
	 * @param spinnerIndex the index of the spinner to check. 0 if only one spinner is available
	 * @param text the text that is expected to be selected
	 * @return true if the given text is selected in the given {@code Spinner} and false if it is not
	 */
	
	public boolean isSpinnerTextSelected(int spinnerIndex, String text)
	{	
		//获取给定index的spinner
		Spinner spinner = waiter.waitForAndGetView(spinnerIndex, Spinner.class);
		//获取spinner中的textView
		TextView textView = (TextView) spinner.getChildAt(0);
		//得到textView中的text，与给定的text匹配，为true返回选中，为false返回未选中
		if(textView.getText().equals(text))
			return true;
		else
			return false;
	}
}
