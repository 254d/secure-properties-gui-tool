package secure.prop.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mulesoft.tools.SecurePropertiesTool;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.extern.slf4j.Slf4j;
import secure.prop.utils.model.Algorithm;
import secure.prop.utils.model.Mode;

@Slf4j
public class SecurePropertiesGuiController implements Initializable {
    @FXML
    private ComboBox<String> comboAlgorithm;
    @FXML
    private ComboBox<String> comboMode;
    @FXML
    private RadioButton radioEncrypt;
    @FXML
    private RadioButton radioDecrypt;
    @FXML
    private TextField textKey;
    @FXML
    private TextField textValue;
    @FXML
    private CheckBox checkRandomIV;
    @FXML
    private TextField textResult;
    @FXML
    private TextField textInputDir;
    @FXML
    private TextField textOutputDir;
    @FXML
    private RadioButton radioFile;
    @FXML
    private RadioButton radioFileLevel;

    @Override
    public void initialize(URL url, ResourceBundle br) {
        this.comboAlgorithm.getItems().addAll(Arrays.asList(Algorithm.values()).stream().map(Enum::name).toList());
        this.comboMode.getItems().addAll(Arrays.asList(Mode.values()).stream().map(Enum::name).toList());

        comboAlgorithm.setValue("Blowfish");
        comboMode.setValue("CBC");

        initFolderDrop();
    }

    @FXML
    private void handleRun() {
        String key = textKey.getText();
        String value = textValue.getText();
        String algorithm = comboAlgorithm.getValue();
        String mode = comboMode.getValue();
        boolean randomIV = checkRandomIV.isSelected();

        if (textKey == null || key.isEmpty() || textValue == null || value.isEmpty()) {
            return;
        }

        String action = "encrypt";
        if (radioDecrypt.isSelected()) {
            action = "decrypt";
        }

        log.debug("action: {}, algorithm: {}, mode: {}, key: {}, randomIV: {}, value: {}",
                action, algorithm, mode, key, randomIV, value);

        String result = null;
        try {
            result = SecurePropertiesTool.applyOverString(action, algorithm, mode, key, randomIV, value);
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Error", "", "Failed encryption/decryption.\n" + e.getMessage());

        }
        textResult.setText(result);
    }

    @FXML
    private void handleReverse() {
        textValue.setText(textResult.getText());
        if (radioEncrypt.isSelected()) {
            radioDecrypt.setSelected(true);
            radioEncrypt.setSelected(false);
        } else {
            radioEncrypt.setSelected(true);
            radioDecrypt.setSelected(false);
        }
    }

    @FXML
    private void handleRunBach() {
        String key = textKey.getText();
        String algorithm = comboAlgorithm.getValue();
        String mode = comboMode.getValue();
        boolean randomIV = checkRandomIV.isSelected();

        if (textKey == null || key.isEmpty()) {
            return;
        }

        String inputDir = normalizePath(textInputDir.getText());
        String outputDir = normalizePath(textOutputDir.getText());

        log.debug("inputDir: {}, outputDir: {} " + inputDir, outputDir);

        if (inputDir.isBlank() || outputDir.isBlank()) {
            return;
        }

        String type = "file";
        if (radioFileLevel.isSelected()) {
            type = "file-level";
        }

        // Warning: replace existing
        if (inputDir.equals(outputDir)) {
            boolean proceed = confirmSameDirectory();
            if (!proceed) {
                return;
            }
        }

        try {
            List<String> inputFiles = getInputFileNames(inputDir);
            for (String inputFile : inputFiles) {
                String inFile = Paths.get(inputDir, inputFile).toString();
                String outFile = Paths.get(outputDir, inputFile).toString();

                Path tempIn = Files.createTempFile("secure-properties_in_", ".yaml");
                String tempInFile = tempIn.toString();

                log.debug("copy: {} -> {}", inFile, tempInFile);
                Files.copy(new File(inFile).toPath(), tempIn, StandardCopyOption.REPLACE_EXISTING);

                Path tempOut = Files.createTempFile("secure-properties_out_", ".yaml");
                String tempOutFile = tempOut.toString();

                String action = "encrypt";
                if (radioDecrypt.isSelected()) {
                    action = "decrypt";
                }

                log.debug("inFile: {}", inFile);
                log.debug("outFile: {}", outFile);
                log.debug("action: {}, algorithm: {}, mode: {}, key: {}, randomIV: {}, tempInFile: {}, outFileTemp: {}",
                        action, algorithm, mode, key, randomIV, tempInFile, tempOutFile);

                if (type.equals("file")) {
                    SecurePropertiesTool.applyOverFile(action, algorithm, mode, key, randomIV, tempInFile,
                            tempOutFile);
                } else {
                    SecurePropertiesTool.applyHoleFile(action, algorithm, mode, key, randomIV, tempInFile,
                            tempOutFile);
                }

                log.debug("copy: {} -> {}", tempOutFile, outFile);
                Files.copy(tempOut, new File(outFile).toPath(), StandardCopyOption.REPLACE_EXISTING);

                log.debug("delete: {}", tempOutFile);
                // Files.deleteIfExists(tempIn); // BUG
                Files.deleteIfExists(tempOut);
            }

            alert(Alert.AlertType.INFORMATION, "Success", "",
                    "Encryption/decryption completed successfully.\nNumber of files processed: " + inputFiles.size());
        } catch (Exception e) {
            log.error("Failed encryption/decryption.", e);
            alert(Alert.AlertType.ERROR, "Error", "", "Failed encryption/decryption.\n" + e.getMessage());
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return new File(path).getAbsolutePath().trim();
    }

    private List<String> getInputFileNames(String inputDir) throws IOException {
        Path dir = Paths.get(inputDir);

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".yaml");
                    })
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        }
    }

    private boolean confirmSameDirectory() {
        Alert confirmAlert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "InputDir and OutputDir are the same folder.\nDo you want to continue?",
                ButtonType.YES,
                ButtonType.NO);

        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Input and output directories are the same.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void initFolderDrop() {
        // Input Dir
        textInputDir.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()
                    && !db.getFiles().isEmpty()
                    && db.getFiles().get(0).isDirectory()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        textInputDir.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File dropped = db.getFiles().get(0);
                if (dropped.isDirectory()) {
                    textInputDir.setText(dropped.getAbsolutePath());
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Output Dir
        textOutputDir.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()
                    && !db.getFiles().isEmpty()
                    && db.getFiles().get(0).isDirectory()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        textOutputDir.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File dropped = db.getFiles().get(0);
                if (dropped.isDirectory()) {
                    textOutputDir.setText(dropped.getAbsolutePath());
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    void alert(AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
