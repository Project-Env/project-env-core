package ch.projectenv.core.toolinfo.collector;

import ch.projectenv.core.toolinfo.ToolInfo;
import ch.projectenv.core.common.ProcessEnvironmentHelper;
import ch.projectenv.core.configuration.ToolConfiguration;
import ch.projectenv.core.toolinfo.ImmutableToolInfo;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractToolInfoCollector<ToolConfigurationType extends ToolConfiguration, ToolInfoType extends ToolInfo>
        implements ToolInfoCollector<ToolConfigurationType, ToolInfoType> {

    @Override
    public ToolInfoType collectToolInfo(ToolConfigurationType toolConfiguration, ToolInfoCollectorContext context) {
        File relevantToolBinariesDirectory = getRelevantToolBinariesDirectory(context.getToolBinariesRoot());

        ToolInfo baseToolInfo = collectBaseToolInfo(toolConfiguration, relevantToolBinariesDirectory);

        return collectToolSpecificInfo(baseToolInfo, toolConfiguration, context);
    }

    @Override
    public boolean supportsTool(ToolConfiguration toolConfiguration) {
        return getToolConfigurationClass().isAssignableFrom(toolConfiguration.getClass());
    }

    protected abstract Class<ToolConfigurationType> getToolConfigurationClass();

    protected File getRelevantToolBinariesDirectory(File toolBinariesDirectory) {
        List<File> files = Optional.ofNullable(toolBinariesDirectory.listFiles())
                .map(Arrays::asList)
                .orElse(List.of());

        if (files.size() == 1) {
            return files.get(0);
        } else {
            return toolBinariesDirectory;
        }
    }

    private ToolInfo collectBaseToolInfo(ToolConfigurationType toolConfiguration, File relevantToolBinariesDirectory) {
        Map<String, File> environmentVariables = new HashMap<>();
        environmentVariables.putAll(createFileMap(toolConfiguration.getEnvironmentVariables(), relevantToolBinariesDirectory));
        environmentVariables.putAll(createFileMap(getAdditionalExports(), relevantToolBinariesDirectory));

        List<File> pathElements = new ArrayList<>();
        pathElements.addAll(createFileList(toolConfiguration.getPathElements(), relevantToolBinariesDirectory));
        pathElements.addAll(createFileList(getAdditionalPathElements(), relevantToolBinariesDirectory));

        Optional<File> primaryExecutable = Optional.ofNullable(getPrimaryExecutableName())
                .map(primaryExecutableName -> {
                    File executable = ProcessEnvironmentHelper.resolveExecutableFromPathElements(primaryExecutableName, pathElements);
                    if (executable == null) {
                        throw new IllegalStateException("failed to resolve primary executable " + primaryExecutableName);
                    }

                    return executable;
                });

        return ImmutableToolInfo
                .builder()
                .toolName(toolConfiguration.getToolName())
                .location(relevantToolBinariesDirectory)
                .putAllEnvironmentVariables(environmentVariables)
                .addAllPathElements(pathElements)
                .primaryExecutable(primaryExecutable)
                .build();
    }


    private Map<String, File> createFileMap(Map<String, String> rawMap, File parent) {
        return rawMap
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .map(pair -> Pair.of(pair.getLeft(), new File(parent, pair.getRight())))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private List<File> createFileList(List<String> rawList, File parent) {
        return rawList
                .stream()
                .map(value -> new File(parent, value))
                .collect(Collectors.toList());
    }

    protected Map<String, String> getAdditionalExports() {
        return Map.of();
    }

    protected List<String> getAdditionalPathElements() {
        return List.of();
    }

    protected String getPrimaryExecutableName() {
        return null;
    }

    protected abstract ToolInfoType collectToolSpecificInfo(ToolInfo baseToolInfo, ToolConfigurationType toolConfiguration, ToolInfoCollectorContext context);

}
