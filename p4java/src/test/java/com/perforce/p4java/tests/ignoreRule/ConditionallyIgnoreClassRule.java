/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perforce.p4java.tests.ignoreRule;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConditionallyIgnoreClassRule
        implements TestRule {
    private final IgnoreCondition condition;
    private final List<TestRule> chained;

    public static ConditionallyIgnoreClassRule ifWindows(String reason, TestRule... chained) {
        return new ConditionallyIgnoreClassRule(new OSCondition.IsWindows(reason), chained);
    }

    public ConditionallyIgnoreClassRule(IgnoreCondition condition, TestRule... chained) {
        Assert.assertNotNull(condition);
        this.condition = condition;
        List<TestRule> chainedList = Arrays.asList(chained);
        Collections.reverse(chainedList);
        this.chained = Collections.unmodifiableList(chainedList);
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        Statement next = statement;
        for (TestRule testRule : chained) {
            next = testRule.apply(next, description);
        }
        return createStatement(next);
    }

    private Statement createStatement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate()
                    throws Throwable {
                Assume.assumeFalse(condition.reason(), condition.isSatisfied());
                base.evaluate();
            }
        };
    }
}
