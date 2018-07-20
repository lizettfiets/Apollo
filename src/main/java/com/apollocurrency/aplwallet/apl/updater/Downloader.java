/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;

public class Downloader {
    private UpdaterMediator mediator = UpdaterMediator.getInstance();
    private DownloadExecutor defaultDownloadExecutor = new DefaultDownloadExecutor(TEMP_DIR_PREFIX, DOWNLOADED_FILE_NAME);

    private Downloader() {}

    public static Downloader getInstance() {
        return DownloaderHolder.INSTANCE;
    }

    private boolean checkConsistency(Path file, byte hash[]) {
        try {
            byte[] actualHash = calclulateHash(file);
            if (Arrays.equals(hash, actualHash)) {
                return true;
            }
        }
        catch (Exception e) {
            Logger.logErrorMessage("Cannot calculate checksum for file: " + file, e);
        }
        return false;
    }

    private byte[] calclulateHash(Path file) throws IOException, NoSuchAlgorithmException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return digest.digest();
        }
    }

    /**
     * Download file from uri and return Path to downloaded file
     * @param uri
     * @param hash
     * @param downloadExecutor - configurable download executor
     * @return
     */
    public Path tryDownload(String uri, byte[] hash, DownloadExecutor downloadExecutor) {
        int attemptsCounter = 0;
        mediator.setStatus(UpdateInfo.DownloadStatus.STARTED);
        while (attemptsCounter != UpdaterConstants.DOWNLOAD_ATTEMPTS) {
            try {
                attemptsCounter++;
                mediator.setState(UpdateInfo.DownloadState.IN_PROGRESS);
                Path downloadedFile = downloadExecutor.download(uri);
                if (checkConsistency(downloadedFile, hash)) {
                    mediator.setStatus(UpdateInfo.DownloadStatus.OK);
                    mediator.setState(UpdateInfo.DownloadState.FINISHED);
                    return downloadedFile;
                } else {
                    mediator.setStatus(UpdateInfo.DownloadStatus.INCONSISTENT);
                    Logger.logErrorMessage("Inconsistent file, downloaded from: " + uri);
                }
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                TimeUnit.SECONDS.sleep(NEXT_ATTEMPT_TIMEOUT);
            }
            catch (IOException e) {
                Logger.logErrorMessage("Unable to download update from: " + uri, e);
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                mediator.setStatus(UpdateInfo.DownloadStatus.CONNECTION_FAILURE);
            }
            catch (InterruptedException e) {
                Logger.logInfoMessage("Downloader was awakened", e);
            }
        }
        mediator.setState(UpdateInfo.DownloadState.FINISHED);
        mediator.setStatus(UpdateInfo.DownloadStatus.FAIL);
        return null;
    }

    /**
     * Download file from url and return Path to downloaded file
     * Uses default DownloadExecutor
     * @param uri
     * @param hash
     * @return
     */
    public Path tryDownload(String uri, byte[] hash) {
        return tryDownload(uri, hash, defaultDownloadExecutor);
    }

    private static class DownloaderHolder {
        private static final Downloader INSTANCE = new Downloader();
    }

    public interface DownloadExecutor {
        Path download(String uri) throws IOException;
    }

    public static class DefaultDownloadExecutor implements DownloadExecutor {

        private String tempDirPrefix;
        private String downloadedFileName;

        public DefaultDownloadExecutor(String tempDirPrefix, String downloadedFileName) {
            this.tempDirPrefix = tempDirPrefix;
            this.downloadedFileName = downloadedFileName;
        }

        public Path download(String uri) throws IOException {
            return downloadAttempt(uri, tempDirPrefix, downloadedFileName);
        }

        public static Path downloadAttempt(String url, String tempDirPrefix, String downloadedFileName) throws IOException {
            Path tempDir = Files.createTempDirectory(tempDirPrefix);
            Path downloadedFilePath = tempDir.resolve(Paths.get(downloadedFileName));
            try {
                URL webUrl = new URL(url);
                BufferedInputStream bis = new BufferedInputStream(webUrl.openStream());
                FileOutputStream fos = new FileOutputStream(downloadedFilePath.toFile());
                byte[] buffer = new byte[1024];
                int count;
                while ((count = bis.read(buffer, 0, 1024)) != -1) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                bis.close();
            }
            catch (Exception e) {
                //delete failed file and directory
                Files.deleteIfExists(downloadedFilePath);
                Files.deleteIfExists(tempDir);
                //rethrow exception
                throw e;
            }
            return downloadedFilePath;
        }

    }

}
