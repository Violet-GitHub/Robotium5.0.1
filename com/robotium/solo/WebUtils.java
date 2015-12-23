package com.robotium.solo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import com.robotium.solo.Solo.Config;
import android.app.Activity;
import android.app.Instrumentation;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.TextView;


/**
 * web工具类
 * Contains web related methods. Examples are:
 * enterTextIntoWebElement(), getWebTexts(), getWebElements().
 * 
 * @author Renas Reda, renas.reda@robotium.com
 * 
 */

class WebUtils {
	//view获取工具类
	private ViewFetcher viewFetcher;
	//事件发送器
	private Instrumentation inst;
	//Activity工具类
	private ActivityUtils activityUtils;
	//Robotium定制的WebClient
	RobotiumWebClient robotiumWebCLient;
	//WebElement构造工具方法
	WebElementCreator webElementCreator;
	//原生WebChromeClient 保留，不需要Robotium修改的使用原生的执行
	WebChromeClient originalWebChromeClient = null;
	// 配置文件
	private Config config;


	/**
	 * Constructs this object.
	 * 
	 * @param config the {@code Config} instance
	 * @param instrumentation the {@code Instrumentation} instance
	 * @param activityUtils the {@code ActivityUtils} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 */

	public WebUtils(Config config, Instrumentation instrumentation, ActivityUtils activityUtils, ViewFetcher viewFetcher, Sleeper sleeper){
		this.config = config;
		this.inst = instrumentation;
		this.activityUtils = activityUtils;
		this.viewFetcher = viewFetcher;
		webElementCreator = new WebElementCreator(sleeper);
		robotiumWebCLient = new RobotiumWebClient(instrumentation, webElementCreator);
	}

	/**
	 * 返回webViews中的所有可见textView
	 * Returns {@code TextView} objects based on web elements shown in the present WebViews
	 * 
	 * @param onlyFromVisibleWebViews true if only from visible WebViews
	 * @return an {@code ArrayList} of {@code TextViews}s created from the present {@code WebView}s 
	 */

	public ArrayList<TextView> getTextViewsFromWebView(){
		//判断能否执行给定的javascript函数
		boolean javaScriptWasExecuted = executeJavaScriptFunction("allTexts();");	
		//返回webViews上的所有可见的textView
		return createAndReturnTextViewsFromWebElements(javaScriptWasExecuted);	
	}

	/**
	 * 创造并返回webElemnts上的所有可见的textView
	 * Creates and returns TextView objects based on WebElements
	 * 
	 * @return an ArrayList with TextViews
	 */

	private ArrayList <TextView> createAndReturnTextViewsFromWebElements(boolean javaScriptWasExecuted){
		ArrayList<TextView> webElementsAsTextViews = new ArrayList<TextView>();
		//如果JavaScript函数已经被执行，返回webViews上的所有可见的textView
		if(javaScriptWasExecuted){
			for(WebElement webElement : webElementCreator.getWebElementsFromWebViews()){
				if(isWebElementSufficientlyShown(webElement)){
					RobotiumTextView textView = new RobotiumTextView(inst.getContext(), webElement.getText(), webElement.getLocationX(), webElement.getLocationY());
					webElementsAsTextViews.add(textView);
				}
			}	
		}
		return webElementsAsTextViews;		
	}

	/**
	 * 返回webView中的所有webElements
	 * Returns an ArrayList of WebElements currently shown in the active WebView.
	 * 
	 * @param onlySufficientlyVisible true if only sufficiently visible {@link WebElement} objects should be returned
	 * @return an {@code ArrayList} of the {@link WebElement} objects shown in the active WebView
	 */

	public ArrayList<WebElement> getWebElements(boolean onlySufficientlyVisible){
		//在webview中执行指定的JavaScript函数，完成后返回true
		boolean javaScriptWasExecuted = executeJavaScriptFunction("allWebElements();");
		//返回所有的webElements
		return getWebElements(javaScriptWasExecuted, onlySufficientlyVisible);
	}

	/**
	 * 返回当前webView上的所有webElements
	 * Returns an ArrayList of WebElements of the specified By object currently shown in the active WebView.
	 * 
	 * @param by the By object. Examples are By.id("id") and By.name("name")
	 * @param onlySufficientlyVisible true if only sufficiently visible {@link WebElement} objects should be returned
	 * @return an {@code ArrayList} of the {@link WebElement} objects currently shown in the active WebView 
	 */

	public ArrayList<WebElement> getWebElements(final By by, boolean onlySufficientlyVisbile){
		//执行javascript函数，并返回是否执行成功
		boolean javaScriptWasExecuted = executeJavaScript(by, false);
		//该判断目前还没使用,2条路径相同业务逻辑
		if(config.useJavaScriptToClickWebElements){
			//如果执行失败，返回一个空arrayList对象；
			if(!javaScriptWasExecuted){
				return new ArrayList<WebElement>();
			}
			//返回一个空的arrayList对象
			return webElementCreator.getWebElementsFromWebViews();
		}
		//返回所有可见webElement
		return getWebElements(javaScriptWasExecuted, onlySufficientlyVisbile);
	}

	/**
	 * 返回所有可见webElement
	 * javaScriptWasExecuted  为true，代表JavaScript函数已经执行
	 * onlySufficientlyVisbile  为true，只返回可见的webElement，为false，则返回所有webElement
	 * Returns the sufficiently shown WebElements
	 * 
	 * @param javaScriptWasExecuted true if JavaScript was executed
	 * @param onlySufficientlyVisible true if only sufficiently visible {@link WebElement} objects should be returned
	 * @return the sufficiently shown WebElements
	 */

	private ArrayList<WebElement> getWebElements(boolean javaScriptWasExecuted, boolean onlySufficientlyVisbile){
		//构造一个装webElement的arrayList数组
		ArrayList<WebElement> webElements = new ArrayList<WebElement>();
		//JavaScript执行了，
		if(javaScriptWasExecuted){
			//通过webElementCreator获取当前所有的webElement，并判断是否可见，添加到webElements容器中
			for(WebElement webElement : webElementCreator.getWebElementsFromWebViews()){
				if(!onlySufficientlyVisbile){
					webElements.add(webElement);
				}
				else if(isWebElementSufficientlyShown(webElement)){
					webElements.add(webElement);
				}
			}
		}
		return webElements;
	}

	/**
	 * 准备开始执行JavaScript函数，就是把robotium.js文件中的内容转换成字符串的形式，并返回
	 * Prepares for start of JavaScript execution
	 * 
	 * @return the JavaScript as a String
	 */

	private String prepareForStartOfJavascriptExecution(){
		//准备创造webElement类
		webElementCreator.prepareForStart();
		//获取当前的webChromeClient
		WebChromeClient currentWebChromeClient = getCurrentWebChromeClient();
		//null值判断并赋值给originalWebChromeClient
		if(currentWebChromeClient != null && !currentWebChromeClient.getClass().isAssignableFrom(RobotiumWebClient.class)){
			originalWebChromeClient = getCurrentWebChromeClient();	
		}
		//通过robotiumWebClient对象执行javascript函数
		robotiumWebCLient.enableJavascriptAndSetRobotiumWebClient(viewFetcher.getCurrentViews(WebView.class, true), originalWebChromeClient);
		//返回robotium.js文件的字符串
		return getJavaScriptAsString();
	}
	
	/**
	 * 通过反射获取webChromeClient对象
	 * Returns the current WebChromeClient through reflection
	 * 
	 * @return the current WebChromeClient
	 * 
	 */

	private WebChromeClient getCurrentWebChromeClient(){
		WebChromeClient currentWebChromeClient = null;
		//获取当前最新的webView
		Object currentWebView = viewFetcher.getFreshestView(viewFetcher.getCurrentViews(WebView.class, true));
		// 高版本才用反射获取
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			try{
				// 反射获取相关对象
				currentWebView = new Reflect(currentWebView).field("mProvider").out(Object.class);
			}catch(IllegalArgumentException ignored) {}
		}

		try{
			// 高版本才用反射获取
			if (android.os.Build.VERSION.SDK_INT >= 19) {
				// 反射获取相关对象
				Object mClientAdapter = new Reflect(currentWebView).field("mContentsClientAdapter").out(Object.class);
				// 获取属性并转化成WebChromeClient对象
				currentWebChromeClient = new Reflect(mClientAdapter).field("mWebChromeClient").out(WebChromeClient.class);
			}
			else {
				// 反射获取相关对象
				Object mCallbackProxy = new Reflect(currentWebView).field("mCallbackProxy").out(Object.class);
				// 获取属性并转化成WebChromeClient对象
				currentWebChromeClient = new Reflect(mCallbackProxy).field("mWebChromeClient").out(WebChromeClient.class);
			}
		}catch(Exception ignored){}
		//返回currentWebChromeClient对象
		return currentWebChromeClient;
	}

	/**
	 * 通过by方法在一个web Element元素中输入指定text
	 * Enters text into a web element using the given By method
	 * 
	 * @param by the By object e.g. By.id("id");
	 * @param text the text to enter
	 */

	public void enterTextIntoWebElement(final By by, final String text){
		//按照ID查找WebElement对象的输入
		if(by instanceof By.Id){
			executeJavaScriptFunction("enterTextById(\""+by.getValue()+"\", \""+text+"\");");
		}
		//按照Xpath查找WebElement对象的输入
		else if(by instanceof By.Xpath){
			executeJavaScriptFunction("enterTextByXpath(\""+by.getValue()+"\", \""+text+"\");");
		}
		//按照CssSelector查找WebElement对象的输入
		else if(by instanceof By.CssSelector){
			executeJavaScriptFunction("enterTextByCssSelector(\""+by.getValue()+"\", \""+text+"\");");
		}
		//按照Name查找WebElement对象的输入
		else if(by instanceof By.Name){
			executeJavaScriptFunction("enterTextByName(\""+by.getValue()+"\", \""+text+"\");");
		}
		//按照ClassName查找WebElement对象的输入
		else if(by instanceof By.ClassName){
			executeJavaScriptFunction("enterTextByClassName(\""+by.getValue()+"\", \""+text+"\");");
		}
		//按照Text查找WebElement对象的输入
		else if(by instanceof By.Text){
			executeJavaScriptFunction("enterTextByTextContent(\""+by.getValue()+"\", \""+text+"\");");
		}
		//按照TagName查找WebElement对象的输入
		else if(by instanceof By.TagName){
			executeJavaScriptFunction("enterTextByTagName(\""+by.getValue()+"\", \""+text+"\");");
		}
	}

	/**
	 * 根据by对象执行JavaScript
	 * Executes JavaScript determined by the given By object
	 * 
	 * @param by the By object e.g. By.id("id");
	 * @param shouldClick true if click should be performed
	 * @return true if JavaScript function was executed
	 */

	public boolean executeJavaScript(final By by, boolean shouldClick){
		// 拼接按照Id执行的JavaScript脚本
		if(by instanceof By.Id){
			return executeJavaScriptFunction("id(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		// 拼接按照Xpath执行的JavaScript脚本
		else if(by instanceof By.Xpath){
			return executeJavaScriptFunction("xpath(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		// 拼接按照CssSelector执行的JavaScript脚本
		else if(by instanceof By.CssSelector){
			return executeJavaScriptFunction("cssSelector(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		// 拼接按照Name执行的JavaScript脚本
		else if(by instanceof By.Name){
			return executeJavaScriptFunction("name(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		// 拼接按照ClassName执行的JavaScript脚本
		else if(by instanceof By.ClassName){
			return executeJavaScriptFunction("className(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		// 拼接按照Text执行的JavaScript脚本
		else if(by instanceof By.Text){
			return executeJavaScriptFunction("textContent(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		// 拼接按照TagName执行的JavaScript脚本
		else if(by instanceof By.TagName){
			return executeJavaScriptFunction("tagName(\""+by.getValue()+"\", \"" + String.valueOf(shouldClick) + "\");");
		}
		return false;
	}

	/**
	 * 执行给定的JavaScript函数
	 * Executes the given JavaScript function
	 * 
	 * @param function the function as a String
	 * @return true if JavaScript function was executed
	 */
	
	private boolean executeJavaScriptFunction(final String function){
		//在webView当前的view中获取当前试图中最新的webview
		final WebView webView = viewFetcher.getFreshestView(viewFetcher.getCurrentViews(WebView.class, true));
		//判断是否找到文本View
		if(webView == null){
			return false;
		}
		//把robotium.js文件转换成字符串并设置webframe
		final String javaScript = setWebFrame(prepareForStartOfJavascriptExecution());
		Activity activity = activityUtils.getCurrentActivity(false);
		if(activity != null){
		// 如果Activity不为空，直接启动UI线程，在WebView中加载相关JavaScript	
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if(webView != null){
					webView.loadUrl("javascript:" + javaScript + function);
				}
			}
		});
		}
		else{
			//否则通过事件发送器运行异步线程，在webView中加载相关javascript
			inst.runOnMainSync(new Runnable() {
			public void run() {
				if(webView != null){
					webView.loadUrl("javascript:" + javaScript + function);
				}
			}
		});
		}
		return true;
	}
	
	//给JavaScript字符串设置下webframe，并返回
	private String setWebFrame(String javascript){
		//？？？？
		String frame = config.webFrame;
		//根据frame判断，不需要转换，直接返回javaScript
		if(frame.isEmpty() || frame.equals("document")){
			return javascript;
		}
		//否则，就进行转换一下，在返回javascript
		javascript = javascript.replaceAll(Pattern.quote("document, "), "document.getElementById(\""+frame+"\").contentDocument, ");
		javascript = javascript.replaceAll(Pattern.quote("document.body, "), "document.getElementById(\""+frame+"\").contentDocument, ");
		return javascript;
	}

	/**
	 * 判断指定webElement是否可见
	 * Returns true if the view is sufficiently shown
	 *
	 * @param view the view to check
	 * @return true if the view is sufficiently shown
	 */

	public final boolean isWebElementSufficientlyShown(WebElement webElement){
		//获取当前最新的webView
		final WebView webView = viewFetcher.getFreshestView(viewFetcher.getCurrentViews(WebView.class, true));
		//new个数组，放坐标
		final int[] xyWebView = new int[2];
		
		if(webView != null && webElement != null){
			//获取webView当前的xy坐标
			webView.getLocationOnScreen(xyWebView);
			//如果，webelement的一半以上在webView外，则视为不可见，否则可见
			if(xyWebView[1] + webView.getHeight() > webElement.getLocationY())
				return true;
		}
		return false;
	}
	
	/**
	 * 按照大写字母分割字符串，各字符串之间添加空格 ,并转换成小写
	 * Splits a name by upper case.
	 * 
	 * @param name the name to split
	 * @return a String with the split name
	 * 
	 */

	public String splitNameByUpperCase(String name) {
		//按照大写字母分割字符串
		String [] texts = name.split("(?=\\p{Upper})");
		StringBuilder stringToReturn = new StringBuilder();

		for(String string : texts){
			if(stringToReturn.length() > 0) {
				//各字符串之间添加空格 ,并转换成小写
				stringToReturn.append(" " + string.toLowerCase());
			}
			else {
				stringToReturn.append(string.toLowerCase());
			}
		}
		return stringToReturn.toString();
	}

	/**
	 * 以一个string形式返回JavaScript file “robotiumWeb.js”
	 * Returns the JavaScript file RobotiumWeb.js as a String
	 *  
	 * @return the JavaScript file RobotiumWeb.js as a {@code String} 
	 */

	private String getJavaScriptAsString() {
		//获取一个输入流对象，把robotiumWeb.js文件输入
		InputStream fis = getClass().getResourceAsStream("RobotiumWeb.js");
		//new一个string缓冲流
		StringBuffer javaScript = new StringBuffer();

		try {
			//new一个输入流，把robotium.js中的每一行输入到缓冲流JavaScript对象中
			BufferedReader input =  new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while (( line = input.readLine()) != null){
				javaScript.append(line);
				javaScript.append("\n");
			}
			input.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//以一个string形式返回
		return javaScript.toString();
	}
}