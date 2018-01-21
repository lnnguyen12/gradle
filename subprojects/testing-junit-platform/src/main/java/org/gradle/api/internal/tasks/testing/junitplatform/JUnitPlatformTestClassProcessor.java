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

package org.gradle.api.internal.tasks.testing.junitplatform;

import org.gradle.api.Action;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.internal.tasks.testing.junit.JUnitTestClassProcessor;
import org.gradle.api.internal.tasks.testing.junit.TestClassExecutionListener;
import org.gradle.internal.actor.ActorFactory;
import org.gradle.internal.id.IdGenerator;
import org.gradle.internal.time.Clock;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class JUnitPlatformTestClassProcessor extends JUnitTestClassProcessor {
    public JUnitPlatformTestClassProcessor(IdGenerator<?> idGenerator, ActorFactory actorFactory, Clock clock) {
        super(null, idGenerator, actorFactory, clock);
    }

    @Override
    protected Action<String> createTestExecutor(TestResultProcessor threadSafeResultProcessor, TestClassExecutionListener threadSafeTestClassListener) {
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(new JUnitPlatformTestExecutionListener(threadSafeResultProcessor, clock, idGenerator));
        return testClassName -> {
            threadSafeTestClassListener.testClassStarted(testClassName);
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClassName))
                .build();

            Throwable e = null;
            try {
                launcher.execute(request);
            } catch (Throwable throwable) {
                e = throwable;
            }
            threadSafeTestClassListener.testClassFinished(e);
        };
    }
}
