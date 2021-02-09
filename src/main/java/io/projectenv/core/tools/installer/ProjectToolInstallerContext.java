package io.projectenv.core.tools.installer;

import org.immutables.value.Value;

import java.io.File;

@Value.Immutable
public interface ProjectToolInstallerContext {

    File getProjectRoot();

    File getToolRoot();

}