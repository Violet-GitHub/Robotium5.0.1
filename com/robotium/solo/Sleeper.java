package com.robotium.solo;
//休息工具类
class Sleeper {
	//间隔500ms
	private final int PAUSE = 500;
	//最小间隔300ms
	private final int MINIPAUSE = 300;

	/**
	 * 当前线程默认休息500ms
	 * Sleeps the current thread for a default pause length.
	 */

	public void sleep() {
        sleep(PAUSE);
	}


	/**
	 * 当前线程最小休息300ms
	 * Sleeps the current thread for a default mini pause length.
	 */

	public void sleepMini() {
        sleep(MINIPAUSE);
	}


	/**
	 * 当前线程休息指定时间
	 * Sleeps the current thread for <code>time</code> milliseconds.
	 *
	 * @param time the length of the sleep in milliseconds
	 */

	public void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ignored) {}
	}

}
