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

import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class ArduinoDivider {
    SerialPort serialPort;
    private final String COMMAND_GET_STATUS = "S";

    private enum DividerStatus { WAITING, DOWNLOADING, UPLOADING, RUNNING };
    private enum CommStatus { UP, DOWN };
    
    private CommStatus commStatus = CommStatus.DOWN;
    private double currentPosition = 0;

    public ArduinoDivider() {
        initSerialComm();
    }
 
    private void initSerialComm() {
        ComPortParameters comPortParams = new ComPortParameters();
        serialPort = new SerialPort(comPortParams.getComPort());
        try {
            serialPort.openPort();
            serialPort.setParams(comPortParams.getBaudRate(),
                    comPortParams.getDataBits(), 
                    comPortParams.getStopBits(),
                    comPortParams.getParity());
            commStatus = CommStatus.UP;
        } catch (SerialPortException ex) {
            System.out.println(ex.getMessage());
            commStatus = CommStatus.DOWN;
        }
    }

    private CommStatus getCommStatus() {
        return commStatus;
    }
    
    private boolean isCommUp() {
        return commStatus == CommStatus.UP;
    }
    
    private DividerStatus getDividerStatus() {
        sendCommand(COMMAND_GET_STATUS);
        String response = waitForResponse();
        return DividerStatus.WAITING;
    }
    
    private void sendCommand( String command ) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private String waitForResponse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
