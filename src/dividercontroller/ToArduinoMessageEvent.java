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
class ToArduinoMessageEvent {

    public enum Command {
        DOWNLOAD_FROM_PC,
        UPLOAD_TO_PC,
        RUN_PROGRAM,
        QUIT_PROGRAM,
        STEP_POSITIVE,
        STEP_NEGATIVE,
        SET_INC_MODE,
        POSITION_TO,
        GET_CURRENT_POSITION,
        GET_STATUS,
        ZERO_POSITION,
        GET_VERSION
    }
    
    private Command command;
    private double value;

    public ToArduinoMessageEvent() {
    }

    public ToArduinoMessageEvent(Command command, double value) {
        this.command = command;
        this.value = value;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

}
