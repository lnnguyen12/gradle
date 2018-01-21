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

package org.gradle.test.fixtures.junitplatform

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Instructs Spock to use {@link RepeatWithJUnitPlatformExtension} to rewrite the tests with JUnit platform and executes the test again:
 * <ul>
 *   <li>dependencies &#123; testCompile 'junit:junit:4.12' &#125; -> dependencies &#123; testCompile 'junit:junit:4.12', 'org.junit.platform:junit-platform-launcher:1.0.2' &#125; </li>
 *   <li>org.junit.Test -> org.junit.jupiter.api.Test</li>
 *   <li>org.junit.Before -> org.junit.jupiter.api.BeforeEach</li>
 *   <li>org.junit.After -> org.junit.jupiter.api.AfterEach</li>
 *   <li>org.junit.BeforeClass -> org.junit.jupiter.api.BeforeAll</li>
 *   <li>org.junit.AfterClass -> org.junit.jupiter.api.AfterAll</li>
 *   <li>org.junit.Ignore -> org.junit.jupiter.api.Disabled</li>
 * </ul>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@ExtensionAnnotation(RepeatWithJUnitPlatformExtension)
@interface RepeatWithJUnitPlatform {
}
