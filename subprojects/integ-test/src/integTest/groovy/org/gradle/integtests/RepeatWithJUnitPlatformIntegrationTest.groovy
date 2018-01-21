/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.junitplatform.RepeatWithJUnitPlatform

class RepeatWithJUnitPlatformIntegrationTest extends AbstractIntegrationSpec {
    private static int iterationCount = 0

    def cleanupSpec() {
        assert iterationCount % 2 == 0
    }

    @RepeatWithJUnitPlatform
    def 'test with JUnit 4 can be executed twice'() {
        given:
        buildFile << '''
apply plugin: 'java'
apply from: 'check.gradle'

dependencies { testCompile 'junit:junit:4.12' }
'''

        file('check.gradle') << '''
task checkSeenFiles {
    doLast {
        def buildGradle = new File('build.gradle').text
        def testJava = new File('src/test/java/Test.java').text
        if("${iteration}"=='first') {
            assert buildGradle.contains('junit:4.12') 
            assert testJava.contains('@Ignore')
            assert !testJava.contains('@Disabled')
        } else {
            assert buildGradle.contains('jupiter') 
            assert !testJava.contains('@Ignore')
            assert testJava.contains('@Disabled')
        }
    }
}
'''
        file('src/test/java/Test.java') << '''
import org.junit.Test;
import org.junit.Ignore;

public class Test {
    @Test
    @Ignore
    public void broken() {
        throw new RuntimeException();
    }    
}
'''

        when:
        args("-Piteration=${getIteration()}")

        then:
        succeeds('checkSeenFiles')
    }

    static getIteration() {
        iterationCount++
        String iteration = (iterationCount % 2 == 1) ? 'first' : 'second'

        return iteration
    }
}
