/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.tooling.internal.consumer.connection;

import org.gradle.tooling.BuildController;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.internal.adapter.ObjectGraphAdapter;
import org.gradle.tooling.internal.adapter.ProtocolToModelAdapter;
import org.gradle.tooling.internal.adapter.ViewBuilder;
import org.gradle.tooling.internal.connection.DefaultProjectIdentifier;
import org.gradle.tooling.internal.consumer.converters.BasicGradleProjectIdentifierMixin;
import org.gradle.tooling.internal.consumer.converters.FixedProjectIdentifierProvider;
import org.gradle.tooling.internal.consumer.converters.GradleProjectIdentifierMixin;
import org.gradle.tooling.internal.consumer.versioning.ModelMapping;
import org.gradle.tooling.internal.protocol.BuildResult;
import org.gradle.tooling.internal.protocol.InternalBuildController;
import org.gradle.tooling.internal.protocol.InternalUnsupportedModelException;
import org.gradle.tooling.internal.protocol.ModelIdentifier;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.HasGradleProject;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.internal.Exceptions;

import java.io.File;

class BuildControllerAdapter implements BuildController {
    private final InternalBuildController buildController;
    private final ProtocolToModelAdapter adapter;
    private final ObjectGraphAdapter resultAdapter;
    private final ModelMapping modelMapping;
    private final File rootDir;

    public BuildControllerAdapter(ProtocolToModelAdapter adapter, InternalBuildController buildController, ModelMapping modelMapping, File rootDir) {
        this.adapter = adapter;
        this.buildController = buildController;
        this.modelMapping = modelMapping;
        this.rootDir = rootDir;
        // Treat all models returned to the action as part of the same object graph
        resultAdapter = adapter.newGraph();
    }

    public <T> T getModel(Class<T> modelType) throws UnknownModelException {
        return getModel(null, modelType);
    }

    public <T> T findModel(Class<T> modelType) {
        try {
            return getModel(modelType);
        } catch (UnknownModelException e) {
            // Ignore
            return null;
        }
    }

    public GradleBuild getBuildModel() {
        return getModel(null, GradleBuild.class);
    }

    public <T> T findModel(Model target, Class<T> modelType) {
        try {
            return getModel(target, modelType);
        } catch (UnknownModelException e) {
            // Ignore
            return null;
        }
    }

    public <T> T getModel(Model target, Class<T> modelType) throws UnknownModelException {
        ModelIdentifier modelIdentifier = modelMapping.getModelIdentifierFromModelType(modelType);
        Object originalTarget = target == null ? null : adapter.unpack(target);

        BuildResult<?> result;
        try {
            result = buildController.getModel(originalTarget, modelIdentifier);
        } catch (InternalUnsupportedModelException e) {
            //TODO we should be mapping exceptions here just like we do for model requests
            throw Exceptions.unknownModel(modelType, e);
        }

        if (rootDir != null) {
            //TODO shouldn't we be applying HasCompatibilityMapping#applyCompatibilityMapping here?
            FixedProjectIdentifierProvider identifierProvider = new FixedProjectIdentifierProvider(new DefaultProjectIdentifier(rootDir, getProjectPath(target)));
            ViewBuilder<T> viewBuilder = resultAdapter.builder(modelType);
            viewBuilder.mixInTo(GradleProject.class, new GradleProjectIdentifierMixin(identifierProvider.getBuildIdentifier()));
            viewBuilder.mixInTo(BasicGradleProject.class, new BasicGradleProjectIdentifierMixin(identifierProvider.getBuildIdentifier()));
            return identifierProvider.applyTo(viewBuilder).build(result.getModel());
        }
        return resultAdapter.adapt(modelType, result.getModel());
    }

    private String getProjectPath(Model target) {
        if (target instanceof GradleProject) {
            return ((GradleProject) target).getPath();
         }
        if (target instanceof BasicGradleProject) {
            return ((BasicGradleProject) target).getPath();
        }
        if (target instanceof HasGradleProject) {
            return ((HasGradleProject) target).getGradleProject().getPath();
        }
        return ":";
    }
}
