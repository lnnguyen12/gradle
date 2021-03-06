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

package org.gradle.vcs.internal

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.resolve.ResolveTestFixture
import org.gradle.test.fixtures.file.TestFile
import org.gradle.vcs.fixtures.GitRepository
import org.junit.Rule
import spock.lang.Unroll

class GitVersionSelectionIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    GitRepository repo = new GitRepository('dep', temporaryFolder.getTestDirectory())
    TestFile repoSettingsFile
    def fixture = new ResolveTestFixture(buildFile)

    def setup() {
        settingsFile << """
            rootProject.name = 'consumer'
            gradle.rootProject {
                configurations {
                    compile
                }
                group = 'test'
                version = '1.2'
            }
            sourceControl.vcsMappings.withModule("test:test") {
                from(GitVersionControlSpec) {
                    url = uri('${repo.url}')
                }
            }
        """

        repoSettingsFile = repo.file("settings.gradle")
        repoSettingsFile << '''
            rootProject.name = 'test'
            gradle.rootProject {
                configurations.create('default')
                group = 'test'
                version = '1.0'
                def jar = tasks.create("jar_$version", Jar) {
                    baseName = "test"
                    version = project.version
                }
                configurations['default'].outgoing.artifact(jar)
            }
        '''
        fixture.prepare()
    }

    def "selects and builds from HEAD for latest.integration selector"() {
        given:
        buildFile << """
            dependencies { compile 'test:test:latest.integration' }
        """
        repo.commit("v1")
        repoSettingsFile.replace("version = '1.0'", "version = '2.0'")
        repo.commit("v2")

        when:
        run('checkDeps')

        then:
        fixture.expectGraph {
            root(":", "test:consumer:1.2") {
                edge("test:test:latest.integration", "project :test", "test:test:2.0") {
                }
            }
        }
        result.assertTasksExecuted(":test:jar_2.0", ":checkDeps")

        when:
        repoSettingsFile.replace("version = '2.0'", "version = '3.0'")
        repo.commit("v3")
        run('checkDeps')

        then:
        fixture.expectGraph {
            root(":", "test:consumer:1.2") {
                edge("test:test:latest.integration", "project :test", "test:test:3.0") {
                }
            }
        }
        result.assertTasksExecuted(":test:jar_3.0", ":checkDeps")

        when:
        repo.createBranch("ignore")
        repo.checkout("ignore")
        repoSettingsFile.replace("version = '3.0'", "version = 'ignore'")
        repo.commit("v4")
        repo.checkout("master")

        run('checkDeps')

        then:
        fixture.expectGraph {
            root(":", "test:consumer:1.2") {
                edge("test:test:latest.integration", "project :test", "test:test:3.0") {
                }
            }
        }
        result.assertTasksExecuted(":test:jar_3.0", ":checkDeps")
    }

    def "selects and builds from tag for static selector"() {
        given:
        buildFile << """
            dependencies { compile 'test:test:2.0' }
        """
        repo.commit("v1")
        repo.createLightWeightTag("1.0")
        repoSettingsFile.replace("version = '1.0'", "version = '2.0'")
        repo.commit("v2")
        repo.createLightWeightTag("2.0")
        repoSettingsFile.replace("version = '2.0'", "version = '3.0'")
        repo.commit("v3")
        repo.createLightWeightTag("3.0")

        when:
        run('checkDeps')

        then:
        fixture.expectGraph {
            root(":", "test:consumer:1.2") {
                edge("test:test:2.0", "project :test", "test:test:2.0") {
                }
            }
        }
        result.assertTasksExecuted(":test:jar_2.0", ":checkDeps")
    }

    @Unroll
    def "selects and builds from highest tag that matches #selector selector"() {
        given:
        buildFile << """
            dependencies { compile 'test:test:${selector}' }
        """
        repo.commit("v1")
        repo.createLightWeightTag("1.0")
        repoSettingsFile.replace("version = '1.0'", "version = '1.1'")
        repo.commit("v2")
        repo.createLightWeightTag("1.1")
        repoSettingsFile.replace("version = '1.1'", "version = '2.0'")
        repo.commit("v3")
        repo.createLightWeightTag("2.0")

        when:
        run('checkDeps')

        then:
        fixture.expectGraph {
            root(":", "test:consumer:1.2") {
                edge("test:test:${selector}", "project :test", "test:test:1.1") {
                }
            }
        }
        result.assertTasksExecuted(":test:jar_1.1", ":checkDeps")

        when:
        repoSettingsFile.replace("version = '2.0'", "version = '1.2'")
        repo.commit("v4")
        repo.createLightWeightTag("1.2")
        run('checkDeps')

        then:
        fixture.expectGraph {
            root(":", "test:consumer:1.2") {
                edge("test:test:${selector}", "project :test", "test:test:1.2") {
                }
            }
        }
        result.assertTasksExecuted(":test:jar_1.2", ":checkDeps")

        where:
        selector    | _
        "1.+"       | _
        "[1.0,1.9]" | _
    }

    @Unroll
    def "static selector cannot reference branch #selector"() {
        given:
        buildFile << """
            dependencies { compile 'test:test:${selector}' }
        """
        repo.commit("v1")
        repo.createBranch("release")

        when:
        fails('checkDeps')

        then:
        failure.assertHasCause("Could not resolve test:test:${selector}.")

        where:
        selector  | _
        "master"  | _
        "release" | _
        "HEAD"    | _
    }
}
