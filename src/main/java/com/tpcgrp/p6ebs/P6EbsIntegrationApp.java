// Main Application Class
package com.tpcgrp.p6ebs;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.tpcgrp.p6ebs.config.SpringConfig;

public class P6EbsIntegrationApp extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(SpringConfig.class)
                .run();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Scene scene = new Scene(fxmlLoader.load(), 1024, 768);

            // Load CSS
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            primaryStage.setTitle("P6-EBS Integration Tool");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}