package org.flycraft.vktoppicpicker.donwloader;

public interface VkDownloaderListener {

    void onStateChange(VkDownloader.State state);

    void onDownloadStarted(int totalFilesCount);

    void onNewFileDownloaded(int downloadedFilesCount, String currentFileName);

    void onException(Exception exception);

}
