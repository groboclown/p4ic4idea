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
package net.groboclown.p4.server.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigProblemTest {
    @Test
    public void testReduceCamelCase1() {
        assertThat(
                ConfigProblem.reduceCamelCase("ThisIsATest"),
                is("this is atest")
        );
    }

    @Test
    public void testReduceCamelCase2() {
        assertThat(
                ConfigProblem.reduceCamelCase("thisIsA Test"),
                is("this is a test")
        );
    }

    @Test
    public void testCleanMessage1() {
        assertThat(
                ConfigProblem.cleanMessage(new Exception("This is a message")),
                is("This is a message")
        );
    }

    @Test
    public void testCleanMessage2() {
        assertThat(
                ConfigProblem.cleanMessage(new Exception("Perforce %'CrummyMessage'% Issue")),
                is("Perforce 'CrummyMessage' Issue")
        );
    }

    @Test
    public void testCleanMessage3() {
        assertThat(
                ConfigProblem.cleanMessage(new NullPointerException()),
                is("null pointer exception")
        );
    }}