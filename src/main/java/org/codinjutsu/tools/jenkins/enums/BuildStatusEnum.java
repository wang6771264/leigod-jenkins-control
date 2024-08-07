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

package org.codinjutsu.tools.jenkins.enums;

import com.intellij.openapi.diagnostic.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Currently missing color: nobuilt
 *
 * @see <a href="https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/model/BallColor.java">Jenkins Color</a>
 * @see <a href="https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/model/StatusIcon.java>Jenkins Status</a>
 */
@Getter
@RequiredArgsConstructor
public enum BuildStatusEnum {

    FAILURE("Failure", ColorEnum.RED),
    UNSTABLE("Unstable", ColorEnum.YELLOW),
    ABORTED("Aborted", ColorEnum.ABORTED),
    SUCCESS("Success", ColorEnum.BLUE),
    STABLE("Stable", ColorEnum.BLUE),
    NULL("Null"),
    // TODO: handle the folder-case explicitly. @mcmics: use better Folder Detection
    // instead of simply making it a BuildStatusEnum so that the icon renders
    FOLDER("Folder"),
    RUNNING("Running", ColorEnum.GRAY);


    private static final Logger log = Logger.getInstance(BuildStatusEnum.class);

    private final String status;
    private final ColorEnum colorEnum;

    private static final Map<String, BuildStatusEnum> MAPPING = new HashMap<>();

    static {
        for (BuildStatusEnum value : values()) {
            MAPPING.put(value.getStatus().toUpperCase(), value);
        }
    }

    BuildStatusEnum(String status) {
        this(status, ColorEnum.DISABLED);
    }

    public static BuildStatusEnum parseStatus(String status) {
        if (status == null) {
            return NULL;
        }
        return MAPPING.get(status.toUpperCase());
    }

    /**
     * Parse status from color
     */
    public static BuildStatusEnum getStatusByColor(String jobColor) {
        if (null == jobColor) {
            return NULL;
        }
        BuildStatusEnum[] jobStates = values();
        for (BuildStatusEnum jobStatus : jobStates) {
            if (jobStatus.getColorEnum().isForJobColor(jobColor)) {
                return jobStatus;
            }
        }
        return NULL;
    }

    public boolean equalsByStatus(String status){
        return status != null && StringUtils.equalsIgnoreCase(status, this.status);
    }
}
