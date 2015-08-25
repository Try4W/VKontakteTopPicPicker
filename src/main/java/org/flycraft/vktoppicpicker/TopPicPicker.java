package org.flycraft.vktoppicpicker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.flycraft.vktoppicpicker.controllers.DownloadScreenController;
import org.flycraft.vktoppicpicker.controllers.RootScreenController;
import org.flycraft.vktoppicpicker.donwloader.VkDownloader;

import java.io.IOException;

public class TopPicPicker extends Application {

    public final String RESOURCES_PATH = "/org/flycraft/vktoppicpicker/";
    public final String FXML_PATH = RESOURCES_PATH + "fxml/";

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_PATH + "root_screen.fxml"));
        Parent root = loader.load();
        RootScreenController controller = loader.getController();
        controller.setApplication(this);
        primaryStage.setTitle("TopPicPicker");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void startDownload(String wallUrl, int likesBorder) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_PATH + "download_screen.fxml"));
            Parent root = loader.load();
            DownloadScreenController controller = loader.getController();
            controller.setApplication(this);
            controller.setVkDownloader(new VkDownloader(wallUrl, likesBorder));
            controller.startVkDownloader();
            Stage stage = new Stage();
            controller.setStage(stage);
            stage.setTitle("Загрузка");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
