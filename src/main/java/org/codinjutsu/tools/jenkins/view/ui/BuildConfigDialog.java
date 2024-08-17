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

package org.codinjutsu.tools.jenkins.view.ui;

import com.alibaba.fastjson.JSON;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckBoxList;
import org.codinjutsu.tools.jenkins.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.JenkinsSettings;
import org.codinjutsu.tools.jenkins.cache.JobCache;
import org.codinjutsu.tools.jenkins.logic.RequestManagerInterface;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.codinjutsu.tools.jenkins.view.util.SpringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class BuildConfigDialog extends DialogWrapper {
    private static final String LAST_SIZE = "jenkins.build.config";
    private static final Logger logger = Logger.getInstance(BuildConfigDialog.class);
    private final Job job;
    private final @NotNull Project project;
    private final JenkinsAppSettings configuration;
    private final RequestManagerInterface requestManager;
    private final Collection<JobParameterComponent<?>> inputFields = new LinkedHashSet<>();
    private JPanel contentPane;
    private JPanel contentPanel;

    BuildConfigDialog(@NotNull Project project, Job job, JenkinsAppSettings configuration,
                      RequestManagerInterface requestManager) {
        super(project);
        this.project = project;
        init();
        setTitle("This build config parameters");
        this.job = job;
        this.configuration = configuration;
        this.requestManager = requestManager;

        contentPanel.setName("contentPanel");
        addParameterInputs();
        setModal(true);
        setAutoAdjustable(false);
    }

    public static void showDialog(@NotNull Project project, final Job job, final JenkinsAppSettings appSetting,
                                  final JenkinsSettings settings, final RequestManagerInterface requestManager) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final BuildConfigDialog dialog = new BuildConfigDialog(project, job, appSetting, requestManager);
            dialog.pack();
            dialog.restoreLastWidth();
            if (dialog.showAndGet()) {
                dialog.onOK();
            }
            dialog.saveLastSize();
        }, ModalityState.nonModal());
    }

    private void restoreLastWidth() {
        final var height = getWindow().getHeight();
        final var storedSize = DimensionService.getInstance().getSize(LAST_SIZE, project);
        if (storedSize != null) {
            final var preferredSize = new Dimension();
            preferredSize.setSize(storedSize.getWidth(), height);
            getWindow().setPreferredSize(preferredSize);
            pack();
        }
    }

    @NotNull
    private static JLabel appendColonIfMissing(@NotNull JLabel label) {
        if (!label.getText().endsWith(":")) {
            label.setText(label.getText() + ":");
        }
        return label;
    }

    @NotNull
    private static JLabel setJLabelStyles(@NotNull JLabel label) {
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        return label;
    }

    @NotNull
    private static Function<JLabel, JLabel> setJLabelStyles(@NotNull JobParameterComponent<?> jobParameterComponent) {
        return label -> {
            label.setLabelFor(jobParameterComponent.getViewElement());
            setJLabelStyles(label);
            return label;
        };
    }

    private void addParameterInputs() {
        contentPanel.setLayout(new SpringLayout());
        List<JobParameter> parameters = job.getParameters();
        System.out.println(JSON.toJSONString(parameters));
        final AtomicInteger rows = new AtomicInteger(0);
        for (JobParameter jobParameter : parameters) {
            //跳过测试是必然的
            final JobParameterRenderer jobParameterRenderer = JobParameterRenderer.findRenderer(jobParameter)
                    .orElseGet(DefaultRenderer::new);
            final ProjectJob projectJob = ProjectJob.builder()
                    .job(job)
                    .project(project).lastBuild(job.getLastBuild()).build();
            final JobParameterComponent<?> jobParameterComponent = jobParameterRenderer.render(jobParameter, projectJob);

            if (jobParameterComponent.isVisible()) {
                rows.incrementAndGet();
                jobParameterComponent.getViewElement().setName(jobParameter.getName());

                final JLabel label = jobParameterRenderer.createLabel(jobParameter)
                        .map(setJLabelStyles(jobParameterComponent))
                        .map(BuildConfigDialog::appendColonIfMissing)
                        .orElseGet(JLabel::new);
                contentPanel.add(label);
                contentPanel.add(jobParameterComponent.getViewElement());

                final String description = jobParameter.getDescription();
                if (StringUtil.isNotEmpty(description)) {
                    JLabel placeHolder = new JLabel("", SwingConstants.CENTER);
                    contentPanel.add(placeHolder);
                    contentPanel.add(new JLabel(description));
                    rows.incrementAndGet();
                }

                inputFields.add(jobParameterComponent);
            }
        }

        final int columns = 2;
        final int initial = 6;
        final int padding = 6;
        SpringUtilities.makeCompactGrid(contentPanel,
                rows.get(), columns,
                initial, initial,
                padding, padding);

        getOKAction().setEnabled(!hasError());
    }

    private boolean hasError() {
        return inputFields.stream().anyMatch(JobParameterComponent::hasError);
    }

    private void onOK() {
        //TODO 改为保存预置的构建配置
    }

    private void saveLastSize() {
        DimensionService.getInstance().setSize(LAST_SIZE, getSize(), project);
    }

    @NotNull
    private Map<String, ?> getParamValueMap() {
        final Map<String, Object> valueByNameMap = new HashMap<>();
        for (JobParameterComponent<?> jobParameterComponent : inputFields) {
            final JobParameter jobParameter = jobParameterComponent.getJobParameter();
            jobParameterComponent.ifHasValue(value -> valueByNameMap.put(jobParameter.getName(), value));
        }
        return valueByNameMap;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public static class DefaultRenderer implements JobParameterRenderer {

        @NotNull
        @Override
        public JobParameterComponent<String> render(@NotNull JobParameter jobParameter, @Nullable ProjectJob projectJob) {
            return JobParameterRenderers.createTextField(jobParameter, jobParameter.getDefaultValue());
        }

        @Override
        public boolean isForJobParameter(@NotNull JobParameter jobParameter) {
            return true;
        }
    }

}
