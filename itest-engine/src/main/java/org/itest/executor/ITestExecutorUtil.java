/**
 * <pre>
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Grzegorz Kochański
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * </pre>
 */
package org.itest.executor;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.itest.ITestConfig;
import org.itest.ITestExecutor;
import org.itest.definition.ITestDefinition;
import org.itest.execution.ITestMethodExecutionResult;
import org.itest.verify.ITestFieldVerificationResult;

public class ITestExecutorUtil {

    public static ITestExecutor buildExecutor(ITestConfig iTestExecutorConfig) {
        return new ITestExecutorImpl(iTestExecutorConfig);
    }

    private static class ITestExecutorImpl implements ITestExecutor {

        private final ITestConfig itestConfig;

        public ITestExecutorImpl(ITestConfig iTestExecutorConfig) {
            this.itestConfig = iTestExecutorConfig;
        }

        @Override
        public String performTestsFor(int expectedNumberOfAssertions, Class<?>... classes) {
            Collection<ITestDefinition> iTestFlowDefinitions = itestConfig.getITestDefinitionFactory().buildTestFlowDefinitions(classes);
            StringBuilder sb = new StringBuilder();
            int performedAsserts = 0;
            for (ITestDefinition iTestPathDefinition : iTestFlowDefinitions) {
                try {
                    String name = iTestPathDefinition.getITestClass().getName() + "." + iTestPathDefinition.getITestName();
                    // Do not execute tests without 'verify' specified. #4
                    if ( null == iTestPathDefinition.getVeryficationParams() ) {
                        continue;
                    }
                    ITestMethodExecutionResult executionData = itestConfig.getITestMethodExecutor().execute(iTestPathDefinition);
                    Collection<ITestFieldVerificationResult> verificationResult = itestConfig.getITestExecutionVerifier().verify(name, executionData,
                            iTestPathDefinition.getVeryficationParams());
                    for (ITestFieldVerificationResult res : verificationResult) {
                        performedAsserts++;
                        if ( !res.isSuccess() ) {
                            sb.append(res).append('\n');
                        }
                    }
                } catch (InvocationTargetException e) {
                    String name = iTestPathDefinition.getITestClass().getName() + "." + iTestPathDefinition.getITestName();
                    sb.append(name).append(' ').append(e.getTargetException()).append('\n');
                    StackTraceElement[] trace = e.getTargetException().getStackTrace();
                    for (int i = 0; i < trace.length; i++) {
                        sb.append("\tat ").append(trace[i]).append('\n');
                    }

                }
            }
            if ( expectedNumberOfAssertions >= 0 ) {
                if ( expectedNumberOfAssertions > performedAsserts ) {
                    sb.append(performedAsserts).append("/").append(expectedNumberOfAssertions);
                    if ( 1 == expectedNumberOfAssertions - performedAsserts ) {
                        sb.append(": There is 1 assertion missed.");
                    } else {
                        sb.append(": There are ").append(expectedNumberOfAssertions - performedAsserts).append(" assertions missed.");
                    }
                    sb.append(" It may be caused by refactoring of class name, package or method.").append(
                            " Verify your changes with itest files and/or update expendedNumberOfAssertions in ITestExecutor.performTestsFor() if required.");
                } else if ( expectedNumberOfAssertions < performedAsserts ) {
                    sb.append(performedAsserts).append("/").append(expectedNumberOfAssertions);
                    if ( 1 == performedAsserts - expectedNumberOfAssertions ) {
                        sb.append(": It seems, there is 1 new assertion.");
                    } else {
                        sb.append(": It seems, there are ").append(performedAsserts - expectedNumberOfAssertions).append(" new assertions.");
                    }
                    sb.append(" Please update expectedNumberOfAssertions in ITestExecutor.performTestsFor() accordingly.");
                }
            }
            return sb.toString();
        }
    }

}
