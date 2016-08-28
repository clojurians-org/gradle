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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.result;

import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.internal.component.local.model.CurrentBuildIdentifier;
import org.gradle.internal.component.local.model.DefaultBuildIdentifier;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;

import java.io.File;
import java.io.IOException;

public class BuildIdentifierSerializer implements Serializer<BuildIdentifier> {
    @Override
    public BuildIdentifier read(Decoder decoder) throws IOException {
        String buildName = decoder.readNullableString();
        if (buildName == null) {
            return new CurrentBuildIdentifier();
        }
        return new DefaultBuildIdentifier(buildName, new File(decoder.readString()));
    }

    @Override
    public void write(Encoder encoder, BuildIdentifier value) throws IOException {
        if (value.isCurrentBuild()) {
            encoder.writeNullableString(null);
        } else {
            encoder.writeNullableString(value.getName());
            encoder.writeString(((DefaultBuildIdentifier) value).getRootDir().getAbsolutePath());
        }
    }
}