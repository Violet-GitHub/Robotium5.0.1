package junit.framework;

/**
 * Thrown when an assertion failed.
 */
public class AssertionFailedError extends Error {
	/**
	 * 构造方法
	 */
    public AssertionFailedError () {
    }
    /**
     * 带参数的构造方法 
     */
    public AssertionFailedError (String message) {
        //调用父类方法，抛出异常
    	super (message);
    }
}
