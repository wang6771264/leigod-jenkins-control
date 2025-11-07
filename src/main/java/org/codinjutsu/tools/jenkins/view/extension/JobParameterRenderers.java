package org.codinjutsu.tools.jenkins.view.extension;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.popup.list.ListPopupImpl;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.jenkins.constant.BuildConst;
import org.codinjutsu.tools.jenkins.logic.RequestManager;
import org.codinjutsu.tools.jenkins.logic.RequestManagerInterface;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameter;
import org.codinjutsu.tools.jenkins.model.jenkins.JobParameterType;
import org.codinjutsu.tools.jenkins.model.jenkins.ProjectJob;
import org.codinjutsu.tools.jenkins.util.SymbolPool;
import org.codinjutsu.tools.jenkins.view.parameter.JobParameterComponent;
import org.codinjutsu.tools.jenkins.view.parameter.PasswordComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public final class JobParameterRenderers {

    public static final Icon ERROR_ICON = AllIcons.General.BalloonError;
    public static final String MISSING_NAME_LABEL = "<Missing Name>";

    @NotNull
    public static JobParameterComponent<VirtualFile> createFileUpload(JobParameter jobParameter, String defaultValue) {
        final TextFieldWithBrowseButton textFieldWithBrowseButton = new TextFieldWithBrowseButton();
        final Project project = null;
        textFieldWithBrowseButton.addBrowseFolderListener(jobParameter.getName(), jobParameter.getDescription(), project,
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
        textFieldWithBrowseButton.setTextFieldPreferredWidth(30);
        if (StringUtil.isNotEmpty(defaultValue)) {
            textFieldWithBrowseButton.setText(defaultValue);
        }
        return new JobParameterComponent<>(jobParameter, textFieldWithBrowseButton, JobParameterRenderers.getFile());
    }

    @NotNull
    public static JobParameterComponent<String> createPasswordField(JobParameter jobParameter, String defaultValue) {
        final PasswordComponent passwordComponent = PasswordComponent.create();
        passwordComponent.init();
        if (StringUtil.isNotEmpty(defaultValue)) {
            passwordComponent.setValue(defaultValue);
        }
        return new JobParameterComponent<>(jobParameter, passwordComponent.asComponent(), c -> passwordComponent.getValue());
    }

    @NotNull
    public static JobParameterComponent<String> createTextArea(JobParameter jobParameter, String defaultValue) {
        final JTextArea textArea = new JBTextArea();
        textArea.setRows(5);
        if (StringUtil.isNotEmpty(defaultValue)) {
            textArea.setText(defaultValue);
        }
        return new JobParameterComponent<>(jobParameter, textArea, JTextComponent::getText);
    }

    @NotNull
    public static JobParameterComponent<String> createTextField(JobParameter jobParameter, String defaultValue) {
        final JTextField textField = new JBTextField();
        if (StringUtil.isNotEmpty(defaultValue)) {
            textField.setText(defaultValue);
        }
        return new JobParameterComponent<>(jobParameter, textField, JTextComponent::getText);
    }

    @NotNull
    public static JobParameterComponent<String> createCheckBox(JobParameter jobParameter, String defaultValue) {
        final JCheckBox checkBox = new JCheckBox();
        if (Boolean.TRUE.equals(Boolean.valueOf(defaultValue))) {
            checkBox.setSelected(true);
        }
        return new JobParameterComponent<>(jobParameter, checkBox, asString(JCheckBox::isSelected));
    }

    @NotNull
    public static JobParameterComponent<String> createComboBox(@NotNull JobParameter jobParameter, String defaultValue) {
        final List<String> choices = jobParameter.getChoices();

        JBTextField textField = new JBTextField(defaultValue);
        textField.setEditable(false);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(textField, choices, "");
            }
        });
        return new JobParameterComponent<>(jobParameter, textField, component -> textField.getText());
    }

    private void showPopup(JBTextField textField, List<String> choices, String prefix) {
        BaseListPopupStep<String> step = new BaseListPopupStep<>(null, choices) {
            @Override
            public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
                return doFinalStep(() -> {
                    //填充选择值
                    textField.setText(selectedValue);
                });
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }

            @Override
            public boolean hasSubstep(String selectedValue) {
                return false;
            }
        };
        ListPopup currentPopup = JBPopupFactory.getInstance().createListPopup(step);
        ((ListPopupImpl) currentPopup).getSpeedSearch().updatePattern(prefix);
        // 在文本框下方显示弹出列表
        currentPopup.showUnderneathOf(textField);
    }

    private static JCheckBox[] convertJCheckBoxList(List<String> choices) {
        return convertJCheckBoxList(choices, null);
    }

    private static JCheckBox[] convertJCheckBoxList(List<String> choices, List<String> selectedChoices) {
        JCheckBox[] checkBoxes = new JCheckBox[choices.size()];
        int index = 0;
        boolean hasSeleted = selectedChoices != null;
        for (String choice : choices) {
            JCheckBox checkBox = new JCheckBox(choice,
                    hasSeleted && selectedChoices.contains(choice));
            checkBoxes[index++] = checkBox;
        }
        return checkBoxes;
    }

    private static final String K8S_ENV_EQUAL_TRUE = "=true";

    private List<String> getCheckBoxText(JCheckBox[] choices, CheckBoxList<String> list) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < choices.length; i++) {
            JCheckBox choice = choices[i];
            if (list.getModel().getElementAt(i).isSelected()) {
                res.add(choice.getText());
            }
        }
        return res;
    }

    /**
     * fixme 该方法不通用,只能用于k8s环境变量
     *
     * @param jobParameter
     * @param defaultValue
     * @return
     */
    @NotNull
    public static JobParameterComponent<String> createCheckBoxList(@NotNull JobParameter jobParameter,
                                                                   String defaultValue) {
        JCheckBox[] choices = new JCheckBox[]{};
        if (CollectionUtils.isNotEmpty(jobParameter.getChoices())) {
            //添加choices，包含默认值
            choices = convertJCheckBoxList(jobParameter.getChoices(),
                    Arrays.stream(defaultValue.split(",")).toList());
        }
        CheckBoxList<String> list = new CheckBoxList<>();
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        DefaultListModel<JCheckBox> models = new DefaultListModel<>();
        for (JCheckBox choice : choices) {
            models.addElement(choice);
        }
        list.setModel(models);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(1); // 设置为1行可见
        JCheckBox[] finalChoices = choices;
        return new JobParameterComponent<>(jobParameter, list, asString(o -> {
            List<String> checkBoxText = getCheckBoxText(finalChoices, o);
            if (BuildConst.isEnvProp(jobParameter.getName())) {
                return checkBoxText.stream().map(select -> select + K8S_ENV_EQUAL_TRUE)
                        .collect(Collectors.joining(SymbolPool.AMPERSAND));
            } else {
                return String.join(",", checkBoxText);
            }
        }));
    }

    @SuppressWarnings("unused")
    @NotNull
    public static JobParameterComponent<String> createLabel(@NotNull JobParameter jobParameter, String defaultValue) {
        final JBLabel label = new JBLabel();
        if (StringUtil.isNotEmpty(defaultValue)) {
            label.setText(defaultValue);
        }
        return new JobParameterComponent<>(jobParameter, label, JLabel::getText);
    }

    @NotNull
    public static JLabel createErrorLabel(@Nullable JobParameterType jobParameterType) {
        final String text;
        if (jobParameterType == null) {
            text = "Unknown parameter type";
        } else {
            text = jobParameterType.getType() + " is unsupported.";
        }
        return createErrorLabel(text);
    }

    @NotNull
    public static JLabel createErrorLabel(@NotNull String label) {
        return new ErrorLabel(label);
    }

    @NotNull
    public static JobParameterComponent<String> createErrorLabel(@NotNull JobParameter jobParameter) {
        return new JobParameterComponent<>(jobParameter, createErrorLabel(jobParameter.getJobParameterType()), () -> true);
    }

    @SuppressWarnings("unused")
    @NotNull
    public static JobParameterComponent<String> createErrorLabel(@NotNull JobParameter jobParameter, String defaultValue) {
        return createErrorLabel(jobParameter);
    }

    @NotNull
    public static JobParameterComponent<String> createComboBoxIfChoicesExists(@NotNull JobParameter jobParameter,
                                                                              String defaultValue) {
        final BiFunction<JobParameter, String, JobParameterComponent<String>> renderer;
        if (jobParameter.getChoices().isEmpty()) {
            renderer = JobParameterRenderers::createTextField;
        } else {
            renderer = JobParameterRenderers::createComboBox;
        }
        return renderer.apply(jobParameter, defaultValue);
    }

    @NotNull
    public static JobParameterComponent<String> createCascadeComboBoxIfExists(@NotNull JobParameter jobParameter,
                                                                              String defaultValue) {
        if (jobParameter.getCascadeComboBox() == null) {
            return JobParameterRenderers.createTextField(jobParameter, defaultValue);
        } else {
            //fixme 级联获取值目前只能从toolTipText中获取,后续需要修正
            return new JobParameterComponent<>(jobParameter,
                    jobParameter.getCascadeComboBox().getComponent(),
                    asString(JComponent::getToolTipText));
        }
    }


    @NotNull
    public static Function<JobParameter, JobParameterComponent<String>> createGitParameterChoices(
            @NotNull ProjectJob projectJob) {
        return jobParameter -> createGitParameterChoices(projectJob, jobParameter, jobParameter.getDefaultValue());
    }

    /**
     * 可以显示的分支
     */
    private final List<String> FILTER_BRANCH_INCLUDE =
            Lists.newArrayList("master", "develop", "test1", "test", "test2");
    /**
     * 默认的分支
     */
    private final String DEFAULT_BRANCH_INCLUDE = "test1";

    private boolean isBranchInclude(String branch) {
        for (String filterBranch : FILTER_BRANCH_INCLUDE) {
            if (branch.endsWith(filterBranch)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static JobParameterComponent<String> createGitParameterChoices(@NotNull ProjectJob projectJob,
                                                                          @NotNull JobParameter jobParameter,
                                                                          String defaultValue) {
        if (jobParameter.getChoices().isEmpty()) {
            final RequestManagerInterface requestManager = RequestManager.getInstance(projectJob.getProject());
            String defaultSelect = "origin/master";
            List<String> choices = requestManager.getGitParameterChoices(projectJob.getJob(), jobParameter);
            //前置选择
            List<String> preChoices = new ArrayList<>();
            choices.removeIf(branch -> {
                if (isBranchInclude(branch)) {
                    preChoices.add(branch);
                    return true;
                }
                return false;
            });
            choices.addAll(0, preChoices);
            for (String branch : choices) {
                if (StringUtils.endsWith(branch, DEFAULT_BRANCH_INCLUDE)) {
                    defaultSelect = branch;
                    break;
                }
            }
            JobParameter gitParameter = JobParameter.builder()
                    .name(jobParameter.getName())
                    .description(jobParameter.getDescription())
                    .jobParameterType(jobParameter.getJobParameterType())
                    .defaultValue(defaultSelect)
                    .choices(choices)
                    .build();
			return createComboBoxIfChoicesExists(gitParameter, defaultSelect);
        } else {
            return createComboBoxIfChoicesExists(jobParameter, defaultValue);
        }
    }

    @NotNull
    private static <T> Function<T, String> asString(Function<T, Object> provider) {
        return c -> String.valueOf(provider.apply(c));
    }

    @NotNull
    public static Function<TextFieldWithBrowseButton, VirtualFile> getFile() {
        return JobParameterRenderers::getFile;
    }

    @Nullable
    private static VirtualFile getFile(@NotNull TextFieldWithBrowseButton filePathField) {
        if (!StringUtil.isEmpty(filePathField.getText())) {
            // use com.intellij.openapi.vfs.VirtualFileLookup (2020.2 and later)
            final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePathField.getText());
            if (file != null && !file.isDirectory()) {
                return file;
            }
        }
        return null;
    }

    public static class ErrorLabel extends JLabel {

        public ErrorLabel(@Nullable String text) {
            setText(text);
            setIcon(ERROR_ICON);
        }
    }
}
