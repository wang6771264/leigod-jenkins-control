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
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.text.StringUtil;
import org.codinjutsu.tools.jenkins.persistent.JenkinsAppSettings;
import org.codinjutsu.tools.jenkins.cache.JobCache;
import org.codinjutsu.tools.jenkins.component.SelectComponent;
import org.codinjutsu.tools.jenkins.logic.RequestManagerInterface;
import org.codinjutsu.tools.jenkins.model.jenkins.Job;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.task.RunBuild;
import org.codinjutsu.tools.jenkins.task.callback.RunBuildCallback;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderer;
import org.codinjutsu.tools.jenkins.view.extension.JobParameterRenderers;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.codinjutsu.tools.jenkins.view.util.SpringUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class BuildParamDialog extends DialogWrapper {
    private static final String LAST_SIZE = "jenkins.build.parameter";
    private static final Logger logger = Logger.getInstance(BuildParamDialog.class);
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BuildParamDialog.class);
    private final Job job;
    private final @NotNull Project project;
    private final JenkinsAppSettings configuration;
    private final RequestManagerInterface requestManager;
    private final RunBuildCallback runBuildCallback;
    private final Collection<JobParameterComponent<?>> inputFields = new LinkedHashSet<>();
    private JPanel contentPane;
    private JPanel contentPanel;

    public BuildParamDialog(@NotNull Project project, Job job, JenkinsAppSettings configuration,
                            RequestManagerInterface requestManager, RunBuildCallback runBuildCallback) {
        super(project);
        this.project = project;
        init();
        setTitle("This build requires parameters");
        this.job = job;
        this.configuration = configuration;
        this.requestManager = requestManager;
        this.runBuildCallback = runBuildCallback;

        contentPanel.setName("contentPanel");

        addParameterInputs();
        setModal(true);
        setAutoAdjustable(false);
        // 添加一个组件监听器，监听窗口大小变化
//        contentPanel.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                // 当窗口大小变化时，手动调用 revalidate 和 repaint
//                contentPanel.revalidate(); // 重新验证布局
//                contentPanel.repaint(); // 重新绘制组件
//            }
//        });
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        //保存构建的参数
        for (JobParameterComponent<?> inputField : inputFields) {
            //fixme 目前只处理自定义的下拉列表
            if(!(inputField.getViewElement() instanceof SelectComponent)){
                continue;
            }
            String parameter = inputField.getJobParameter().getName();
            String value = (String) inputField.getValueProvider().apply(inputField.getViewElement());
            System.out.printf("add job parameter,job:%s,param:%s,value:%s%n", job.getName(), parameter, value);
            JobCache.addParamDefault(job.getName(), parameter, value);
        }
        //删除需要构建的标识
        JobCache.removeNeedBuild(job.getName());
    }

    public static void showDialog(@NotNull Project project, final Job job,
                                  final JenkinsAppSettings configuration,
                                  final RequestManagerInterface requestManager,
                                  final RunBuildCallback runBuildCallback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final BuildParamDialog dialog = new BuildParamDialog(project, job, configuration, requestManager, runBuildCallback);
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
        //缓存构建参数的记录
        final AtomicInteger rows = new AtomicInteger(0);
        for (JobParameter jobParameter : parameters) {
            final JobParameterRenderer jobParameterRenderer = JobParameterRenderer.findRenderer(jobParameter)
                    .orElseGet(DefaultRenderer::new);
            final ProjectJob projectJob = ProjectJob.builder()
                    .job(job).project(project).lastBuild(job.getLastBuild())
                    .build();
            final JobParameterComponent<?> jobParameterComponent = jobParameterRenderer.render(jobParameter,
                    projectJob);

            if (jobParameterComponent.isVisible()) {
                rows.incrementAndGet();
                jobParameterComponent.getViewElement().setName(jobParameter.getName());

                final JLabel label = jobParameterRenderer.createLabel(jobParameter)
                        .map(setJLabelStyles(jobParameterComponent))
                        .map(BuildParamDialog::appendColonIfMissing)
                        .orElseGet(JLabel::new);
                contentPanel.add(label);
                //fixme 这里需要注意的是如果只有一个组件的时候这样设置会导致组件显示不出来,具体原因未知
                if (parameters.size() > 1) {
                    if (jobParameterComponent.getViewElement() instanceof ComboBox<?>) {
                        //下拉列表的高度设置理想高度
                        jobParameterComponent.getViewElement().setPreferredSize(new Dimension(-1, 35));
                        jobParameterComponent.getViewElement().setMaximumSize(new Dimension(-1, 35));
                    }
                }
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
        new RunBuild(project, job, configuration, getParamValueMap(),
                requestManager, runBuildCallback).queue();
    }

    private void saveLastSize() {
        DimensionService.getInstance().setSize(LAST_SIZE, getSize(), project);
    }

    @NotNull
    private Map<String, ?> getParamValueMap() {
        final HashMap<String, Object> valueByNameMap = new HashMap<>();
        for (JobParameterComponent<?> jobParameterComponent : inputFields) {
            final JobParameter jobParameter = jobParameterComponent.getJobParameter();
            jobParameterComponent.ifHasValue(value ->{
                //添加build参数
                valueByNameMap.put(jobParameter.getName(), value);
            });
        }
        return valueByNameMap;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public class DefaultRenderer implements JobParameterRenderer {

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
