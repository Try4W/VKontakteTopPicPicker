package org.flycraft.vktoppicpicker.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.flycraft.vktoppicpicker.TopPicPicker;

import java.net.URL;
import java.util.ResourceBundle;

public class RootScreenController implements Initializable {

    @FXML
    private TextField wallLinkField;
    @FXML
    private TextField likesBorderField;
    @FXML
    private Button startDownloadButton;

    private TopPicPicker application;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wallLinkField.textProperty().addListener((observable, oldValue, newValue) -> {
            startDownloadButton.setDisable(!validateWallLink(newValue));
        });
        likesBorderField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                likesBorderField.setText(oldValue);
            }
        });
        startDownloadButton.setOnMouseClicked(event -> {
            application.startDownload(wallLinkField.getText(), Integer.valueOf(likesBorderField.getText()));
        });
    }

    public boolean validateWallLink(String link) {
        String wallLinkText = wallLinkField.getText();
        return wallLinkText.contains("vk.com") && wallLinkText.contains("w=wall");
    }

    public void setApplication(TopPicPicker application) {
        this.application = application;
    }

}
