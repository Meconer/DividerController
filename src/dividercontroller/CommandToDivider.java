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
public class CommandToDivider {

    // Divider commands
    public enum DividerCommand {
        DOWNLOAD_PROGRAM('D'),
        UPLOAD_PROGRAM('U'),
        RUN_PROGRAM('R'),
        STEP_PLUS('+'),
        STEP_MINUS('-'),
        POSITION_TO('P'),
        SET_INCREMENTAL('I'),
        SET_ABSOLUTE('A'),
        ZERO_POSITION('Z'),
        GET_STATUS('S'),
        GET_ANGLE('?'),
        STOP_RUNNING('Q'),
        GET_VERSION('V');
        
        private final char commandChar;
        
        DividerCommand(char commandChar) {
            this.commandChar = commandChar;
        }
        
        public char getCommandByte() {
            return commandChar;
        }

    }
    
    private DividerCommand command;
    private double value;

    public char getCommandChar() {
        return command.commandChar;
    }

    public CommandToDivider(DividerCommand command) {
        this.command = command;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public DividerCommand getCommand() {
        return command;
    }
    
    
    
}
