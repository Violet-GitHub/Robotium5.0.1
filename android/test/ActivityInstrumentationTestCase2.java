/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.test;

import android.app.Activity;
import android.content.Intent;

import java.lang.reflect.Method;

/**
 * This class provides functional testing of a single activity.  The activity under test will
 * be created using the system infrastructure (by calling InstrumentationTestCase.launchActivity())
 * and you will then be able to manipulate your Activity directly.
 * 
 * <p>Other options supported by this test case include:
 * <ul>
 * <li>You can run any test method on the UI thread (see {@link android.test.UiThreadTest}).</li>
 * <li>You can inject custom Intents into your Activity (see 
 * {@link #setActivityIntent(Intent)}).</li>
 * </ul>
 * 
 * <p>This class replaces {@link android.test.ActivityInstrumentationTestCase}, which is deprecated.
 * New tests should be written using this base class.
 * 
 * <p>If you prefer an isolated unit test, see {@link android.test.ActivityUnitTestCase}.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about application testing, read the
 * <a href="{@docRoot}guide/topics/testing/index.html">Testing</a> developer guide.</p>
 * </div>
 */
public abstract class ActivityInstrumentationTestCase2<T extends Activity> 
        extends ActivityTestCase {
	//申明一个测试Activity对象
    Class<T> mActivityClass;
    //初始化触摸方式
    boolean mInitialTouchMode = false;
    //申明一个定制意图对象
    Intent mActivityIntent = null;

    /**
     * 
     * Creates an {@link ActivityInstrumentationTestCase2}.
     *
     * @param pkg ignored - no longer in use.
     * @param activityClass The activity to test. This must be a class in the instrumentation
     * targetPackage specified in the AndroidManifest.xml
     *
     * @deprecated use {@link #ActivityInstrumentationTestCase2(Class)} instead
     */
    @Deprecated
    public ActivityInstrumentationTestCase2(String pkg, Class<T> activityClass) {
        this(activityClass);
    }

    /**
     * 传入Activity名的构造方法
     * Creates an {@link ActivityInstrumentationTestCase2}.
     *
     * @param activityClass The activity to test. This must be a class in the instrumentation
     * targetPackage specified in the AndroidManifest.xml
     */
    public ActivityInstrumentationTestCase2(Class<T> activityClass) {
        //赋值全局变量mActivityClass
    	mActivityClass = activityClass;
    }

    /**
     * 获取全局变量mActivityClass和被测试目标程序包名在一起后的Activity
     * Get the Activity under test, starting it if necessary.
     *
     * For each test method invocation, the Activity will not actually be created until the first
     * time this method is called. 
     * 
     * <p>If you wish to provide custom setup values to your Activity, you may call 
     * {@link #setActivityIntent(Intent)} and/or {@link #setActivityInitialTouchMode(boolean)} 
     * before your first call to getActivity().  Calling them after your Activity has 
     * started will have no effect.
     *
     * <p><b>NOTE:</b> Activities under test may not be started from within the UI thread.
     * If your test method is annotated with {@link android.test.UiThreadTest}, then your Activity
     * will be started automatically just before your test method is run.  You still call this
     * method in order to get the Activity under test.
     * 
     * @return the Activity under test
     */
    @Override
    public T getActivity() {
    	Activity a = super.getActivity();
        //null值判断
    	if (a == null) {
            //设置初始化的触摸方式
    		// set initial touch mode
    		getInstrumentation().setInTouchMode(mInitialTouchMode);
            //获取目标app的context对象，并获取context中的包名
    		final String targetPackage = getInstrumentation().getTargetContext().getPackageName();
            //意向null值判断
    		// inject custom intent, if provided
            if (mActivityIntent == null) {
                //返回将测试Activity和被测试目标程序包名在一起后的Activity
            	a = launchActivity(targetPackage, mActivityClass, null);
            } else {
                a = launchActivityWithIntent(targetPackage, mActivityClass, mActivityIntent);
            }
            setActivity(a);
        }
        return (T) a;
    }

    /**
     * 赋值全局变量intent
     * Call this method before the first call to {@link #getActivity} to inject a customized Intent
     * into the Activity under test.
     * 
     * <p>If you do not call this, the default intent will be provided.  If you call this after
     * your Activity has been started, it will have no effect.
     * 
     * <p><b>NOTE:</b> Activities under test may not be started from within the UI thread.
     * If your test method is annotated with {@link android.test.UiThreadTest}, then you must call
     * {@link #setActivityIntent(Intent)} from {@link #setUp()}.
     *
     * <p>The default Intent (if this method is not called) is:
     *  action = {@link Intent#ACTION_MAIN}
     *  flags = {@link Intent#FLAG_ACTIVITY_NEW_TASK}
     * All other fields are null or empty.
     *
     * @param i The Intent to start the Activity with, or null to reset to the default Intent.
     */
    public void setActivityIntent(Intent i) {
        mActivityIntent = i;
    }
    
    /**
     * 赋值全局变量mInitialTouchMode
     * Call this method before the first call to {@link #getActivity} to set the initial touch
     * mode for the Activity under test.
     * 
     * <p>If you do not call this, the touch mode will be false.  If you call this after
     * your Activity has been started, it will have no effect.
     * 
     * <p><b>NOTE:</b> Activities under test may not be started from within the UI thread.
     * If your test method is annotated with {@link android.test.UiThreadTest}, then you must call
     * {@link #setActivityInitialTouchMode(boolean)} from {@link #setUp()}.
     * 
     * @param initialTouchMode true if the Activity should be placed into "touch mode" when started
     */
    public void setActivityInitialTouchMode(boolean initialTouchMode) {
        mInitialTouchMode = initialTouchMode;
    }
    
    //重写父类setUp方法，用来初始化及完成准备工作
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        mInitialTouchMode = false;
        mActivityIntent = null;
    }
    
    //重写父类tearDown方法，用来结束当前用例，进入下一个用例
    @Override
    protected void tearDown() throws Exception {
        // Finish the Activity off (unless was never launched anyway)
        Activity a = super.getActivity();
        if (a != null) {
            a.finish();
            setActivity(null);
        }
        
        // Scrub out members - protects against memory leaks in the case where someone 
        // creates a non-static inner class (thus referencing the test case) and gives it to
        // someone else to hold onto
        scrubClass(ActivityInstrumentationTestCase2.class);

        super.tearDown();
    }

    /**
     * 重写父类runTest方法
     * Runs the current unit test. If the unit test is annotated with
     * {@link android.test.UiThreadTest}, force the Activity to be created before switching to
     * the UI thread.
     */
    @Override
    protected void runTest() throws Throwable {
        try {
            Method method = getClass().getMethod(getName(), (Class[]) null);
            if (method.isAnnotationPresent(UiThreadTest.class)) {
                getActivity();
            }
        } catch (Exception e) {
            // eat the exception here; super.runTest() will catch it again and handle it properly
        }
        super.runTest();
    }

}
