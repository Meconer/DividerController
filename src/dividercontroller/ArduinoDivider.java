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
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 *
 * This class handles the arduino divider unit. It has different states as
 * follows
 *
 * State 0 : Waiting for commands from pc State 1 : Loading divider program from
 * pc State 2 : Sending divider program to pc State 3 : Running program
 *
 * For commands executed during state 0 the process is as following
 *
 * Send the command and possible values to Arduino
 *
 * The possible commands are: D Download from PC. D followed by the program and
 * then eof (27) U Upload to PC. Sends the program followed by eof and "Upload
 * finished" R Start automatic execution of program. Sends "R" + Jog 1 step
 * positive. Responds with "+" and "A" followed by the angle - Jog 1 step
 * negative Responds with "-" and "A" followed by the angle P angle Positions to
 * angle. Responds with "Pangle" and then "Aangle" ? Request current position.
 * Responds with "Aangle" S Request current status (0 or 3). Response "S0" or
 * "S3" Z Set current position to zero. Response "Z" and then "A0.00" V Request
 * firmware version. Sends a version string.
 *
 * All the commands is acknowledged with the command as above and all responses
 * is ended with an ETB character (23);
 *
 * This is handled in this class with a state machine that has different states
 * as follows.
 */
public class ArduinoDivider implements SerialPortEventListener {

    SerialPort serialPort;

    // Divider commands
    private final String COMMAND_GET_STATUS = "S";
    private final String COMMAND_GET_ANGLE = "?";
    private final String COMMAND_STOP_RUNNING = "Q";
    private final String COMMAND_GET_VERSION = "V";

    private final int SIZE_OF_BYTE_BUFFER = 50;
    
    private final byte etbChar = 23;
    private final byte eotChar = 27;

    private static int threadCounter = 0;
    private boolean stopThread = false;

    /**
     * The different states of the state machine
     */
    private enum CommState {
        Idle,
        Sending,
        WaitingForResponse,
        WaitingForAngle,
        WaitingForStatus,
        DownloadProgramToArduino,
        UploadProgramToPc
    };

    // The current state
    private CommState currentCommState = CommState.Idle;

    // A buffer for the character received from the serial port
    private final Queue<Byte> serialBuffer = new ConcurrentLinkedQueue<>();

    private enum CommStatus {
        UP, DOWN
    };

    private CommStatus commStatus = CommStatus.DOWN;

    private enum DividerStatus {
        WaitingForCommand, RunningProgram
    };

    private double currentPosition = 0;

    public ArduinoDivider() {
        // Init serial comm parameters.
        initSerialComm();
        // Start serial communication thread
        initSerialReader();
        // Start up state machine thread
        initStateMachine();
    }

    // Start up serial receiver event listener.
    private void initSerialReader() {
        try {
            serialPort.addEventListener(this);
        } catch (SerialPortException ex) {
            System.out.println("SerialPortException " + ex.getMessage());
        }
    }

    private static int state = 0;

    private void initStateMachine() {
        Service stateMachineService = new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        while (state < 10) {
                            System.out.println("state=" + state);
                            if (state == 5) {
                                sendCommand(COMMAND_STOP_RUNNING);
                            }
                            if (state == 6) {
                                sendCommand(COMMAND_GET_VERSION);
                            }
                            state++;
                            sleep(2000);
                        }
                        return null;

                    }
                };
            }
        };
        stateMachineService.start();
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
        //String response = waitForStatusResponse();
        return DividerStatus.WaitingForCommand;
    }

    private void sendCommand(String command) {
        if (commStatus == CommStatus.UP) {
            try {
                serialPort.writeByte(command.getBytes()[0]);
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

    byte[] byteBuffer = new byte[SIZE_OF_BYTE_BUFFER];

    @Override
    public void serialEvent(SerialPortEvent event) {
        //Object type SerialPortEvent carries information about which event occurred and a value.
        //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.

        if (event.isRXCHAR()) {  // If there are characters recieved
            try {
                // Get the characters read and add them to our serial buffer
                int noOfBytesInBuffer = event.getEventValue();
                byte[] b = serialPort.readBytes(noOfBytesInBuffer);
                //System.out.println(  b.toString() );
                for (int i = 0; i < noOfBytesInBuffer; i++) {
                    byte ch = b[i];
                    if (ch > 30) {
                        System.out.print(String.valueOf((char) ch ));
                    }
                    
                }

            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
    }
}
