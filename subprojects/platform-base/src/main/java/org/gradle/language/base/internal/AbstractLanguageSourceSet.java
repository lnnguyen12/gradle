/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.language.base.internal;

import org.gradle.api.BuildableComponentSpec;
import org.gradle.api.Task;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.AbstractBuildableComponentSpec;
import org.gradle.platform.base.internal.ComponentSpecIdentifier;

public abstract class AbstractLanguageSourceSet extends AbstractBuildableComponentSpec implements LanguageSourceSetInternal {
    private final String languageName;
    private final SourceDirectorySet source;
    private boolean generated;
    private Task generatorTask;

    public AbstractLanguageSourceSet(ComponentSpecIdentifier identifier, Class<? extends BuildableComponentSpec> publicType, SourceDirectorySet source) {
        super(identifier, publicType);
        this.source = source;
        this.languageName = guessLanguageName(getTypeName());
        super.builtBy(source.getBuildDependencies());
    }

    protected String getLanguageName() {
        return languageName;
    }

    private String guessLanguageName(String typeName) {
        return typeName.replaceAll("LanguageSourceSet$", "").replaceAll("SourceSet$", "").replaceAll("Source$", "").replaceAll("Set$", "");
    }

    @Override
    public String getProjectScopedName() {
        return getIdentifier().getProjectScopedName();
    }

    @Override
    public void builtBy(Object... tasks) {
        generated = true;
        super.builtBy(tasks);
    }

    @Override
    public void generatedBy(Task generatorTask) {
        this.generatorTask = generatorTask;
    }

    @Override
    public Task getGeneratorTask() {
        return generatorTask;
    }

    @Override
    public boolean getMayHaveSources() {
        // This doesn't take into account build dependencies of the SourceDirectorySet.
        // Should just ditch SourceDirectorySet from here since it's not really a great model, and drags in too much baggage.
        return generated || !source.isEmpty();
    }

    @Override
    public String getDisplayName() {
        String languageName = getLanguageName();
        if (languageName.toLowerCase().endsWith("resources")) {
            return String.format("%s '%s'", languageName, getIdentifier().getPath());
        }
        return String.format("%s source '%s'", languageName, getIdentifier().getPath());
    }

    @Override
    public SourceDirectorySet getSource() {
        return source;
    }

    @Override
    public String getParentName() {
        return getIdentifier().getParent() == null ? null : getIdentifier().getParent().getName();
    }
}
