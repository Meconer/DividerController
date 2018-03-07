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

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
class FromArduinoMessageEvent {

    MessageType getCommand() {
        return messageType;
    }

    public enum MessageType {
        DOWNLOAD_FROM_PC,
        UPLOAD_TO_PC,
        PROGRAM_IS_RUNNING,
        PROGRAM_IS_HALTED,
        STEP_POSITIVE,
        STEP_NEGATIVE,
        GOT_POSITION,
        GET_CURRENT_POSITION,
        GOT_STATUS,
        ZERO_POSITION,
        GET_VERSION,
        COMMUNICATION_STARTED,
        INCREMENTAL_IS_OFF,
        INCREMENTAL_IS_ON
    }
    
    private MessageType messageType;
    private double value;

    public FromArduinoMessageEvent() {
    }

    public FromArduinoMessageEvent(MessageType command, double value) {
        this.messageType = command;
        this.value = value;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

}
