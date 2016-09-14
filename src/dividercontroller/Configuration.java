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

import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    private final String COMM_BAUDRATE_KEY = "CommBaudRate";
    private final int DEFAULT_COMM_BAUDRATE = SerialPort.BAUDRATE_115200;
    private final String COMM_DATABITS_KEY = "CommDataBits";
    private final int DEFAULT_COMM_DATABITS = SerialPort.DATABITS_8;
    private final String COMM_STOPBITS_KEY = "CommStopBits";
    private final int DEFAULT_COMM_STOPBITS = SerialPort.STOPBITS_1;
    private final String COMM_PARITY_KEY = "CommParity";
    private final int DEFAULT_COMM_PARITY = SerialPort.PARITY_NONE;
    
    private final String initialDirectoryName = null;

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
        return initialDirectoryName;
    }
    
    public void showConfigurationDialog() {
        List<String> portList = SerialCommHandler.getAvailablePorts();
        
        int selectedIndex = -1;
        int counter = 0;
        for ( String s :portList ) {
            if ( s.equals(commPort) ) selectedIndex = counter;
            counter++;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Inställningar");
        Label label = new Label("Comport:");
        
        ChoiceBox cb = new ChoiceBox();
        cb.setItems(FXCollections.observableList(portList));
        if ( selectedIndex >= 0 ) cb.getSelectionModel().select(selectedIndex);
        HBox hBox = new HBox(label,cb);
        hBox.setPadding(new Insets(20));
        hBox.setSpacing(10);
        
        BorderPane borderPane = new BorderPane( hBox );
        
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.showAndWait();
        String selected = (String) cb.getSelectionModel().getSelectedItem();
        System.out.println("Selected " + selected);
        if ( !selected.equals(commPort) ) {
            commPort = selected;
            
        }
    }
    
    
}
