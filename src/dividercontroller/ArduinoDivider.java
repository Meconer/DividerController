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
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import static java.lang.Thread.sleep;

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
public class ArduinoDivider {

    SerialCommHandler serialCommHandler;

    EventBus eventBus;
    private final int RESPONSE_TIMEOUT = 3000;
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

    private enum DividerStatus {
        WaitingForCommand, RunningProgram
    };

    private double currentPosition = 0;

    public ArduinoDivider() {
         initStateMachine();
         serialCommHandler = new SerialCommHandler();

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

    private synchronized void emptyReceiveBuffer() {

    }

    private byte responseToWaitFor;
    boolean gotResponse = false;

    private void initStateMachine() {
        Service stateMachineService = new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    private boolean threadRun = true;

                    @Override
                    protected Void call() throws Exception {
                        while (threadRun) {
                            switch (currentCommState) {
                                case Idle:
                                    if (commandToBeSent != 0) {  // There is a command to be sent to Arduino
                                        System.out.println("Command to be sent " + commandToBeSent);
                                        serialCommHandler.sendCommand(commandToBeSent);
                                        currentCommState = CommState.WaitingForResponse;
                                        responseToWaitFor = commandToBeSent;
                                        commandToBeSent = 0;
                                        waitForResponse(responseToWaitFor);
                                    } else // No command. Keep idling. Throw away if anything is received from Arduino
                                     if (answerReceived) {

                                        }
                                    break;
                            }
                            sleep(1000);
                        }

                        return null;
                    }

                    private void waitForResponse(byte responseToWaitFor) {
                        System.out.println("Response to wait for " + responseToWaitFor);
                        long timeOutTime = System.currentTimeMillis() + RESPONSE_TIMEOUT;
                        System.out.println("timeOutTime " + timeOutTime);
                        boolean timeOut = false;
                        while (!gotResponse && !timeOut) {
                            if (answerReceived) {
                                gotResponse = checkReceivedAnswer(responseToWaitFor);
                            }
                            if ( System.currentTimeMillis() > timeOutTime ) {
                                System.out.println("Timeout");
                                currentCommState = CommState.Idle;
                                timeOut = true;
                            }
                            try {
                                sleep(200);
                            } catch (InterruptedException ex) {
                                
                            }
                        }
                    }

                    private boolean checkReceivedAnswer(byte responseToWaitFor) {
                        if (!receiveBuffer.isEmpty()) {
                            System.out.println("Got here");
                            if (receiveBuffer.peek() == responseToWaitFor) {
                                receiveBuffer.remove();
                            }
                        }
                        return false;
                    }

                };
            }
        };
        System.out.println("Starting state machine");
        stateMachineService.start();
    }

    private DividerStatus getDividerStatus() {
        sendCommand(COMMAND_GET_STATUS);
        //String response = waitForStatusResponse();
        return DividerStatus.WaitingForCommand;
    }

}
