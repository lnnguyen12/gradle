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

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.DataProviderInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.NameProvider
import org.spockframework.runtime.model.SpecInfo

class RepeatWithJUnitPlatformExtension extends AbstractAnnotationDrivenExtension<RepeatWithJUnitPlatform> {

    static final String JUNIT4 = "JUnit 4"
    static final String JUNIT_PLATFORM = "JUnit Platform"

    @Override
    void visitSpecAnnotation(RepeatWithJUnitPlatform annotation, SpecInfo spec) {
        for (FeatureInfo feature : spec.features) {
            configureFeature(feature)
        }
    }

    @Override
    void visitFeatureAnnotation(RepeatWithJUnitPlatform annotation, FeatureInfo feature) {
        configureFeature(feature)
    }

    private boolean annotatedWithUnroll(FeatureInfo feature) {
        // @RepeatWithJUnitPlatform can't be used together with @Unroll now
        return !feature.getDataProviders().isEmpty()
    }

    void configureFeature(FeatureInfo feature) {
        if (annotatedWithUnroll(feature)) {
            return
        }
        feature.addDataProvider(createJUnitPlatformDataProvider())
        feature.setDataProcessorMethod(new MethodInfo() {
            @Override
            Object invoke(Object target, Object... arguments) throws Throwable {
                return null
            }

            @Override
            boolean hasBytecodeName(String name) {
                false
            }
        })
        feature.setIterationNameProvider(new NameProvider<IterationInfo>() {
            int iterationCount = 1

            @Override
            String getName(IterationInfo iterationInfo) {
                String suffix = iterationCount % 2 == 1 ? '[with JUnit 4]' : '[with JUnit platform]'
                iterationCount++
                return iterationInfo.feature.name + suffix
            }
        })
        feature.addIterationInterceptor(new IMethodInterceptor() {
            int iterationCount = 1

            @Override
            void intercept(IMethodInvocation invocation) throws Throwable {
                if (iterationCount == 1) {
                    iterationCount++
                } else {
                    assert AbstractIntegrationSpec.isAssignableFrom(invocation.getInstance().class): '@RepeatWithJUnitPlatform can only be annotated on subclasses of AbstractIntegrationSpec!'
                    hookMethods(invocation.getInstance())
                }

                invocation.proceed()
            }

            def hookMethods(AbstractIntegrationSpec instance) {
                replaceMethod(instance, instance.testDirectory, 'succeeds', [new String[0]] as Object[])
                replaceMethod(instance, instance.testDirectory, 'fails', [new String[0]] as Object[])
                replaceMethod(instance.executer, instance.testDirectory, 'run', new Object[0])
            }

            def replaceMethod(Object instance, File projectDirectory, String methodName, Object[] methodArguments) {
                def originalMethod = instance.metaClass.getMetaMethod(methodName, methodArguments)
                def hookMethod = { String[] args ->
                    JUnitPlatformTestRewriter.rewriteDirectory(projectDirectory)
                    originalMethod.invoke(delegate, [args] as Object[])
                }
                instance.metaClass[methodName] = hookMethod
            }
        })
        feature.setReportIterations(true)
    }

    DataProviderInfo createJUnitPlatformDataProvider() {
        DataProviderInfo dataProviderInfo = new DataProviderInfo()
        dataProviderInfo.setDataProviderMethod(new MethodInfo() {
            @Override
            Object invoke(Object target, Object... arguments) throws Throwable {
                return [JUNIT4, JUNIT_PLATFORM]
            }

            @Override
            boolean hasBytecodeName(String name) {
                false
            }
        })
        return dataProviderInfo
    }
}
