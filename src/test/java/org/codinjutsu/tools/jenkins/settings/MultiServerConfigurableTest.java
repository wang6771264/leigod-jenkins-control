package org.codinjutsu.tools.jenkins.settings;

import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;

public class MultiServerConfigurableTest {

    private static final @NotNull Disposable DO_NOTHING = () -> {
    };

    private final MockProject project = new MockProject(null, DO_NOTHING);

    @Before
    public void setUp() {

    }

}
