package secure.prop.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurePropertiesGui extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Bug workaround
        Path tmpdir = Path.of(System.getProperty("java.io.tmpdir"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmpdir, "secure-properties_*_*.yaml")) {
            for (Path entry : stream) {
                Files.deleteIfExists(entry);
            }
        } catch (Exception e) {
            log.error("Error cleaning up temporary files: {}", e.getMessage());
        }

        try {
            Pane root = (Pane) FXMLLoader.load(getClass().getResource("SecurePropertiesGuiView.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Secure Properties GUI Tool");
            primaryStage.show();
        } catch (IOException e) {
            log.error("Error loading FXML file: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
