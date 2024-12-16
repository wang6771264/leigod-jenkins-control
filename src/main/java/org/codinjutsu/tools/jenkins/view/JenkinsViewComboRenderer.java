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

package org.codinjutsu.tools.jenkins.view;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.list.ListCellBackgroundSupplier;
import com.intellij.util.ui.UIUtil;
import java.awt.Color;
import javax.swing.JList;
import org.codinjutsu.tools.jenkins.model.jenkins.FavoriteView;
import org.codinjutsu.tools.jenkins.model.jenkins.ViewV2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JenkinsViewComboRenderer extends SimpleListCellRenderer<ViewV2>
        implements ListCellBackgroundSupplier<ViewV2> {

    public void renderView(ViewV2 view) {
        final boolean isNestedParent = view.hasNestedView();
        setEnabled(!isNestedParent);
        setFocusable(!isNestedParent);
        if (isNestedParent) {
            setIcon(AllIcons.Nodes.Folder);
            setBackground(getCellBackground(view, 0));
            setForeground(UIUtil.getLabelDisabledForeground());
        }
        setText(getText(view));
        if (view instanceof FavoriteView) {
            setIcon(JenkinsTreeRenderer.FAVORITE_ICON);
        }
    }

    @Override
    public void customize(@NotNull JList<? extends ViewV2> list, ViewV2 view, int index, boolean selected, boolean hasFocus) {
        renderView(view);
    }

    @Override
    public @Nullable Color getCellBackground(ViewV2 view, int row) {
        return view.hasNestedView() ? JBColor.LIGHT_GRAY : null;
    }

    private @NotNull String getText(ViewV2 view) {
        final var viewName = view.getAlias() != null ? view.getAlias() : view.getName();
        return view.isNested() ? "   " + viewName : viewName;
    }
}
