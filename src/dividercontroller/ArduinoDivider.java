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
    
    private final String DEFAULT_COMM_PORT = "COM3";
    private enum DividerStatus { WAITING, DOWNLOADING, UPLOADING, RUNNING };
    private enum CommStatus { UP, DOWN };
    
    private CommStatus commStatus = CommStatus.DOWN;
    private double currentPosition = 0;

    public ArduinoDivider() {
        initSerialComm();
    }
 
    private void initSerialComm() {
        serialPort = new SerialPort(DEFAULT_COMM_PORT);
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            commStatus = CommStatus.UP;
        } catch (SerialPortException ex) {
            Logger.getLogger(ArduinoDivider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private CommStatus getCommStatus() {
        return commStatus;
    }
    
    private boolean isCommUp() {
        return commStatus == CommStatus.UP;
    }
    
    private DividerStatus getDividerStatus() {
        return DividerStatus.WAITING;
    }
    
}
