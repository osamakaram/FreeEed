/*
 *
 * Copyright SHMsoft, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.freeeed.ui;

import org.freeeed.main.FreeEedMain;
import org.freeeed.util.LogFactory;

import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;

/**
 *
 * @author mark
 */
public class UtilUI {

    private final static java.util.logging.Logger LOGGER = LogFactory.getLogger(UtilUI.class.getName());

    public static void openBrowser(Component parent, String url) {
        boolean success = false;
        try {
            //if (Desktop.isDesktopSupported()) {
            Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                URI uri = new URI(url);
                desktop.browse(uri);
                success = true;
            }
            //}
        } catch (URISyntaxException | IOException e) {
            success = false;
        }
        if (!success) {
            JOptionPane.showMessageDialog(parent, "Can't open the browser - just go to\n" + url);
        }
    }

    public static void openImage(Component parent, String filePath) {
        try {
            Desktop desktop = java.awt.Desktop.getDesktop();
            desktop.open(new File(filePath));
        } catch (Exception e) {
            LOGGER.severe("Error opening image: " + e.getMessage());
        }

    }

    /**
     * Open the user's default email client with a pre-filled message. The user
     * remains in full control - nothing is sent until they press Send. No data
     * leaves the machine silently. Falls back to showing the address if no mail
     * client is available.
     *
     * @param parent  parent component for fallback dialogs
     * @param to      recipient address
     * @param subject email subject (will be URL-encoded)
     * @param body    email body (will be URL-encoded)
     */
    public static void openMailClient(Component parent, String to, String subject, String body) {
        String mailto = "mailto:" + to
                + "?subject=" + encodeMailParam(subject)
                + "&body=" + encodeMailParam(body);
        try {
            Desktop desktop = java.awt.Desktop.getDesktop();
            URI uri = new URI(mailto);
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                desktop.mail(uri);
                return;
            }
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
                return;
            }
        } catch (URISyntaxException | IOException e) {
            LOGGER.warning("Could not open mail client: " + e.getMessage());
        }
        JOptionPane.showMessageDialog(parent,
                "Could not open your email client.\nPlease email us directly at:\n" + to);
    }

    /**
     * Encode a mailto query parameter. URLEncoder targets HTML forms, so we
     * convert '+' back to %20 to keep spaces correct in the mailto scheme.
     */
    private static String encodeMailParam(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }
}
