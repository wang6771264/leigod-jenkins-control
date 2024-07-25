package org.codinjutsu.tools.jenkins.model;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import lombok.*;

@Data
@Tag("favorite")
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteJob {

    @Setter(value = AccessLevel.NONE)
    @Attribute("name")
    private String name;

    @Setter(value = AccessLevel.NONE)
    @Attribute("url")
    private String url;
}