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
        arduinoDivider.setEventBus( eventBus);
        //initUIControls();
    }    

    // Setup buttons and labels from Arduino status.
    private void initUIControls() {
        // If it is running a program, enable the start button and disable everything else
        // If it is not running a program, disable the start button and enable everything else
        // Get the current angular position and show it in the position label
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @FXML
    private void onStopBtnClicked() {
       ToArduinoMessageEvent event = new ToArduinoMessageEvent(ToArduinoMessageEvent.Command.QUIT_PROGRAM, 0);
       eventBus.post(event);
       System.out.println("Stop button clicked");
        
    }
}
