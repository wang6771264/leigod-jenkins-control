package org.codinjutsu.tools.jenkins.enums;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public enum ColorEnum {

    RED,
    YELLOW,
    ABORTED,
    BLUE,
    DISABLED,
    GRAY;

    public boolean isForJobColor(@NotNull String jobColor) {
        return jobColor.toLowerCase(Locale.ENGLISH).startsWith(getJobColorName());
    }

    @NotNull
    public String getJobColorName() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}
