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

import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class ArduinoDivider implements SerialPortEventListener{

    SerialPort serialPort;
    
    // Divider commands
    private final String COMMAND_GET_STATUS = "S";
    
    // A buffer for the character received from the serial port
    private final Queue<Integer> serialBuffer = new ConcurrentLinkedQueue<>();

    // Start up serial receiver event listener.
    private void initSerialReader() {
        try {
            serialPort.addEventListener(this);
        } catch (SerialPortException ex) {
            System.out.println("SerialPortException " + ex.getMessage());
        }
    }

    private enum DividerStatus {
        WAITING, DOWNLOADING, UPLOADING, RUNNING
    };

    private enum CommStatus {
        UP, DOWN
    };

    private CommStatus commStatus = CommStatus.DOWN;
    private double currentPosition = 0;

    public ArduinoDivider() {
        initSerialComm();
        initSerialReader();
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
        String response = waitForStatusResponse();
        return DividerStatus.WAITING;
    }

    private void sendCommand(String command) {
        if (commStatus == CommStatus.UP) {
            try {
                serialPort.writeByte("S".getBytes()[0]);
            } catch (SerialPortException ex) {
                System.out.println("serialPort.writeString exception " + ex.getMessage());
            }
        }
    }

    private void waitForStatusResponse() {
        try {
            ArrayList<Byte> charArray = new ArrayList<>();
            while (serialPort.getInputBufferBytesCount() > 0) {
                byte[] character = serialPort.readBytes(1);

                if (character[0] != 0x10) {
                    charArray.add(character[0]);
                }
                try {
                    sleep(100);
                } catch (InterruptedException ex) {

                }
            }
        } catch (SerialPortException ex) {
            System.out.println("serialPort.readBytes exception " + ex.getMessage());
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        //Object type SerialPortEvent carries information about which event occurred and a value.
        //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.
        if (event.isRXCHAR()) {
            try {
                int noOfBytesInBuffer = event.getEventValue();
                String charactersRead = serialPort.readString(noOfBytesInBuffer);
                charactersRead.chars().forEach(c -> serialBuffer.add(c));
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
    }
}
