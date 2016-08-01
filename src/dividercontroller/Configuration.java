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

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
public class Configuration {
    private final String CONFIG_NAME = "se.mecona.dividerController";
    private final String DEFAULT_COMPORT = "COM3";
    private final String COMPORT_KEY = "ComPort";
    
    private final Preferences preferences;
    private final String comPort;

    private static final Configuration instance = new Configuration();
    
    private Configuration() {
        preferences = Preferences.userRoot();
        comPort = preferences.get(COMPORT_KEY, DEFAULT_COMPORT);
    }
    
    public static Configuration getConfiguration() {
        return instance;
    }
    
    public String getComport() {
        return comPort;
    }
}
