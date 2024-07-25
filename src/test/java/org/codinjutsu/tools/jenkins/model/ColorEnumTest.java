package org.codinjutsu.tools.jenkins.model;

import org.codinjutsu.tools.jenkins.enums.ColorEnum;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ColorEnumTest {

    @Test
    public void isForJobColor() {
        assertThat(ColorEnum.ABORTED.isForJobColor("Aborted")).isTrue();
        assertThat(ColorEnum.ABORTED.isForJobColor("aborted")).isTrue();
        assertThat(ColorEnum.ABORTED.isForJobColor("ABORTED")).isTrue();
        assertThat(ColorEnum.BLUE.isForJobColor("Blue")).isTrue();
        assertThat(ColorEnum.DISABLED.isForJobColor("Disabled")).isTrue();
        assertThat(ColorEnum.RED.isForJobColor("Red")).isTrue();
        assertThat(ColorEnum.YELLOW.isForJobColor("Yellow")).isTrue();
        assertThat(ColorEnum.YELLOW.isForJobColor("RedYellow")).isFalse();
    }

    @Test
    public void getJobColorName() {
        assertThat(ColorEnum.ABORTED.getJobColorName()).isEqualTo("aborted");
        assertThat(ColorEnum.BLUE.getJobColorName()).isEqualTo("blue");
        assertThat(ColorEnum.DISABLED.getJobColorName()).isEqualTo("disabled");
        assertThat(ColorEnum.RED.getJobColorName()).isEqualTo("red");
        assertThat(ColorEnum.YELLOW.getJobColorName()).isEqualTo("yellow");
    }
}
