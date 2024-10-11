package org.codinjutsu.tools.jenkins.view.parameter.renderer;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.codinjutsu.tools.jenkins.constant.BuildConst;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.util.StringUtil;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.codinjutsu.tools.jenkins.constant.BuildConst.*;

public class ExtendedChoiceParameterRenderer extends AbstractParameterRenderer implements JobParameterRenderer {

    @NonNls
    private static final String TYPE_CLASS = "com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition";

    public static final JobParameterType PT_SINGLE_SELECT = new JobParameterType("PT_SINGLE_SELECT", TYPE_CLASS);

    public static final JobParameterType PT_MULTI_SELECT = new JobParameterType("PT_MULTI_SELECT", TYPE_CLASS);

    public static final JobParameterType PT_CHECKBOX = new JobParameterType("PT_CHECKBOX", TYPE_CLASS);

    public static final JobParameterType PT_RADIO = new JobParameterType("PT_RADIO", TYPE_CLASS);

    public static final JobParameterType PT_TEXTBOX = new JobParameterType("PT_TEXTBOX", TYPE_CLASS);

    public static final JobParameterType PT_HIDDEN = new JobParameterType("PT_HIDDEN", TYPE_CLASS);

    private final Map<JobParameterType, BiFunction<JobParameter, String, JobParameterComponent<String>>> converter
            = new HashMap<>();

    public ExtendedChoiceParameterRenderer() {
        converter.put(PT_SINGLE_SELECT, JobParameterRenderers::createComboBoxIfChoicesExists);
        converter.put(PT_MULTI_SELECT, JobParameterRenderers::createComboBoxIfChoicesExists);
        converter.put(PT_CHECKBOX, JobParameterRenderers::createCheckBoxList);
        converter.put(PT_RADIO, JobParameterRenderers::createTextField);
        converter.put(PT_TEXTBOX, JobParameterRenderers::createTextField);
        converter.put(PT_HIDDEN, (jobParameter, name) -> new JobParameterComponent<>(jobParameter, new JLabel(), false));
    }

    @Override
    protected JobParameterComponent getJobParameterComponent(JobParameter jobParameter,
                                                             ProjectJob projectJob, String defaultValue) {
        BiFunction<JobParameter, String, JobParameterComponent<String>> biFunction =
                converter.get(jobParameter.getJobParameterType());
        if (biFunction == null) {
            biFunction = JobParameterRenderers::createTextField;
        }
        //checkbox选项的默认值填充
        if (BuildConst.isEnvProp(jobParameter.getName())) {
            if (CollectionUtils.isEmpty(jobParameter.getChoices())) {
                jobParameter.setChoices(ENV_LIST);
            }
            defaultValue = StringUtil.defaultIfBlank(defaultValue, DEFAULT_ENV);
        } else if (SKIP_TEST.equals(jobParameter.getName())) {
            if (CollectionUtils.isEmpty(jobParameter.getChoices())) {
                jobParameter.setChoices(Lists.newArrayList(SKIP_TEST_VALUE));
            }
            defaultValue = StringUtil.defaultIfBlank(defaultValue, SKIP_TEST_VALUE);
        }
        return biFunction.apply(jobParameter, defaultValue);
    }

    @Override
    public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
        return converter.containsKey(jobParameter.getJobParameterType());
    }
}
