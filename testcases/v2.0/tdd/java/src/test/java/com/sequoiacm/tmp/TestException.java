package com.sequoiacm.tmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestException extends Exception {
    private final static Logger logger = LoggerFactory.getLogger(TestException.class);

    public TestException(String message, Throwable e) {
        super(message, e);
    }

    @Override
    public String toString() {
        return super.toString() + ",haha";
    }

    public static void main(String[] args) {
        RuntimeException rte = new RuntimeException("RuntimeException1");
        RuntimeException rte2 = new RuntimeException("RuntimeException2", rte);
        TestException e = new TestException("aa", rte2);

        logger.info("nihao", e);
    }
}
