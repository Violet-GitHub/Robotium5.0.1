package com.robotium.solo;

import java.util.ArrayList;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerProperties;
import android.view.MotionEvent.PointerCoords;
import android.view.InputDevice;
import android.app.Instrumentation;
import android.os.SystemClock;

/**
 * 屏幕画图工具类
 * A class that draws Illustrations to the screen
 *
 * @author Jake Kuli, 3kajjak3@gmail.com
 */
class Illustrator {
	//事件发送工具类
    private Instrumentation inst;

    public Illustrator(Instrumentation inst) {
        this.inst = inst;
    }
    /**
     * 
     * @param illustration
     */
    public void illustrate(Illustration illustration) {
        //null值和坐标点为空判断，为空则抛出异常
    	if (illustration == null || illustration.getPoints().isEmpty()) {
            throw new IllegalArgumentException("Illustration must not be null and requires at least one point.");
        }
    	//构造事件
        MotionEvent event;
        //
        int currentAction;
        //初始化时间
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        //构造坐标集合数组
        PointerCoords[] coords = new PointerCoords[1];
        PointerCoords coord = new PointerCoords();
        PointerProperties[] properties = new PointerProperties[1];
        PointerProperties prop = new PointerProperties();
        prop.id = 0;
        prop.toolType = illustration.getToolType();
        properties[0] = prop;
        coords[0] = coord;
        //获取插画中所有的坐标点，并存储在points数组中
        ArrayList<PressurePoint> points = illustration.getPoints();
        //画图
        for (int i = 0; i < points.size(); i++) {
            PressurePoint currentPoint = points.get(i);
            coord.x = currentPoint.x;
            coord.y = currentPoint.y;
            coord.pressure = currentPoint.pressure;
            coord.size = 1;
            if (i == 0) {
                currentAction = MotionEvent.ACTION_DOWN;
            }
            else {
                currentAction = MotionEvent.ACTION_MOVE;
            }
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime,
                eventTime,
                currentAction,
                1,
                properties,
                coords,
                0, 0, 1, 1, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
            try {
			    inst.sendPointerSync(event);
		    }
            catch (SecurityException ignored) {}
        }
        //停止画图
        currentAction = MotionEvent.ACTION_UP;
        coords[0] = coord;
        PressurePoint currentPoint = points.get(points.size() - 1);
        coord.x = currentPoint.x;
        coord.y = currentPoint.y;
        coord.pressure = currentPoint.pressure;
        coord.size = 1;
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime,
            eventTime,
            currentAction,
            1,
            properties,
            coords,
            0, 0, 1, 1, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0);
        try {
			inst.sendPointerSync(event);
		}
        catch (SecurityException ignored) {}
    }
}
