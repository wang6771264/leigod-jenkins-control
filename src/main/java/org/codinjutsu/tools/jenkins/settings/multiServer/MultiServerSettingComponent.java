package org.codinjutsu.tools.jenkins.settings.multiServer;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.codinjutsu.tools.jenkins.JenkinsControlBundle;
import org.codinjutsu.tools.jenkins.persistent.JenkinsSettings;
import org.codinjutsu.tools.jenkins.exception.AuthenticationException;
import org.codinjutsu.tools.jenkins.settings.ServerConnectionValidator;
import org.codinjutsu.tools.jenkins.util.GuiUtil;
import org.codinjutsu.tools.jenkins.util.SnapshotWatcher;
import org.codinjutsu.tools.jenkins.util.StringUtil;
import org.codinjutsu.tools.jenkins.view.action.ActionUtil;
import org.codinjutsu.tools.jenkins.view.action.ReloadConfigurationAction;
import org.codinjutsu.tools.jenkins.view.annotation.FormValidationPanel;
import org.codinjutsu.tools.jenkins.view.annotation.GuiField;
import org.codinjutsu.tools.jenkins.view.validator.PositiveIntegerValidator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.List;
import java.util.*;

import static org.codinjutsu.tools.jenkins.util.GuiUtil.simplePanel;
import static org.codinjutsu.tools.jenkins.view.validator.ValidatorTypeEnum.POSITIVE_INTEGER;

/**
 * 可以配置多个服务
 */
public class MultiServerSettingComponent implements FormValidationPanel {
    private final JPanel mainPanel;
    private final JBTable table;
    @GuiField(validators = POSITIVE_INTEGER)
    private final JBIntSpinner connectionTimeout = new JBIntSpinner(10, 5, 300);
    private final JButton testConnection = new JButton(JenkinsControlBundle.message("settings.server.test_connection"));
    private final JLabel connectionStatusLabel = new JLabel();
    private final JTextPane debugTextPane = createDebugTextPane();
    private final JPanel debugPanel = JBUI.Panels.simplePanel(debugTextPane);
    @Getter
    private boolean apiTokenModified;

    public MultiServerSettingComponent(Project project, ServerConnectionValidator serverConnectionValidator) {
        BeanDataTableModel<JenkinsServerTableItem> model = new BeanDataTableModel<>(JenkinsServerTableItem.class);
        this.table = new JBTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        JenkinsSettings jenkinsSettings = JenkinsSettings.getSafeInstance(project);
        SnapshotWatcher watcher = new SnapshotWatcher(jenkinsSettings::setServerTableStyle);
        if (jenkinsSettings.hasPersistentTableStyle()) {
            int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                TableColumn column = table.getColumnModel().getColumn(i);
                Optional.ofNullable(jenkinsSettings.getServerTableColumnWidth(column.getHeaderValue().toString()))
                        .ifPresent(column::setPreferredWidth);
            }
        }
        table.getColumnModel().addColumnModelListener(new JTableHeader() {
            @Override
            public void columnMarginChanged(ChangeEvent e) {
                Map<String, Integer> preferredWidth = getPreferredWidth();
                watcher.updateSnapshot(preferredWidth);
            }
        });

        // 使用TableToolbarDecorator
        ToolbarDecorator decorator = CommTableToolbarDecorator.createDecorator(table);
        decorator.setAddAction(e -> {
            // 添加行的逻辑
            model.addRow(new JenkinsServerTableItem());
        });
        decorator.setRemoveAction(e -> {
            // 删除选中行的逻辑
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                model.removeRow(selectedRow);
            }
        });
        //初始化已有数据
        Optional.ofNullable(jenkinsSettings.getMultiSettings()).stream().flatMap(List::stream).forEach(setting -> {
            JenkinsServerTableItem tableItem = setting.toJenkinsServerTableItem();
            model.addRow(tableItem);
        });

        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD));
        final var reloadConfiguration = new JButton(JenkinsControlBundle.message("action.Jenkins.ReloadConfiguration.text"));
        reloadConfiguration.addActionListener(event -> reloadConfiguration(DataManager.getInstance().getDataContext(reloadConfiguration)));

        testConnection.addActionListener(event -> testConnection(model.getBean(table.getSelectedRow()), serverConnectionValidator));
        debugPanel.setVisible(false);
        debugPanel.setBorder(IdeBorderFactory.createTitledBorder(//
                JenkinsControlBundle.message("settings.server.debugInfo"), false,//
                JBUI.insetsTop(8)).setShowLine(false));
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(decorator.createPanel())
                .addLabeledComponent(JenkinsControlBundle.message("settings.server.connection_timeout"),
                        createConnectionTimeout())
                .addComponentToRightColumn(reloadConfiguration)
                .addComponentToRightColumn(createTestConnectionPanel())
                .addComponent(debugPanel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private Map<String, Integer> getPreferredWidth() {
        //保存列宽
        Map<String, Integer> preferredWidthMap = new HashMap<>();
        Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            int width = column.getPreferredWidth();
            preferredWidthMap.put(column.getHeaderValue().toString(), width);
        }
        return preferredWidthMap;
    }

    private void reloadConfiguration(@NotNull DataContext dataContext) {
        Optional.ofNullable(ActionManager.getInstance().getAction(ReloadConfigurationAction.ACTION_ID))
                .ifPresent(action -> ActionUtil.performAction(action, "ServerSetting", dataContext));
    }

    private static @NotNull JTextPane createDebugTextPane() {
        final var textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(JBColor.WHITE);
        final HTMLEditorKit simple = HTMLEditorKitBuilder.simple();
        textPane.setEditorKit(simple);
        textPane.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        return textPane;
    }

    private void testConnection(JenkinsServerTableItem tableItem, ServerConnectionValidator serverConnectionValidator) {
        try {
            new PositiveIntegerValidator().validate(connectionTimeout);
            debugPanel.setVisible(false);
            final var validationResult = serverConnectionValidator.validateConnection(tableItem.toServerSetting());
            if (validationResult.isValid()) {
                setConnectionFeedbackLabel(JBColor.GREEN,//
                        JenkinsControlBundle.message("settings.server.test_connection.successful"));
            } else {
                setConnectionFeedbackLabel(JBColor.RED,//
                        JenkinsControlBundle.message("settings.server.test_connection.invalidConfiguration"));
                debugPanel.setVisible(true);
                debugTextPane.setText(String.join("<br>", validationResult.getErrors()));
            }
        } catch (AuthenticationException authenticationException) {
            setConnectionFeedbackLabel(authenticationException);
            final var responseBody = authenticationException.getResponseBody();
            if (StringUtil.isNotBlank(responseBody)) {
                debugPanel.setVisible(true);
                debugTextPane.setText(responseBody);
            }
        } catch (Exception ex) {
            setConnectionFeedbackLabel(ex);
        }
    }

    private void setConnectionFeedbackLabel(@NotNull Exception cause) {
        setConnectionFeedbackLabel(JBColor.RED,//
                JenkinsControlBundle.message("settings.server.test_connection.fail", cause.getMessage()));
    }

    private void setConnectionFeedbackLabel(final Color labelColor, final String labelText) {
        GuiUtil.runInSwingThread(() -> {
            connectionStatusLabel.setForeground(labelColor);
            connectionStatusLabel.setText(labelText);
        });
    }

    private @NotNull JPanel createConnectionTimeout() {
        return GuiUtil.createLabeledComponent(connectionTimeout, JenkinsControlBundle.message("settings.seconds"));
    }

    private @NotNull JPanel createTestConnectionPanel() {
        return simplePanel(testConnection, connectionStatusLabel);
    }

    public @NotNull JPanel getPanel() {
        return mainPanel;
    }

    @SuppressWarnings("unchecked")
    public JenkinsServerTableItem getSelectedTableItem() {
        return Optional.ofNullable(((BeanDataTableModel<JenkinsServerTableItem>) this.table.getModel())
                .getBean(this.table.getSelectedRow())).orElse(new JenkinsServerTableItem());
    }

    @SuppressWarnings("unchecked")
    public List<MultiJenkinsSettings> getTableItems() {
        BeanDataTableModel<JenkinsServerTableItem> model = (BeanDataTableModel<JenkinsServerTableItem>) this.table.getModel();
        List<MultiJenkinsSettings> list = new LinkedList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            JenkinsServerTableItem bean = model.getBean(i);
            if (bean != null) {
                list.add(new MultiJenkinsSettings(bean));
            }
        }
        return list;
    }

    public MultiServerSettings getMultiServerSettings() {
        return new MultiServerSettings(getTableItems(), connectionTimeout.getNumber());
    }

    public int getConnectionTimeout() {
        return connectionTimeout.getNumber();
    }

    public void setConnectionTimeout(int timeout) {
        connectionTimeout.setNumber(timeout);
    }

    public void resetApiTokenModified() {
        setApiTokenModified(false);
    }

    private void setApiTokenModified(boolean apiTokenModified) {
        this.apiTokenModified = apiTokenModified;
    }
}
