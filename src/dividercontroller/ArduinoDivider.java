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
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.lang.Thread.sleep;
import javafx.application.Platform;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 *
 * This class handles the arduino divider unit. It has different states as
 * follows
 *
 * State 0 : Waiting for commands from pc 
 * State 1 : Loading divider program from pc
 * State 2 : Sending divider program to pc 
 * State 3 : Running program
 *
 * For commands executed during state 0 the process is as following
 *
 * Send the command and possible values to Arduino
 *
 * The possible commands are: 
 * D Download from PC. D followed by the program and then eof (27) 
 * U Upload to PC. Sends the program followed by eof and "Upload finished" 
 * R Start automatic execution of program. Sends "R" 
 * + Jog 1 step positive. Responds with "+" and "A" followed by the angle 
 * - Jog 1 step negative. Responds with "-" and "A" followed by the angle 
 * P angle Positions to angle. Responds with "Pangle" and then "Aangle" 
 * ? Request current position.  * Responds with "Aangle" 
 * S Request current status (0 or 3). Response "S0" or "S3" 
 * Z Set current position to zero. Response "Z" and then "A0.00" 
 * V Request firmware version. Sends a version string.
 *
 * All the commands is acknowledged with the command as above and all responses
 * is ended with an ETB character (23);
 *
 * This is handled in this class with a state machine
 * 
 */
public class ArduinoDivider {

    private final SerialCommHandler serialCommHandler;

    private final EventBus eventBus;
    private final long DELAY_BEFORE_GETTING_FIRST_STATUS = 3000;

    /**
     * The different states of the state machine
     */
    private enum CommState {
        StartingUp,
        Idle,
        Sending,
        DownloadProgramToArduino,
        UploadProgramToPc
    };

    // The current state
    private CommState currentCommState = CommState.StartingUp;

    // Queue for commands to be sent to divider
    private final ConcurrentLinkedQueue<CommandToDivider> commandSendQueue = new ConcurrentLinkedQueue<>();

    public enum DividerStatus {
        Unknown,
        WaitingForCommand,
        RunningProgram,
        UploadToPC
    };

    private DividerStatus dividerStatus = DividerStatus.Unknown;

    private long timeToGetFirstStatus;

    private String programToDownload;

    public ArduinoDivider(EventBus eventBus) {
        serialCommHandler = new SerialCommHandler();
        //sendGetStatusCommand();
        // initCommandSender();
        // initMessageReceiver();
        this.eventBus = eventBus;
        eventBus.register(this);
    }

    public void startDivider() {
        initMessageReceiverTask();
        initSerialSendTask();
        startSerial();
    }

    void startSerial() {
        serialCommHandler.startReader();
        timeToGetFirstStatus = System.currentTimeMillis() + DELAY_BEFORE_GETTING_FIRST_STATUS;
    }

    @Subscribe
    private void handleDownloadProgramMessage(DownloadProgramMessage downloadProgramMessage) {
        programToDownload = downloadProgramMessage.getDividerProgram();
        //currentCommState = CommState.DownloadProgramToArduino;
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.DOWNLOAD_PROGRAM));
    }

    @Subscribe
    private void handleEventBusEvent(ToArduinoMessageEvent event) {
        switch (event.getCommand()) {
            case QUIT_PROGRAM:
                sendStopCommand();
                break;
            case RUN_PROGRAM:
                sendRunCommand();
                break;
            case GET_STATUS:
                sendGetStatusCommand();
                break;
            case ZERO_POSITION:
                sendSetZeroPosition();
                break;
            case POSITION_TO:
                sendPositionTo(event.getValue());
                break;
            case SET_INC_MODE:
                sendSetIncremental(event.getValue());
                break;
            case STEP_NEGATIVE:
                sendStepNegativeCommand();
                break;
            case STEP_POSITIVE:
                sendStepPositiveCommand();
                break;
            case UPLOAD_TO_PC:
                sendUploadToPCCommand();
            default:
                break;
        }
    }

    private void sendStopCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.STOP_RUNNING));
        Utils.debugOutput("Stop Running sent", 2);
    }

    private void sendRunCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.RUN_PROGRAM));
        Utils.debugOutput("Run program sent", 2);
    }

    private void sendGetStatusCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.GET_STATUS));
        Utils.debugOutput("Get status sent", 2);
    }

    private void sendSetZeroPosition() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.ZERO_POSITION));
        Utils.debugOutput("Set Zero sent", 2);
    }

    private void sendSetIncremental(double value) {
        if ( value == 0 ) {
            commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.SET_ABSOLUTE));
        } else {
            commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.SET_INCREMENTAL));
        }
    }
    
    private void sendPositionTo(double position) {
        CommandToDivider commandToDivider = new CommandToDivider(CommandToDivider.DividerCommand.POSITION_TO);
        commandToDivider.setValue(position);
        commandSendQueue.add(commandToDivider);
        Utils.debugOutput("Position to sent" + position, 2);
    }

    private void sendStepNegativeCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.STEP_MINUS));
        Utils.debugOutput("Step negative sent", 2);
    }

    private void sendStepPositiveCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.STEP_PLUS));
        Utils.debugOutput("Step positive sent", 2);
    }

    private void sendUploadToPCCommand() {
        commandSendQueue.add(new CommandToDivider(CommandToDivider.DividerCommand.UPLOAD_PROGRAM));
        Utils.debugOutput("Upload sent", 2);
        currentCommState = CommState.UploadProgramToPc;
    }

    private static final int LOOP_TIME = 500;

    private SerialSendTask serialSendTask;
    private boolean stopSerialSendTask;
    private volatile boolean messageReceiverTaskStopped = false;
    private volatile boolean serialSendTaskStopped = false;

    private void initSerialSendTask() {
        serialSendTask = new SerialSendTask();
        stopSerialSendTask = false;
        Thread sST = new Thread(serialSendTask);
        sST.setDaemon(true);
        sST.start();
    }

    private class SerialSendTask implements Runnable {

        private long nextTimeToAskForAngle;
        private long nextTimeToAskForStatus;

        @Override
        public void run() {
            int numTimesInUploadState = 0;
            long uploadTimeOutTime = 0;
            int numTimesInDownloadState = 0;
            long downloadTimeOutTime = 0;
            CommState lastCommState = CommState.Idle;
            while (!stopSerialSendTask) {
                if (currentCommState != lastCommState) {
                    Utils.debugOutput("currentCommState :" + currentCommState, 2);
                    lastCommState = currentCommState;
                }
                long now = System.currentTimeMillis();
                switch (currentCommState) {
                    case StartingUp:
                        if (now > timeToGetFirstStatus) {
                            currentCommState = CommState.Idle;
                            nextTimeToAskForAngle = now + 3000;
                            nextTimeToAskForStatus = now + 1000;
                        }
                        break;

                    case Idle:
                        CommandToDivider command = commandSendQueue.poll();
                        if (command != null) {
                            nextTimeToAskForAngle += 2000;
                            Utils.debugOutput("Sending command :" + command.getCommandChar(), 2);
                            serialCommHandler.sendCommand(command.getCommandChar());
                            if (command.getCommand() == CommandToDivider.DividerCommand.POSITION_TO) {
                                Utils.debugOutput("Sending position value " + command.getValue(), 2);
                                serialCommHandler.sendPosition(command.getValue());
                            }
                            if (command.getCommand() == CommandToDivider.DividerCommand.DOWNLOAD_PROGRAM) {
                                currentCommState = CommState.DownloadProgramToArduino;
                                downloadTimeOutTime = now + 5000;
                            }
                            command = null;
                        } else if (now > nextTimeToAskForAngle) {
                            serialCommHandler.sendCommand(new CommandToDivider(CommandToDivider.DividerCommand.GET_ANGLE).getCommandChar());
                            nextTimeToAskForAngle = now + 10000;
                        } else if (now > nextTimeToAskForStatus) {
                            serialCommHandler.sendCommand(new CommandToDivider(CommandToDivider.DividerCommand.GET_STATUS).getCommandChar());
                            nextTimeToAskForStatus = now + 20000;
                        }
                        numTimesInDownloadState = 0;
                        break;

                    case UploadProgramToPc:
                        if (numTimesInUploadState == 0) {
                            uploadTimeOutTime = now + UP_AND_DOWNLOAD_TIMEOUT;
                            numTimesInUploadState++;
                        } else if (numTimesInUploadState < 5) {
                            command = commandSendQueue.poll();  // should be upload command.
                            if (command != null) {
                                serialCommHandler.sendCommand(command.getCommandChar());
                                dividerStatus = DividerStatus.UploadToPC;
                            }
                            numTimesInUploadState++;
                        } else if (now > uploadTimeOutTime) {
                            currentCommState = CommState.Idle;
                            dividerStatus = DividerStatus.WaitingForCommand;
                            numTimesInUploadState = 0;
                        }
                        break;

                    case DownloadProgramToArduino:
                        if (numTimesInDownloadState == 0) {
                            downloadTimeOutTime = now + UP_AND_DOWNLOAD_TIMEOUT;
                            numTimesInDownloadState++;
                        } else if (numTimesInDownloadState == 1) {
                            serialCommHandler.sendProgram(programToDownload);
                            numTimesInDownloadState++;
                        } else if (now > downloadTimeOutTime) {
                            currentCommState = CommState.Idle;
                            dividerStatus = DividerStatus.WaitingForCommand;
                            numTimesInDownloadState = 0;
                        }
                        break;

                    default:
                        break;
                }
                try {
                    sleep(LOOP_TIME);
                } catch (InterruptedException ex) {

                }
            }
            serialSendTaskStopped = true;
        }
        private static final int UP_AND_DOWNLOAD_TIMEOUT = 20000;
    }

    private MessageReceiverTask messageReceiverTask;
    private boolean stopMessageReceiverTask;

    private void initMessageReceiverTask() {
        messageReceiverTask = new MessageReceiverTask();
        stopMessageReceiverTask = false;
        Thread mRT = new Thread(messageReceiverTask);
        mRT.setDaemon(true);
        mRT.start();
    }

    private class MessageReceiverTask implements Runnable {

        @Override
        public void run() {
            String previousMessage = null;
            while (!stopMessageReceiverTask) {
                String message = serialCommHandler.getMessageFromReceiveQueue();
                eventBus.post( new ArduinoStatusMessageEvent(message));

                if (message != null) {
                    if (currentCommState == CommState.UploadProgramToPc) {
                        Utils.debugOutput("Uploadmessage is : " + message, 2);
                        if (message.contains("Upload finished")) {
                            Utils.debugOutput("Previous message :" + previousMessage, 2);
                            sendMessageToGui(previousMessage);
                            Utils.debugOutput("Upload completed :" + message, 2);
                            currentCommState = CommState.Idle;
                        }
                        previousMessage = message;
                    } else if (currentCommState == CommState.DownloadProgramToArduino) {
                        if (message.contains("Download finished")) {
                            Utils.debugOutput(message, 2);
                            currentCommState = CommState.Idle;
                        }
                    } else {
                        checkMessage(message);
                    }

                }
                try {
                    sleep(LOOP_TIME);
                } catch (InterruptedException ex) {

                }
                //System.out.println("MessageReceiverTask is running");
            }
            messageReceiverTaskStopped = true;

        }

        private void checkMessage(String message) {
            //System.out.println("CheckMessage :" + message);
           
            if (message.equals("R")) {
                // Response to R command. Throw away and set status to running
                dividerStatus = DividerStatus.RunningProgram;
                Utils.debugOutput("dividerStatus = Running", 2);
                eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_RUNNING, 0));
            } else if (message.equals("Q")) {
                // Response to Q command
                dividerStatus = DividerStatus.WaitingForCommand;
                Utils.debugOutput("dividerStatus = WaitingForCommand", 2);
                eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_HALTED, 0));
            } else if (message.startsWith("S")) {
                if (message.length() == 2) {
                    if (message.endsWith("0")) {
                        eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_HALTED, 0));
                    } else if (message.endsWith("3")) {
                        dividerStatus = DividerStatus.RunningProgram;
                        Utils.debugOutput("dividerStatus = Running", 2);
                        eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.PROGRAM_IS_RUNNING, 0));
                    }
                }
            } else if (message.startsWith("A")) {
                try {
                    double position = getPositionFromMessage(message);
                    eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.GOT_POSITION, position));
                } catch (NumberFormatException ex) {

                }

            } else if ( message.startsWith("I")) {
                if (message.length() == 2) {
                    if (message.endsWith("0")) {
                        eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.INCREMENTAL_IS_OFF, 0));
                    } else if (message.endsWith("1")) {
                        eventBus.post(new FromArduinoMessageEvent(FromArduinoMessageEvent.MessageType.INCREMENTAL_IS_ON, 0));
                    }
                }
            }

        }

        private void sendMessageToGui(String message) {
            Platform.runLater(() -> {
                eventBus.post(new UploadedProgramMessage(message));
            });
        }

        private double getPositionFromMessage(String message) {
            String angularValue = message.substring(1);
            double position = Double.parseDouble(angularValue);
            return position;

        }

    }

    public DividerStatus getDividerStatus() {

        return dividerStatus;
    }

    void stopThreads() {
        //messageReceiverService.cancel();
        //commandSenderService.cancel();
        serialCommHandler.stopReader();
        stopMessageReceiverTask = true;
        stopSerialSendTask = true;
        while (!messageReceiverTaskStopped) {
            // Wait for task stop;
        }

        while (!serialSendTaskStopped) {
            // Wait for task stop;
        }
    }

}
