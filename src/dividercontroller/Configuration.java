/*
 * Copyright (C) 2016 matsa.
 *
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this code; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package dividercontroller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import jssc.SerialPort;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class Configuration {

    public final static int DEBUG_LEVEL = 2;
    private final String CONFIG_NAME = "se.mecona.dividerController";
    private final String DEFAULT_COMPORT = "COM3";
    private final String COMPORT_KEY = "CommPort";
    private final String INITIAL_PATH_KEY = "InitialPath";
    
    private final String COMM_BAUDRATE_KEY = "CommBaudRate";
    private final int DEFAULT_COMM_BAUDRATE = SerialPort.BAUDRATE_115200;
    private final String COMM_DATABITS_KEY = "CommDataBits";
    private final int DEFAULT_COMM_DATABITS = SerialPort.DATABITS_8;
    private final String COMM_STOPBITS_KEY = "CommStopBits";
    private final int DEFAULT_COMM_STOPBITS = SerialPort.STOPBITS_1;
    private final String COMM_PARITY_KEY = "CommParity";
    private final int DEFAULT_COMM_PARITY = SerialPort.PARITY_NONE;

    private String initialPath = null;

    private final Preferences prefs = Preferences.userNodeForPackage(getClass());
    private String commPort;
    private final int commBaudRate;
    private final int commDataBits;
    private final int commStopBits;
    private final int commParity;

    private static final Configuration INSTANCE = new Configuration();

    public static final String YES_BUTTON_TEXT = "Ja";
    public static final String NO_BUTTON_TEXT = "Nej";
    private final int SETTINGS_DIALOG_WIDTH = 400;
    private final int SETTINGS_DIALOG_HEIGHT = 200;

    private Configuration() {
        commPort = prefs.get(COMPORT_KEY, DEFAULT_COMPORT);
        initialPath = prefs.get(INITIAL_PATH_KEY, initialPath);
        commBaudRate = prefs.getInt(COMM_BAUDRATE_KEY, DEFAULT_COMM_BAUDRATE);
        commDataBits = prefs.getInt(COMM_DATABITS_KEY, DEFAULT_COMM_DATABITS);
        commStopBits = prefs.getInt(COMM_STOPBITS_KEY, DEFAULT_COMM_STOPBITS);
        commParity = prefs.getInt(COMM_PARITY_KEY, DEFAULT_COMM_PARITY);
    }

    public static Configuration getConfiguration() {
        return INSTANCE;
    }

    public String getComport() {
        return commPort;
    }

    public String getCommPort() {
        return commPort;
    }

    public int getCommBaudRate() {
        return commBaudRate;
    }

    public int getCommDataBits() {
        return commDataBits;
    }

    public int getCommStopBits() {
        return commStopBits;
    }

    public int getCommParity() {
        return commParity;
    }

    public String getInitialDirectoryName() {
        return initialPath;
    }

    private Dialog<SettingsDialogData> buildSettingsDialog(SettingsDialogData currentData) {
        Dialog<SettingsDialogData> settingsDialog = new Dialog<>();
        settingsDialog.setTitle("Inställningar");
        Label label1 = new Label("Serieport:");
        Label label2 = new Label("Startkatalog:");
        List<String> portList = SerialCommHandler.getAvailablePorts();
        ChoiceBox<String> cbCommPort = new ChoiceBox<>();
        cbCommPort.setItems(FXCollections.observableList(portList));
        cbCommPort.getSelectionModel().select(currentData.commPort);
        TextField tfDefaultPath = new TextField(currentData.initialPath);
        tfDefaultPath.setPrefWidth(300);
        
        GridPane gridPane = new GridPane();
        gridPane.add(label1, 0, 0);
        GridPane.setHalignment(label1, HPos.RIGHT);
        gridPane.add(label2, 0, 1);
        GridPane.setHalignment(label2, HPos.RIGHT);
        gridPane.add(cbCommPort, 1, 0);
        gridPane.add(tfDefaultPath, 1, 1);
        gridPane.setPadding(new Insets(30));
        gridPane.setVgap(20);
        gridPane.setHgap(20);
        settingsDialog.getDialogPane().setContent(gridPane);
        ButtonType buttonTypeOk = new ButtonType("Ok", ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Avbryt", ButtonData.CANCEL_CLOSE);

        settingsDialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

        settingsDialog.setResultConverter((ButtonType bt) -> {
            if (bt == buttonTypeOk) {
                return new SettingsDialogData(cbCommPort.getSelectionModel().getSelectedItem(), tfDefaultPath.getText());
            }
            return null;
        });

        return settingsDialog;
    }

    public void showConfigurationDialog() {
        Dialog<SettingsDialogData> sd = buildSettingsDialog(new SettingsDialogData(commPort, initialPath));
        sd.setTitle("Inställningar");
        Optional<SettingsDialogData> result = sd.showAndWait();
        if (result.isPresent()) {
            SettingsDialogData newSettings = result.get();
            String newCommPort = newSettings.commPort;
            System.out.println("Comport : " + newCommPort);
            if (!newCommPort.equals(commPort)) {
                commPort = newCommPort;
                ProjectEventBus.getInstance().post(new ProgramEvent(ProgramEvent.Command.NEW_SERIAL_PORT_SELECTED, 0));
                prefs.put(COMPORT_KEY, commPort);
            }
            String newInitialPath = newSettings.initialPath;
            if (newInitialPath != null) {
                if (Files.isDirectory(Paths.get(newSettings.initialPath))) {
                    initialPath = newSettings.initialPath;
                    ProjectEventBus.getInstance().post(new ProgramEvent(ProgramEvent.Command.NEW_INITIAL_PATH_SELECTED, 0));
                    prefs.put(INITIAL_PATH_KEY, initialPath);
                }
            }
        }
    }

}
