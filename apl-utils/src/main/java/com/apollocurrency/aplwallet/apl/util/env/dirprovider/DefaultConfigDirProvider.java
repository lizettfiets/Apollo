/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.io.File;
import java.nio.file.Paths;

/**
 * Config dir provider which provide default config files locations
 */
public class DefaultConfigDirProvider implements ConfigDirProvider {
    protected String applicationName;
    protected boolean isService;

    public DefaultConfigDirProvider(String applicationName, boolean isService) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;
    }

    private boolean checkDirIfExist(String path){
        File dir = new File(path);
        return dir.exists();
    }
        
    @Override
    public String getInstallationConfigDirectory() {
        String confDir = DirProviderUtil.getBinDir().resolve("conf").toAbsolutePath().toString();
        return checkDirIfExist(confDir) ? confDir : "";
    }

    @Override
    public String getSysConfigDirectory() {
        return getInstallationConfigDirectory();
    }

    @Override
    public String getUserConfigDirectory() {
        if (isService) return getInstallationConfigDirectory();
        else {
            String confDir = Paths.get(System.getProperty("user.home"), "." + applicationName, "conf").toAbsolutePath().toString();
            return checkDirIfExist(confDir) ? confDir : "";
        }
        
        
    }
}
