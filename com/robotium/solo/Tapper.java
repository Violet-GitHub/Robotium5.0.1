package com.robotium.solo;

import android.app.Instrumentation;
import android.graphics.PointF;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
/**
 * 屏幕点击工具类
 * @author chenqianjiao
 *
 */
class Tapper
{	//事件发送工具类
    private final Instrumentation _instrument;
    //手势动作间隔1s
    public static final int GESTURE_DURATION_MS = 1000;
    //事件间隔10ms
    public static final int EVENT_TIME_INTERVAL_MS = 10;
    
    public Tapper(Instrumentation inst)
    {
        this._instrument = inst;
    }
    /**
     * 生成屏幕点击事件
     * @param numTaps
     * @param points
     */
	public void generateTapGesture(int numTaps, PointF... points)
    {
        MotionEvent event;
        //初始化时间
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        //初始化坐标点
        // pointer 1
        float x1 = points[0].x;
        float y1 = points[0].y;
        
        float x2 = 0;
        float y2 = 0;
        if (points.length == 2)
        {
            // pointer 2
            x2 = points[1].x;
            y2 = points[1].y;
        }
        //构造坐标点数组
        PointerCoords[] pointerCoords = new PointerCoords[points.length];
        PointerCoords pc1 = new PointerCoords();
        pc1.x = x1;
        pc1.y = y1;
        pc1.pressure = 1;
        pc1.size = 1;
        pointerCoords[0] = pc1;
        PointerCoords pc2 = new PointerCoords();
        if (points.length == 2)
        {
            pc2.x = x2;
            pc2.y = y2;
            pc2.pressure = 1;
            pc2.size = 1;
            pointerCoords[1] = pc2;
        }
        
        PointerProperties[] pointerProperties = new PointerProperties[points.length];
        PointerProperties pp1 = new PointerProperties();
        pp1.id = 0;
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER;
        pointerProperties[0] = pp1;
        PointerProperties pp2 = new PointerProperties();
        if (points.length == 2)
        {
            pp2.id = 1;
            pp2.toolType = MotionEvent.TOOL_TYPE_FINGER;
            pointerProperties[1] = pp2;
        }
        // 发送指定数量的点击
        int i = 0;
        while (i != numTaps)
        {	// 发送第一个按下事件
            event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, points.length, pointerProperties,
                    pointerCoords, 0, 0, 1, 1, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0);
            _instrument.sendPointerSync(event);
            // 如果坐标点是2个.那么发送第二个事件
            if (points.length == 2)
            {
                event = MotionEvent
                        .obtain(downTime,
                                eventTime,
                                MotionEvent.ACTION_POINTER_DOWN
                                        + (pp2.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                                points.length, pointerProperties,
                                pointerCoords, 0, 0, 1, 1, 0, 0,
                                InputDevice.SOURCE_TOUCHSCREEN, 0);
                _instrument.sendPointerSync(event);

                eventTime += EVENT_TIME_INTERVAL_MS;
                event = MotionEvent
                        .obtain(downTime,
                                eventTime,
                                MotionEvent.ACTION_POINTER_UP
                                        + (pp2.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                                points.length, pointerProperties,
                                pointerCoords, 0, 0, 1, 1, 0, 0,
                                InputDevice.SOURCE_TOUCHSCREEN, 0);
                _instrument.sendPointerSync(event);
            }
            // 发送松开事件
            eventTime += EVENT_TIME_INTERVAL_MS;
            event = MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_UP, points.length, pointerProperties,
                    pointerCoords, 0, 0, 1, 1, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0);
            _instrument.sendPointerSync(event);
            // 计算+1
            i++;
        }
    }
}
