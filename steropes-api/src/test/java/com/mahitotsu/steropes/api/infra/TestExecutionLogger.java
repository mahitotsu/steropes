package com.mahitotsu.steropes.api.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class TestExecutionLogger implements TestExecutionListener {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void beforeTestClass(@NonNull final TestContext testContext) throws Exception {
        this.logger.info("START TestClass: {}", testContext.getTestClass().getCanonicalName());
    }

    public void afterTestClass(@NonNull TestContext testContext) throws Exception {
        this.logger.info("END   TestClass: {}", testContext.getTestClass().getCanonicalName());
    }

    public void beforeTestMethod(@NonNull TestContext testContext) throws Exception {
        this.logger.info("START TestMethod: {}#{}", testContext.getTestClass().getCanonicalName(),
                testContext.getTestMethod().getName());
    }

    public void afterTestMethod(@NonNull TestContext testContext) throws Exception {
        this.logger.info("END   TestMethod: {}#{}", testContext.getTestClass().getCanonicalName(),
                testContext.getTestMethod().getName());
    }
}
