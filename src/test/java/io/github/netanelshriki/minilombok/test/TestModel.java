package io.github.netanelshriki.minilombok.test;

import io.github.netanelshriki.minilombok.annotations.AllArgsConstructor;
import io.github.netanelshriki.minilombok.annotations.Getter;
import io.github.netanelshriki.minilombok.annotations.Setter;
import io.github.netanelshriki.minilombok.annotations.ToString;

/**
 * Test model class with all annotations.
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
public class TestModel {
    private String name;
    private int age;
    private boolean active;
}
