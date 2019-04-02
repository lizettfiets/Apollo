/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class DefaultPlatformDependentUpdater extends AbstractPlatformDependentUpdater {
    private String runTool;
    private String updateScriptPath;
        private static final Logger LOG = LoggerFactory.getLogger(DefaultPlatformDependentUpdater.class);

    public DefaultPlatformDependentUpdater(String runTool, String updateScriptPath, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(updaterMediator, updateInfo);
        this.runTool = runTool;
        this.updateScriptPath = updateScriptPath;
    }

    @Override
    Process runCommand(Path updateDirectory, Path workingDirectory, Path appDirectory, boolean userMode) throws IOException {
        String[] cmdArray = new String[] {
                runTool,
                updateDirectory.resolve(updateScriptPath).toAbsolutePath().toString(), //path to update script should include all subfolders
                appDirectory.toAbsolutePath().toString(),
                updateDirectory.toAbsolutePath().toString(),
                String.valueOf(userMode)
        };
        LOG.info("Runscript params {}", Arrays.toString(cmdArray));
        LOG.info("Working directory {}", workingDirectory.toFile().getPath());
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectOutput(appDirectory.resolve("updaterScriptLogs.log").toFile());
        builder.directory(workingDirectory.toFile()).command(cmdArray);
        return builder.start();
    }
}
