/*
 * Copyright (C) 2016 matsa.
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

import java.util.prefs.Preferences;
import jssc.SerialPort;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class Configuration {
    private final String CONFIG_NAME = "se.mecona.dividerController";
    private final String DEFAULT_COMPORT = "COM3";
    private final String COMPORT_KEY = "CommPort";
    private final String COMM_BAUDRATE_KEY = "CommBaudRate";
    private final int DEFAULT_COMM_BAUDRATE = SerialPort.BAUDRATE_115200;
    private final String COMM_DATABITS_KEY = "CommDataBits";
    private final int DEFAULT_COMM_DATABITS = SerialPort.DATABITS_8;
    private final String COMM_STOPBITS_KEY = "CommStopBits";
    private final int DEFAULT_COMM_STOPBITS = SerialPort.STOPBITS_1;
    private final String COMM_PARITY_KEY = "CommParity";
    private final int DEFAULT_COMM_PARITY = SerialPort.PARITY_EVEN;

    private final Preferences preferences;
    private final String commPort;
    private final int commBaudRate;
    private final int commDataBits;
    private final int commStopBits;
    private final int commParity;

    private static final Configuration INSTANCE = new Configuration();
    
    private Configuration() {
        preferences = Preferences.userRoot();
        commPort = preferences.get(COMPORT_KEY, DEFAULT_COMPORT);
        commBaudRate = preferences.getInt(COMM_BAUDRATE_KEY, DEFAULT_COMM_BAUDRATE);
        commDataBits = preferences.getInt(COMM_DATABITS_KEY, DEFAULT_COMM_DATABITS);
        commStopBits = preferences.getInt(COMM_STOPBITS_KEY, DEFAULT_COMM_STOPBITS);
        commParity = preferences.getInt(COMM_PARITY_KEY, DEFAULT_COMM_PARITY);
    }
    
    public static Configuration getConfiguration() {
        return INSTANCE;
    }
    
    public String getComport() {
        return commPort;
    }

    public String getCommPort() {
        return commPort;
    }

    public int getCommBaudRate() {
        return commBaudRate;
    }

    public int getCommDataBits() {
        return commDataBits;
    }

    public int getCommStopBits() {
        return commStopBits;
    }

    public int getCommParity() {
        return commParity;
    }
    
    
}
