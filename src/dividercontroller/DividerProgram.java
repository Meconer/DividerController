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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
class DividerProgram {

    private final String program;
    private String dividerProgramText;

    public DividerProgram(String program) {
        this.program = program;
        errString = "";
        dividerProgramText = "";
    }

    private static final String SYNTAX_CHECK_REGEX = "^((B-*\\d+\\.?\\d*)|(M\\d)|(P-*\\d+\\.?\\d*,-*\\d+\\.?\\d*)|R)$";

    public boolean isSyntaxOk() {
        boolean syntaxIsOk = true;
        if (!program.isEmpty()) {
            String text[] = program.split("\\\n");
            for (int i = 0; i < text.length; i++) {
                // first remove all comments and whitespace
                String s = text[i].replaceAll("\\((.*?)\\)|\\s", "");
                s = s.toUpperCase();
                Pattern allowed = Pattern.compile(SYNTAX_CHECK_REGEX);
                Matcher matcher = allowed.matcher(s);
                if (!matcher.find()) {
                    errString += "Syntaxfel i rad: " + text[i] + "\n";
                    syntaxIsOk = false;
                }
            }
        } else {
            errString = "Filen tom.";
            syntaxIsOk = false;
        }
        return syntaxIsOk;
    }
    private String errString;

    String getDownloadToArduinoText() {
        String text[] = program.split("\\\n");
        dividerProgramText = "";
        for (int i = 0; i < text.length; i++) {
            // first remove all comments and whitespace
            String s = text[i].replaceAll("\\((.*?)\\)|\\s", "");
            s = s.toUpperCase();
            dividerProgramText += s;
        }
        System.out.println("DPT :" + dividerProgramText);
        return dividerProgramText;
    }

    String getSyntaxErrorMessage() {
        return errString;
    }

}
