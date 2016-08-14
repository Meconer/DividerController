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
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    EventBus eventBus;

    // Divider commands
    private final byte COMMAND_DOWNLOAD_PROGRAM = 'D';
    private final byte COMMAND_UPLOAD_PROGRAM = 'U';
    private final byte COMMAND_RUN_PROGRAM = 'R';
    private final byte COMMAND_STEP_PLUS = '+';
    private final byte COMMAND_STEP_MINUS = '-';
    private final byte COMMAND_POSITION_TO = 'P';
    private final byte COMMAND_ZERO_POSITION = 'Z';
    private final byte COMMAND_GET_STATUS = 'S';
    private final byte COMMAND_GET_ANGLE = '?';
    private final byte COMMAND_STOP_RUNNING = 'Q';
    private final byte COMMAND_GET_VERSION = 'V';

    private final int SIZE_OF_BYTE_BUFFER = 50;

    private final byte etbChar = 23;
    private final byte eotChar = 27;

    private int threadCounter = 0;
    private boolean stopThread = false;

    private final byte[] receiveBuffer = new byte[SIZE_OF_BYTE_BUFFER];
    private volatile int numBytesInReceiveBuffer = 0;
    private boolean answerReceived = false;

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
    // Command that is queded to be sent. 0 is no command.
    private byte commandToBeSent = 0;

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

    @Subscribe
    private void handleEventBusEvent(ToArduinoMessageEvent event) {
        switch (event.getCommand()) {
            case QUIT_PROGRAM:
                sendStopCommand();
                break;
            default:
                break;
        }
    }

    void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.register(this);
    }

    private void sendStopCommand() {
        commandToBeSent = COMMAND_STOP_RUNNING;
        System.out.println("Stop Running");

    }

    // Start up serial receiver event listener.
    private void initSerialReader() {
        try {
            serialPort.addEventListener(this);
        } catch (SerialPortException ex) {
            System.out.println("SerialPortException " + ex.getMessage());
        }
    }

    private synchronized void emptyReceiveBuffer() {

    }

    private void initStateMachine() {
        Service stateMachineService = new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    private byte responseToWaitFor;
                    private boolean threadRun = true;

                    @Override
                    protected Void call() throws Exception {
                        while (threadRun) {
                            switch (currentCommState) {
                                case Idle:
                                    if (commandToBeSent != 0) {  // There is a command to be sent to Arduino
                                        System.out.println("Command to be sent " + commandToBeSent);
                                        sendCommand(commandToBeSent);
                                        currentCommState = CommState.WaitingForResponse;
                                        responseToWaitFor = commandToBeSent;
                                        commandToBeSent = 0;
                                        waitForResponse(responseToWaitFor);
                                    } else // No command. Keep idling. Throw away if anything is received from Arduino
                                    {
                                        if (answerReceived) {

                                        }
                                    }
                                    break;
                            }
                            sleep(1000);
                        }

                        return null;
                    }

                    private void waitForResponse(byte responseToWaitFor) {
                        System.out.println("Response to wait for " + responseToWaitFor);
                        boolean gotResponse = false;
                        while ( !gotResponse ) {
                            if ( answerReceived ) {
                                gotResponse = checkReceivedAnswer( responseToWaitFor );
                            }
                            try {
                                sleep(200);
                            } catch (InterruptedException ex) {
                                System.out.println("waitForResponse interrupted after sleep");
                            }
                        }
                    }

                    private boolean checkReceivedAnswer(byte responseToWaitFor) {
                        if ( numBytesInReceiveBuffer >0 ) {
                            System.out.println("Got here");
                            if ( receiveBuffer[ 0 ] == responseToWaitFor ) return true;
                        }
                        return false;
                    }

                };
            }
        };
        System.out.println("Starting state machine");
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

    private void sendCommand(byte command) {
        if (commStatus == CommStatus.UP) {
            try {
                serialPort.writeByte(command);
            } catch (SerialPortException ex) {
                System.out.println("serialPort.writeString exception " + ex.getMessage());
            }
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        //Object type SerialPortEvent carries information about which event occurred and a value.
        //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.

        if (event.isRXCHAR()) {  // If there are characters recieved
            try {
                // Get the characters read and add them to our serial buffer
                int numBytesReceived = event.getEventValue();
                byte[] b = serialPort.readBytes(numBytesReceived);
                int startByte = numBytesInReceiveBuffer;
                int endByte = numBytesReceived + numBytesInReceiveBuffer;
                for (int i = startByte; i < endByte; i++) {
                    byte ch = b[i];
                    if (ch > 30) {
                        System.out.print(String.valueOf((char) ch));
                    }
                    if (ch == etbChar) {
                        answerReceived = true;
                    }
                }

            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
    }
}
