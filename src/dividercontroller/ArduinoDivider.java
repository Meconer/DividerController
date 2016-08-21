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
import java.nio.charset.StandardCharsets;
import static java.lang.Thread.sleep;
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.lang.Thread.sleep;
import static java.lang.Thread.sleep;
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
    private final long DELAY_BEFORE_GETTING_FIRST_STATUS = 3000;

    /**
     * The different states of the state machine
     */
    private enum CommState {
        StartingUp,
        Idle,
        Sending,
        WaitingForResponse,
        WaitingForAngle,
        WaitingForStatus,
        DownloadProgramToArduino,
        UploadProgramToPc
    };

    // The current state
    private CommState currentCommState = CommState.StartingUp;
    // Command that is queded to be sent. 0 is no command.
    private byte commandToBeSent = 0;

    private final ConcurrentLinkedQueue<CommandToDivider> commandSendQueue = new ConcurrentLinkedQueue<>();

    public enum DividerStatus {
        Unknown,
        WaitingForCommand,
        RunningProgram
    };

    private DividerStatus dividerStatus = DividerStatus.Unknown;

    private long timeToGetFirstStatus;
    private double currentPosition = 0;
    private boolean threadRun = true;

    public ArduinoDivider() {
        serialCommHandler = new SerialCommHandler();
        sendGetStatusCommand();
        initCommandSender();
        initMessageReceiver();
    }

    void startSerial() {
        serialCommHandler.startReader();
        timeToGetFirstStatus = System.currentTimeMillis() + DELAY_BEFORE_GETTING_FIRST_STATUS;
    }

    @Subscribe
    private void handleEventBusEvent(ToArduinoMessageEvent event) {
        switch (event.getCommand()) {
            case QUIT_PROGRAM:
                sendStopCommand();
                break;
            case GET_STATUS:
                sendGetStatusCommand();
                System.out.println("Get Status sent");
                break;
            default:
                break;
        }
    }

    void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.register(this);
        serialCommHandler.setEventBus(eventBus);
    }

    private String byteToString(byte byteToConvert) {
        return new String(new byte[]{(byte) byteToConvert}, StandardCharsets.US_ASCII);
    }

    private void sendStopCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.STOP_RUNNING));

        System.out.println("Stop Running sent");
        String responseToWaitFor = byteToString(commandToBeSent);
        waitForResponse(responseToWaitFor, DEFAULT_TIMEOUT, new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_HALTED, 0));
    }

    private void sendGetStatusCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.GET_STATUS));
        System.out.println("GET_STATUS sent");
        waitForResponse(byteToString(commandToBeSent), DEFAULT_TIMEOUT, new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.GOT_STATUS, 0));
    }

    private static final int DEFAULT_TIMEOUT = 5000;

    private void waitForResponse(String commandToWaitFor, int timeOut, FromArduinoMessageEvent eventToSend) {
        Service waitForStopCommand = new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {

                        long timeoutTime = System.currentTimeMillis() + timeOut;
                        boolean gotResponse = false;

                        while (System.currentTimeMillis() < timeoutTime & !gotResponse) {
                            String message = serialCommHandler.getMessageFromReceiveQueue();
                            if (message != null) {
                                if (message.equals(commandToWaitFor)) {
                                    gotResponse = true;
                                    System.out.println("Got response :" + commandToWaitFor);
                                    eventBus.post(eventToSend);
                                }
                            }
                            Thread.sleep(100);

                        }
                        if (!gotResponse) {
                            System.out.println("Timeout");
                        }
                        return null;
                    }
                };

            }
        };
        waitForStopCommand.start();
    }

    private void initCommandSender() {
        Service commandSenderService = new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    char responseToWaitFor;

                    @Override
                    protected Void call() throws Exception {
                        while (threadRun) {
                            System.out.println("currentCommState :" + currentCommState);
                            switch (currentCommState) {
                                case StartingUp:
                                    long now = System.currentTimeMillis();
                                    if (now > timeToGetFirstStatus) {
                                        currentCommState = CommState.Idle;
                                    }
                                    break;

                                case Idle:
                                    CommandToDivider command = commandSendQueue.poll();
                                    if (command != null) {
                                        System.out.println("Sending command :" + command.getCommandChar());
                                        serialCommHandler.sendCommand(command.getCommandChar());
                                    }
                                    break;

                                default:
                                    break;
                            }
                            sleep(LOOP_TIME);
                        }
                        return null;
                    }
                };
            }
        };
        System.out.println("Starting command sender");
        commandSenderService.start();
    }
    private static final int LOOP_TIME = 1000;

    private void initMessageReceiver() {
        Service messageReceiverService = new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        while (threadRun) {

                            String message = serialCommHandler.getMessageFromReceiveQueue();
                            if (message != null) {
                                System.out.println("Message " + message);
                                checkMessage(message);
                            }
                            sleep(LOOP_TIME);
                        }

                        return null;
                    }

                    private void checkMessage(String message) {
                        System.out.println("CheckMessage :" + message);
                        if (message.equals("R")) {
                            // Response to R command. Throw away and set status to running
                            dividerStatus = DividerStatus.RunningProgram;
                            System.out.println("dividerStatus = Running");
                        } else if (message.equals("Q")) {
                            // Response to Q command
                            dividerStatus = DividerStatus.WaitingForCommand;
                            System.out.println("dividerStatus = WaitingForCommand");
                        } else if ( message.startsWith("S")) {
                            if ( message.length() == 2 )
                                if ( message.endsWith("0")) eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_HALTED, 0));
                                else if ( message.endsWith("3"))
                                    eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_RUNNING, 0));
                        }


                    }

                };
            }
        };
    System.out.println ("Starting message receiver");
    messageReceiverService.start ();
}

public DividerStatus getDividerStatus() {

        return dividerStatus;
    }

    void stopThreads() {
        threadRun = false;
    }

}
