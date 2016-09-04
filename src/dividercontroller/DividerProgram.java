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

import static dividercontroller.Utils.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.stage.FileChooser;

/**
 *
 * @author Mats Andersson <mats.andersson@mecona.se>
 */
class DividerProgram {

    private String program;
    private String dividerProgramText;

    public DividerProgram(String program) {
        this.program = program;
        errString = "";
    }

    public DividerProgram() {
        errString = "";
        program = "";
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
        Utils.debugOutput("DPT :" + dividerProgramText,2);
        return dividerProgramText;
    }

    public String getSyntaxErrorMessage() {
        return errString;
    }

    public void openFromDisc() {
        FileChooser fc = new FileChooser();
        String initialDirectoryName = Configuration.getConfiguration().getInitialDirectoryName();
        if (initialDirectoryName != null) {
            fc.setInitialDirectory(new File(initialDirectoryName));
        }
        File file = fc.showOpenDialog(null);
        if (file != null) {
            program = readFromFile(file.getAbsolutePath()).replaceAll("\\r", "");

        }
    }

    private String readFromFile(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException ex) {
            return null;
        }
    }

    public String getText() {
        return program;
    }

    void saveToDisc() {
        FileChooser fc = new FileChooser();
        String initialDirectoryName = Configuration.getConfiguration().getInitialDirectoryName();
        if (initialDirectoryName != null) {
            fc.setInitialDirectory(new File(initialDirectoryName));
        }
        File file = fc.showSaveDialog(null);
        if (file != null) {
            saveToFile(file);
        }
    }

    private void saveToFile(File file) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(program);
            fileWriter.close();
        } catch (IOException ex) {
            showError("Kan inte spara filen/n" + ex.getMessage());
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ex) {
                    showError("Kan inte spara filen/n" + ex.getMessage());
                }
            }
        }

    }

}
