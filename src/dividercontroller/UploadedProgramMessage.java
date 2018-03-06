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
class UploadedProgramMessage {
    private String text;
    private final String END_TEXT = "Upload finished";

    public UploadedProgramMessage(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    String getCleanedUpText() {
        text = text.replaceAll( END_TEXT, "");
        text = text.replaceAll("([MDGBPRF][-\\d\\.]*(,[-\\d\\.]+)?)", "$1\n");
        return text;
    }
    
    
}
