package org.flycraft.vktoppicpicker.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.flycraft.vktoppicpicker.TopPicPicker;
import org.flycraft.vktoppicpicker.donwloader.VkDownloader;
import org.flycraft.vktoppicpicker.donwloader.VkDownloaderListener;
import org.flycraft.vktoppicpicker.utils.Dialogs;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.ResourceBundle;

public class DownloadScreenController implements Initializable {

    @FXML
    private Hyperlink postHyperlink;
    @FXML
    private Label currentOperationLabel;
    @FXML
    private Label currentFileLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;

    private TopPicPicker application;
    private VkDownloader vkDownloader;

    private int totalFilesCount;

    private Stage rootStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setVkDownloader(VkDownloader vkDownloader) {
        postHyperlink.setText(vkDownloader.getWallUrl());
        postHyperlink.setOnMouseClicked(event -> application.getHostServices().showDocument(postHyperlink.getText()));
        vkDownloader.setListener(new VkDownloaderListener() {
            @Override
            public void onStateChange(VkDownloader.State state) {
                Platform.runLater(() -> {
                    switch (state) {
                        case LOADING_COMMENTS: currentOperationLabel.setText("Получение и обработка комментариев"); break;
                        case DOWNLOADING: currentOperationLabel.setText("Загрузка изображений"); break;
                        case DONE: currentOperationLabel.setText("Готово"); break;
                        case CANCELED: currentOperationLabel.setText("Отменено"); break;
                    }
                });
            }

            @Override
            public void onDownloadStarted(int totalFilesCount) {
                DownloadScreenController.this.totalFilesCount = totalFilesCount;
            }

            @Override
            public void onNewFileDownloaded(int downloadedFilesCount, String currentFileName) {
                updateDownloadState(downloadedFilesCount);
                Platform.runLater(() -> currentFileLabel.setText(currentFileName));
            }

            @Override
            public void onException(Exception exception) {
                if(exception instanceof AccessDeniedException) {
                    Platform.runLater(rootStage::close);
                    vkDownloader.kill();
                    Dialogs.createAlertDialog("Ошибка", "Произошла ошибка ввода/вывода (" + exception.getClass().getName() + "). Программа не может получить доступ к некоторым файлам. Возможно, вы открыли записываемую папку в проводнике, если это так, закройте её. Так же может помочь запуск программы от имени администратора.");
                } else if(exception instanceof ConnectException) {
                    vkDownloader.kill();
                    Dialogs.createAlertDialog("Ошибка", "Произошла ошибка подключения к VK API (" + exception.getClass().getName() + "). Проверьте подключение к интернету, а мб контач опять лег.");
                }
            }
        });
        this.vkDownloader = vkDownloader;
    }

    public void setApplication(TopPicPicker application) {
        this.application = application;
    }

    public void setStage(Stage rootStage) {
        rootStage.setOnCloseRequest(event -> stopVkDownloader());
        this.rootStage = rootStage;
    }

    public void startVkDownloader() {
        System.out.println("Starting vk downloader...");
        if(vkDownloader == null) {
            throw new RuntimeException("At first you need to set vk downloader and then start it");
        }
        this.vkDownloader.start();
    }

    public void stopVkDownloader() {
        System.out.println("Stopping vk downloader...");
        if(vkDownloader == null) {
            throw new RuntimeException("At first you need to set vk downloader and then start it");
        }
        this.vkDownloader.kill();
    }

    private void updateDownloadState(int downloadedFilesCount) {
        Platform.runLater(() -> progressLabel.setText(downloadedFilesCount + "/" + totalFilesCount));
        double progress = (double) downloadedFilesCount / (double) totalFilesCount;
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

}
