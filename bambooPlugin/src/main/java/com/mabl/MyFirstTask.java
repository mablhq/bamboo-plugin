package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import org.jetbrains.annotations.NotNull;

public class MyFirstTask implements TaskType {

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        buildLogger.addBuildLogEntry("Hello World!");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }
}
