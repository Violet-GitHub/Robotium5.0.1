package junit.framework;

/**
 * 断言设置方法
 * A set of assert methods.  Messages are only displayed when an assert fails.
 */

public class Assert {
    /**
     * 构造方法
     * Protect constructor since it is a static only class
     */
    protected Assert() {
    }

    /**
     * 断言condition为true，不是则打印message异常信息
     * Asserts that a condition is true. If it isn't it throws
     * an AssertionFailedError with the given message.
     */
    static public void assertTrue(String message, boolean condition) {
        //条件断言，当为false时，打印message
    	if (!condition)
            fail(message);
    }
    /**
     * 断言condition为true，不为true抛出异常
     * Asserts that a condition is true. If it isn't it throws
     * an AssertionFailedError.
     */
    static public void assertTrue(boolean condition) {
    	//直接调用上一步断言方法
    	assertTrue(null, condition);
    }
    /**
     * 断言condition为false，不是则打印message异常信息
     * Asserts that a condition is false. If it isn't it throws
     * an AssertionFailedError with the given message.
     */
    static public void assertFalse(String message, boolean condition) {
        //这里聪明,采用!condition
    	assertTrue(message, !condition);
    }
    /**
     * 断言condition为false，如果不是抛出异常
     * Asserts that a condition is false. If it isn't it throws
     * an AssertionFailedError.
     */
    static public void assertFalse(boolean condition) {
        //调用带参数的方法
    	assertFalse(null, condition);
    }
    /**
     * 带参数的断言失败方法
     * Fails a test with the given message.
     */
    static public void fail(String message) {
        //抛出异常
    	throw new AssertionFailedError(message);
    }
    /**
     * 不带参的断言失败方法
     * Fails a test with no message.
     */
    static public void fail() {
        //调用带参数的方法
    	fail(null);
    }
    /**
     * 带message参数的断言object相等方法
     * Asserts that two objects are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, Object expected, Object actual) {
        //null值判断
    	if (expected == null && actual == null)
            return;
        //如果object相等，则直接return
    	if (expected != null && expected.equals(actual))
            return;
    	//如果不相等，则断言相等失败，并固定格式打印出失败信息
        failNotEquals(message, expected, actual);
    }
    /**
     * 不带message参数的断言object相等方法
     * Asserts that two objects are equal. If they are not
     * an AssertionFailedError is thrown.
     */
    static public void assertEquals(Object expected, Object actual) {
        //调用带参数的方法
    	assertEquals(null, expected, actual);
    }
    /**
     * 带参数，断言2个string相等
     * Asserts that two Strings are equal.
     */
    static public void assertEquals(String message, String expected, String actual) {
        //null值判断
    	if (expected == null && actual == null)
            return;
        //如果相等，则直接return
    	if (expected != null && expected.equals(actual))
            return;
        //如果不相等，则构造一个失败异常并抛出
    	throw new ComparisonFailure(message, expected, actual);
    }
    /**
     * 不带参数，断言2个string相等
     * Asserts that two Strings are equal.
     */
    static public void assertEquals(String expected, String actual) {
        //调用不带参数断言方法
    	assertEquals(null, expected, actual);
    }
    /**
     * 带message参数，断言2个double类型的数值是否相等，delta是增量值(意思就是说，2个double值差距很小就算相等)
     * Asserts that two doubles are equal concerning a delta.  If they are not
     * an AssertionFailedError is thrown with the given message.  If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(String message, double expected, double actual, double delta) {
        // handle infinity specially since subtracting to infinite values gives NaN and the
        // the following test fails
    	//如果expected是无穷大值
        if (Double.isInfinite(expected)) {
        	//如果expected不等于actual
            if (!(expected == actual))
            	//打印断言失败的日志
                failNotEquals(message, new Double(expected), new Double(actual));
        //Math.abs(expected-actual)=(expected-actual)的绝对值
        } else if (!(Math.abs(expected-actual) <= delta)) // Because comparison with NaN always returns false
            //打印断言失败日志
        	failNotEquals(message, new Double(expected), new Double(actual));
    }
    /**
     * 不带message参数，断言2个double类型的数值是否相等，delta是增量值(意思就是说，2个double值差距很小就算相等)
     * Asserts that two doubles are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(double expected, double actual, double delta) {
        //调用带参数的断言方法
    	assertEquals(null, expected, actual, delta);
    }
    /**
     * 带message参数，断言2个float类型的数值是否相等，delta是增量值(意思就是说，2个float值差距很小就算相等)
     * Asserts that two floats are equal concerning a delta. If they are not
     * an AssertionFailedError is thrown with the given message.  If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(String message, float expected, float actual, float delta) {
         // handle infinity specially since subtracting to infinite values gives NaN and the
        // the following test fails
    	//如果expected是无穷大值
    	if (Float.isInfinite(expected)) {
    		//如果expected不等于actual
    		if (!(expected == actual))
    			//打印断言失败的日志
    			failNotEquals(message, new Float(expected), new Float(actual));
    	//Math.abs(expected-actual)=(expected-actual)的绝对值
    	} else if (!(Math.abs(expected-actual) <= delta))
    		//打印断言失败日志  
    		failNotEquals(message, new Float(expected), new Float(actual));
    }
    /**
     * 不带message参数，断言2个float类型的数值是否相等，delta是增量值(意思就是说，2个float值差距很小就算相等)
     * Asserts that two floats are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(float expected, float actual, float delta) {
    	//调用带参数的断言方法
    	assertEquals(null, expected, actual, delta);
    }
    /**
     * 带message参数，断言2个long类型的数值是否相等
     * Asserts that two longs are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, long expected, long actual) {
        //将long类型转换成object，然后调用断言对象是否相等来判断
    	assertEquals(message, new Long(expected), new Long(actual));
    }
    /**
     * 不带message参数，断言2个long类型的数值是否相等
     * Asserts that two longs are equal.
     */
    static public void assertEquals(long expected, long actual) {
        //调用带参数的断言方法
    	assertEquals(null, expected, actual);
    }
    /**
     * 带message参数，断言2个Boolean值是否相等
     * Asserts that two booleans are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, boolean expected, boolean actual) {
        //将Boolean值转换成object，然后调用断言对象是否相等来判断  
    	assertEquals(message, new Boolean(expected), new Boolean(actual));
      }
    /**
     * 不带message参数，断言2个Boolean值是否相等
     * Asserts that two booleans are equal.
      */
    static public void assertEquals(boolean expected, boolean actual) {
        //调用带参数的断言方法
    	assertEquals(null, expected, actual);
    }
    /**
     * 带message参数，断言2个byte值是否相等
     * Asserts that two bytes are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
      static public void assertEquals(String message, byte expected, byte actual) {
    	  //将Byte值转换成object，然后调用断言对象是否相等来判断
    	  assertEquals(message, new Byte(expected), new Byte(actual));
    }
    /**
     * 不带message参数，断言2个byte值是否相等
     * Asserts that two bytes are equal.
     */
    static public void assertEquals(byte expected, byte actual) {
    	//调用带参数的断言方法
    	assertEquals(null, expected, actual);
    }
    /**
     * 带message参数，断言2个char值是否相等
     * Asserts that two chars are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
      static public void assertEquals(String message, char expected, char actual) {
    	  //将Character值转换成object，然后调用断言对象是否相等来判断  
    	  assertEquals(message, new Character(expected), new Character(actual));
      }
    /**
     * 不带message参数，断言2个char值是否相等
     * Asserts that two chars are equal.
     */
      static public void assertEquals(char expected, char actual) {
    	  //调用带参数的断言方法
    	  assertEquals(null, expected, actual);
    }
    /**
     * 带message参数，断言2个short值是否相等
     * Asserts that two shorts are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, short expected, short actual) {
    	//将Short值转换成object，然后调用断言对象是否相等来判断
    	assertEquals(message, new Short(expected), new Short(actual));
    }
    /**
     * 不带message参数，断言2个short值是否相等
     * Asserts that two shorts are equal.
     */
    static public void assertEquals(short expected, short actual) {
    	//调用带参数的断言方法
    	assertEquals(null, expected, actual);
    }
    /**
     * 带message参数，断言2个Integer值是否相等
     * Asserts that two ints are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
      static public void assertEquals(String message, int expected, int actual) {
    	//将int值转换成object，然后调用断言对象是否相等来判断
    	  assertEquals(message, new Integer(expected), new Integer(actual));
      }
      /**
       * 不带message参数，断言2个int值是否相等
        * Asserts that two ints are equal.
     */
      static public void assertEquals(int expected, int actual) {
    	//调用带参数的断言方法
    	  assertEquals(null, expected, actual);
    }
    /**
     * 不带message参数，断言object不是null值
     * Asserts that an object isn't null.
     */
    static public void assertNotNull(Object object) {
        //调用带参数的方法
    	assertNotNull(null, object);
    }
    /**
     * 带message参数，断言object不是null值
     * Asserts that an object isn't null. If it is
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertNotNull(String message, Object object) {
        //调用断言true方法
    	assertTrue(message, object != null);
    }
    /**
     * 不带message参数，断言object是null值
     * Asserts that an object is null.
     */
    static public void assertNull(Object object) {
        //调用带参数的断言null值方法
    	assertNull(null, object);
    }
    /**
     * 带message参数，断言object是null值
     * Asserts that an object is null.  If it is not
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertNull(String message, Object object) {
    	//调用断言true方法
    	assertTrue(message, object == null);
    }
    /**
     * 带message参数，断言object是相同的
     * Asserts that two objects refer to the same object. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertSame(String message, Object expected, Object actual) {
        //判断相等
    	if (expected == actual)
            return;
    	//断言相同失败，并固定格式打印出失败信息
        failNotSame(message, expected, actual);
    }
    /**
     * 不带message参数，断言object是相同的
     * Asserts that two objects refer to the same object. If they are not
     * the same an AssertionFailedError is thrown.
     */
    static public void assertSame(Object expected, Object actual) {
        //调用带参数的方法
    	assertSame(null, expected, actual);
    }
     /**
      * 带message参数，断言object是不相同的
      * Asserts that two objects do not refer to the same object. If they are
      * an AssertionFailedError is thrown with the given message.
      */
    static public void assertNotSame(String message, Object expected, Object actual) {
        //如果判断相等
    	if (expected == actual)
            //断言不相同失败，按照固定格式打印日志，并抛出异常
    		failSame(message);
    }
    /**
     * 不带message参数，断言object是不相同的
     * Asserts that two objects do not refer to the same object. If they are
     * the same an AssertionFailedError is thrown.
     */
    static public void assertNotSame(Object expected, Object actual) {
        //调用带参数的方法
    	assertNotSame(null, expected, actual);
    }

    /**
     * 断言不相同失败，按照固定格式打印日志，并抛出异常
     * @param message
     */
    static private void failSame(String message) {
        String formatted= "";
         if (message != null)
             formatted= message+" ";
         //断言失败，抛出异常
         fail(formatted+"expected not same");
    }
    /**
     * 断言相同失败，并固定格式打印出失败信息
     * @param message
     * @param expected
     * @param actual
     */
    static private void failNotSame(String message, Object expected, Object actual) {
        String formatted= "";
        if (message != null)
            formatted= message+" ";
        //断言失败方法，按照固定格式打印日志，并抛出异常
        fail(formatted+"expected same:<"+expected+"> was not:<"+actual+">");
    }
    /**
     * 断言相等失败，并固定格式打印出失败信息
     * @param message
     * @param expected
     * @param actual
     */
    static private void failNotEquals(String message, Object expected, Object actual) {
        //断言失败方法抛出异常并打印出断言提示
    	fail(format(message, expected, actual));
    }

    /**
     * 格式化断言失败的打印日志信息，以字符串形式返回
     * @param message
     * @param expected
     * @param actual
     * @return
     */
    static String format(String message, Object expected, Object actual) {
        String formatted= "";
        if (message != null)
            formatted= message+" ";
        //按照固定格式打印出，断言失败的日志
        return formatted+"expected:<"+expected+"> but was:<"+actual+">";
    }
}
