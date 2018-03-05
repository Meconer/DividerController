/*
 * Copyright (C) 2016 Mats Andersson <mats.andersson@mecona.se>.
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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import static dividercontroller.Utils.showError;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class FXMLDocumentController implements Initializable {

    private final EventBus eventBus = ProjectEventBus.getInstance();
    private ArduinoDivider arduinoDivider;

    
    @FXML
    private Label currPosLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button setZeroBtn;
    @FXML
    private TextArea programTextArea;
    @FXML
    private Button runBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Button sendBtn;
    @FXML
    private Button loadBtn;
    @FXML
    private Button openBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private TextField positionTxtFld;
    @FXML
    private Button positionBtn;
    @FXML 
    private Button stepPositiveButton;
    @FXML 
    private Button stepNegativeButton;
    @FXML
    private Pane mainWindow;
    
    private Parent root;

    public void setArduinoDivider(ArduinoDivider arduinoDivider) {
        this.arduinoDivider = arduinoDivider;
    }

    
    
    // Setup buttons and labels from Arduino status.
    private void enableOrDisableUIControls(ArduinoDivider.DividerStatus dividerStatus) {
        switch (dividerStatus) {
            case RunningProgram:
                // If it is running a program, enable the start button and disable everything else
                setControlsForRunningProgram();
                break;

            case WaitingForCommand:
                // If it is not running a program, disable the start button and enable everything else
                setControlsForHaltedProgram();
                break;

            case Unknown:
                // Unknown. Enable all controls
                enableAllControls();
                break;

            default:
                System.out.println("Impossible error");
                break;

        }
        // Get the current angular position and show it in the position label

    }

    @FXML
    private void onStopBtnClicked() {
        ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.QUIT_PROGRAM, 0);
        eventBus.post(event);
    }

    @FXML
    private void onRunBtnClicked() {
        ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.RUN_PROGRAM, 0);
        eventBus.post(event);
    }

    @FXML
    private void onSetZeroBtnClicked() {
        ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.ZERO_POSITION, 0);
        eventBus.post(event);
    }

    @FXML
    private void onPositionBtnClicked() {
        String positionText = positionTxtFld.getText().replaceAll(",", ".");
        double position;
        try {
            position = Double.parseDouble(positionText);
            ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.POSITION_TO, position);
            eventBus.post(event);
        } catch ( NumberFormatException ex) {
            showError("Felaktigt numeriskt format");
        }
    }
    
    @FXML
    private void onStepPositiveBtnClicked() {
        ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.STEP_POSITIVE, 0);
        eventBus.post(event);
    }
    
    @FXML
    private void onStepNegativeBtnClicked() {
        ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.STEP_NEGATIVE, 0);
        eventBus.post(event);
    }
    
    @FXML
    private void onLoadBtnClicked() {
        ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.UPLOAD_TO_PC, 0);
        eventBus.post(event);
    }
    
    @FXML 
    private void onSendButtonClicked() {
        DividerProgram dividerProgram = new DividerProgram(programTextArea.getText());
        if ( dividerProgram.isSyntaxOk() ) {
            eventBus.post(new DownloadProgramMessage( dividerProgram.getDownloadToArduinoText()));
        } else {
            showError( dividerProgram.getSyntaxErrorMessage());
        }
    }
    
    @FXML
    private void onOpenButtonClicked() {
        actionOpenProgram();
    }
    
    @FXML
    private void onMenuOpenClicked() {
        actionOpenProgram();
    }

    private void actionOpenProgram() {
        DividerProgram dividerProgram = new DividerProgram();
        dividerProgram.openFromDisc();
        if ( dividerProgram.isSyntaxOk() ) {
            programTextArea.clear();
            programTextArea.setText(dividerProgram.getText());
        }
    }

    @FXML
    private void onSaveButtonClicked() {
        actionSaveProgram();
    }

    @FXML
    private void onMenuSaveClicked() {
        actionSaveProgram();
    }

    
    private void actionSaveProgram() {
        DividerProgram dividerProgram = new DividerProgram(programTextArea.getText());
        if ( dividerProgram.isSyntaxOk() ) {
            dividerProgram.saveToDisc();
        } else {
            showError("Syntaxfel. Kan inte sparas");
        }
    }
    
    @FXML
    private void onMenuSettingsClicked() {
        Configuration.getConfiguration().showConfigurationDialog();
    }
    
    @FXML
    private void onMenuExitClicked() {
        System.exit(0);
    }
    
    @FXML
    private void onMenuAboutClicked() {
        showAboutBox();
    }
    
    @Subscribe
    private void handleEventBusEvent(FromArduinoMessageEvent event) {
        switch (event.getCommand()) {
            case COMMUNICATION_STARTED:
                // Get Arduino status
                eventBus.post(new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.GET_STATUS, 0));
                break;

            case PROGRAM_IS_HALTED:
                //System.out.println("Got event Program is halted");
                Platform.runLater(() -> {
                    setControlsForHaltedProgram();
                });

                break;

            case PROGRAM_IS_RUNNING:
                //System.out.println("Got event Program is running");
                Platform.runLater(() -> {
                    setControlsForRunningProgram();
                });

                break;

            case GOT_STATUS:
                // System.out.println("got event " + event.getMessageType());
                Platform.runLater(() -> {
                    enableOrDisableUIControls(arduinoDivider.getDividerStatus());
                });
                
                break;

            case GOT_POSITION:
                double position = event.getValue();
                //System.out.println("Position :" + position);
                //System.out.println("Got event " + event.getMessageType());
                DecimalFormat df = new DecimalFormat("0.00");
                String positionText = df.format(position).replaceAll(",", ".");
               
                //System.out.println("positionText :" + positionText);
                Platform.runLater(() -> {
                    currPosLabel.setText(positionText);
                });
                break;

            default:
                break;
        }
    }
    
    @Subscribe
    private void handleUploadedProgramMessage( UploadedProgramMessage message ) {
        programTextArea.clear();
        programTextArea.setText(message.getCleanedUpText());
    }
    
    @Subscribe
    private void handleArduinoStatusMessageEvent( ArduinoStatusMessageEvent asmEvent ) {
        Platform.runLater( () -> {
            statusLabel.setText(asmEvent.getStatusMessage());
        });
    }

    void stopThreads() {
        arduinoDivider.stopThreads();
    }

    private void setControlsForRunningProgram() {
        runBtn.setDisable(true);
        stopBtn.setDisable(false);
        sendBtn.setDisable(true);
        loadBtn.setDisable(true);
        positionBtn.setDisable(true);
        setZeroBtn.setDisable(true);
        stepPositiveButton.setDisable(true);
        stepNegativeButton.setDisable(true);
    }

    private void setControlsForHaltedProgram() {
        runBtn.setDisable(false);
        stopBtn.setDisable(true);
        sendBtn.setDisable(false);
        loadBtn.setDisable(false);
        positionBtn.setDisable(false);
        setZeroBtn.setDisable(false);
        stepPositiveButton.setDisable(false);
        stepNegativeButton.setDisable(false);
    }

    private void enableAllControls() {
        runBtn.setDisable(false);
        stopBtn.setDisable(false);
        sendBtn.setDisable(false);
        loadBtn.setDisable(false);
        positionBtn.setDisable(false);
        setZeroBtn.setDisable(false);
        stepNegativeButton.setDisable(false);
        stepNegativeButton.setDisable(false);
    }

    private void disableAllControls() {
        runBtn.setDisable(true);
        stopBtn.setDisable(true);
        sendBtn.setDisable(true);
        loadBtn.setDisable(true);
        positionBtn.setDisable(true);
        setZeroBtn.setDisable(true);
        stepNegativeButton.setDisable(true);
        stepPositiveButton.setDisable(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventBus.register(this);
        disableAllControls();
    }

    void setRoot(Parent root) {
        this.root = root;
    }

    private void showAboutBox() {
        Alert aboutBox = new Alert(Alert.AlertType.INFORMATION);
        aboutBox.setHeaderText("Om");
        aboutBox.setContentText("Delningsapparatcontroller\nVersion 0.1");
        aboutBox.showAndWait();
    }

}
