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
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class FXMLDocumentController implements Initializable {

    private EventBus eventBus;
    private ArduinoDivider arduinoDivider;

    @FXML
    private Label currPosLabel;
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        eventBus = new EventBus();

        arduinoDivider = new ArduinoDivider();
        arduinoDivider.startSerial();
        arduinoDivider.setEventBus(eventBus);
        eventBus.register(this);
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

    @Subscribe
    private void handleEventBusEvent(FromArduinoMessageEvent event) {
        switch (event.getCommand()) {
            case COMMUNICATION_STARTED:
                // Get Arduino status
                eventBus.post(new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.GET_STATUS, 0));
                break;

            case PROGRAM_IS_HALTED:
                System.out.println("Got event Program is halted");
                setControlsForHaltedProgram();
                break;

            case PROGRAM_IS_RUNNING:
                System.out.println("Got event Program is running");
                setControlsForRunningProgram();
                break;

            case GOT_STATUS:
                System.out.println("got event " +  event.getMessage());
                enableOrDisableUIControls(arduinoDivider.getDividerStatus());
                break;
                
            default:
                break;
        }
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
    }

    private void setControlsForHaltedProgram() {
        runBtn.setDisable(true);
        stopBtn.setDisable(false);
        sendBtn.setDisable(false);
        loadBtn.setDisable(false);
        positionBtn.setDisable(false);
        setZeroBtn.setDisable(false);
    }

    private void enableAllControls() {
        runBtn.setDisable(false);
        stopBtn.setDisable(false);
        sendBtn.setDisable(false);
        loadBtn.setDisable(false);
        positionBtn.setDisable(false);
        setZeroBtn.setDisable(false);
    }
}
