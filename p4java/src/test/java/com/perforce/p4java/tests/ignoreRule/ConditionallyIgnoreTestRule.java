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
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

public class ConditionallyIgnoreTestRule
        implements MethodRule {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface ConditionalIgnore {
        Class<? extends IgnoreCondition> condition();

        String why();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        Statement result = base;
        if (hasConditionalIgnoreAnnotation(method)) {
            IgnoreCondition condition = getIgnoreContition(target, method);
            if (condition.isSatisfied()) {
                result = new IgnoreStatement(getIgnoreContitionWhy(method), condition);
            }
        }
        return result;
    }

    private static boolean hasConditionalIgnoreAnnotation(FrameworkMethod method) {
        return method.getAnnotation(ConditionalIgnore.class) != null;
    }

    private static IgnoreCondition getIgnoreContition(Object target, FrameworkMethod method) {
        ConditionalIgnore annotation = method.getAnnotation(ConditionalIgnore.class);
        return new IgnoreConditionCreator(target, annotation).create();
    }

    private static String getIgnoreContitionWhy(FrameworkMethod method) {
        ConditionalIgnore annotation = method.getAnnotation(ConditionalIgnore.class);
        return annotation == null ? null : annotation.why();
    }

    private static class IgnoreConditionCreator {
        private final Object target;
        private final Class<? extends IgnoreCondition> conditionType;
        private final IgnoreCondition condition;

        IgnoreConditionCreator(Object target, ConditionalIgnore annotation) {
            this.target = target;
            if (annotation.condition() != null) {
                this.conditionType = annotation.condition();
                this.condition = null;
            } else {
                Assert.fail("Did not specify a non-null ignore condition for " + target);
                throw new IllegalStateException();
            }
        }

        IgnoreCondition create() {
            if (condition != null) {
                return condition;
            }
            checkConditionType();
            try {
                return createCondition();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private IgnoreCondition createCondition()
                throws Exception {
            IgnoreCondition result;
            if (isConditionTypeStandalone()) {
                result = conditionType.newInstance();
            } else {
                result = conditionType.getDeclaredConstructor(target.getClass()).newInstance(target);
            }
            return result;
        }

        private void checkConditionType() {
            if (!isConditionTypeStandalone() && !isConditionTypeDeclaredInTarget()) {
                String msg
                        = "Conditional class '%s' is a member class "
                        + "but was not declared inside the test case using it.\n"
                        + "Either make this class a static class, "
                        + "standalone class (by declaring it in it's own file) "
                        + "or move it inside the test case using it";
                throw new IllegalArgumentException(String.format(msg, conditionType.getName()));
            }
        }

        private boolean isConditionTypeStandalone() {
            return !conditionType.isMemberClass() || Modifier.isStatic(conditionType.getModifiers());
        }

        private boolean isConditionTypeDeclaredInTarget() {
            return target.getClass().isAssignableFrom(conditionType.getDeclaringClass());
        }
    }

    private static class IgnoreStatement
            extends Statement {
        private final String message;

        IgnoreStatement(String why, IgnoreCondition condition) {
            final StringBuilder reason = new StringBuilder("Ignored because ");
            if (condition.reason() == null) {
                reason.append(condition.getClass().getSimpleName());
            } else {
                reason.append(condition.reason());
            }
            if (why != null) {
                reason.append(": ").append(why);
            }
            this.message = new String(reason);
        }

        @Override
        public void evaluate() {
            Assume.assumeTrue(message, false);
        }
    }
}
