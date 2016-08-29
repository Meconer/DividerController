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

import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class SerialCommHandler implements SerialPortEventListener {

    SerialPort serialPort;
    private final int SIZE_OF_RECEIVE_BUFFER = 50;
    private int numBytesInBuffer = 0;

    private final byte ETB_CHAR = 23;
    private final byte EOF_CHAR = 27;
    private static final int LF_CHAR = 10;
    private static final int CR_CHAR = 13;

    private final byte[] receiveBuffer = new byte[SIZE_OF_RECEIVE_BUFFER];
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue();

    void sendProgram(String programToDownload) {
        try {
            serialPort.writeString(programToDownload);
            serialPort.writeByte(EOF_CHAR);
        } catch (SerialPortException ex) {
            System.out.println("SerialPortException " + ex.getMessage());
        }
    }

    void stopReader() {
        try {
            serialPort.removeEventListener();
            serialPort.closePort();
        } catch (SerialPortException ex) {
            System.out.println("SerialPortException " + ex.getMessage());
        }
    }

    private enum CommStatus {
        UP, DOWN
    };

    private CommStatus commStatus = CommStatus.DOWN;

    public SerialCommHandler() {

        // Init serial comm parameters.
        initSerialComm();
    }

    public void startReader() {         // Start serial communication thread
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

    // Start up serial receiver event listener.
    private void initSerialReader() {
        try {
            serialPort.addEventListener(this);
        } catch (SerialPortException ex) {
            System.out.println("SerialPortException " + ex.getMessage());
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        //Object type SerialPortEvent carries information about which event occurred and a value.
        //For example, if the data came a method event.getEventValue() returns us the number of bytes in the input buffer.

        if (event.isRXCHAR()) {  // If there are characters recieved
            try {
                // Get the characters read and add them to our serial buffer. When an etbChar (chr 23) comes we will
                // make a string of it and put it in a queue.
                while (serialPort.getInputBufferBytesCount() > 0) {
                    byte readByte = serialPort.readBytes(1)[0];
                    //System.out.println("Received :" + String.valueOf((char) readByte) + readByte);

                    // etb received. Convert the buffer to a string and put it in the answer queue.
                    if (readByte == ETB_CHAR) {
                        if (numBytesInBuffer > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < numBytesInBuffer; i++) {
                                byte rb = receiveBuffer[i];
                                switch (rb) {
                                    case EOF_CHAR:
                                        break;
                                    case CR_CHAR:
                                        break;
                                    case LF_CHAR:
                                        break;
                                    case ETB_CHAR:
                                        break;
                                    default:
                                        sb.append((char) receiveBuffer[i]);
                                        break;
                                }
                            }
                            messageQueue.add(sb.toString());
                            numBytesInBuffer = 0;
                            System.out.println("Message added: " + sb);
                        }
                    } else {
                        receiveBuffer[numBytesInBuffer] = readByte;
                        numBytesInBuffer++;
                        if ( numBytesInBuffer > SIZE_OF_RECEIVE_BUFFER ) {
                            System.out.println("Buffer Overrun!!");
                            numBytesInBuffer=0;
                        }
                    }
                }

            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
    }

    public void sendCommand(char commandChar) {
        if (commStatus == CommStatus.UP) {
            try {
                serialPort.writeByte((byte) commandChar);
            } catch (SerialPortException ex) {
                System.out.println("serialPort.writeString exception " + ex.getMessage());
            }
        }
    }

    public void sendPosition(double position) {
        if (commStatus == CommStatus.UP) {
            DecimalFormat df = new DecimalFormat("0.00");
            String textToSend = df.format(position).replaceAll(",", ".");
            System.out.println("Serial sending :"+textToSend);
            try {
                serialPort.writeString(textToSend);
            } catch (SerialPortException ex) {
                System.out.println("serialPort.writeString exception " + ex.getMessage());
            }
        }
    }

    public String getMessageFromReceiveQueue() {
        return messageQueue.poll();
    }

}
