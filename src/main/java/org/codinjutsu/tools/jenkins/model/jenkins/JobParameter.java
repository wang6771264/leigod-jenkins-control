/*
 * Copyright (c) 2013 David Boissier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codinjutsu.tools.jenkins.model.jenkins;

import com.github.cliftonlabs.json_simple.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.codinjutsu.tools.jenkins.example.CascadeRadioComponent;
import org.codinjutsu.tools.jenkins.example.CascadeSelectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
@Builder(toBuilder = true)
public class JobParameter {

    /**
     * 唯一标识
     */
    private final String uniqueId;

    @NotNull
    private final String name;
    @Nullable
    private final String description;
    @Nullable
    private final JobParameterType jobParameterType;
    /**
     * 默认值的json格式
     */
    @Nullable
    private final JsonObject defaultParamObj;

    @Nullable
    private String defaultValue;
    @NotNull
    @Singular
    private List<String> choices;
    /**
     * 如果已有下拉列表则直接用已经有的
     */
    private CascadeSelectComponent cascadeComboBox;
}
