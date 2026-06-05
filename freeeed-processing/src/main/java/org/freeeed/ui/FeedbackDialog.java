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
package org.freeeed.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

import org.freeeed.main.ParameterProcessing;
import org.freeeed.main.Version;
import org.freeeed.services.Settings;

/**
 * "Send feedback / request support" dialog. The lowest-friction way for a user
 * to reach us (FreeEed issue #549). We open the user's own email client with a
 * pre-filled message - nothing is transmitted until they press Send, and no
 * case data or document content is ever included.
 *
 * @author mark
 */
public class FeedbackDialog extends JDialog {

    private JTextArea messageArea;

    public FeedbackDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Send feedback / request support");
        setLocationRelativeTo(parent);

        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        getRootPane().getActionMap().put(cancelName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Tell us what's working - or what isn't");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize() + 2f));

        JTextArea intro = new JTextArea(
                "We read every message. Your note opens in your own email client, so "
                + "nothing is sent until you press Send. Please don't paste case data or "
                + "document content - just tell us what you need.");
        intro.setEditable(false);
        intro.setOpaque(false);
        intro.setLineWrap(true);
        intro.setWrapStyleWord(true);
        intro.setFont(UIManager.getFont("Label.font"));
        intro.setColumns(44);
        intro.setRows(3);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(heading, BorderLayout.NORTH);
        north.add(intro, BorderLayout.CENTER);

        messageArea = new JTextArea(10, 44);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(messageArea);

        JButton sendButton = new JButton("Compose email");
        sendButton.addActionListener(e -> send());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(sendButton);

        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        south.add(cancelButton);
        south.add(sendButton);

        content.add(north, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setMinimumSize(getSize());
    }

    private void send() {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please type your message first.",
                    "Nothing to send", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Settings settings = Settings.getSettings();
        String who = settings.getUserName();
        String subject = "FreeEed feedback"
                + (who != null && !who.isEmpty() ? " from " + who : "");
        String body = message + "\n\n"
                + "--\n"
                + "Sent from FreeEed\n"
                + "Version: " + Version.getVersionAndBuild() + "\n"
                + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n"
                + (who != null && !who.isEmpty() ? "Name: " + who + "\n" : "")
                + (settings.getUserEmail() != null && !settings.getUserEmail().isEmpty()
                        ? "Email: " + settings.getUserEmail() + "\n" : "");

        UtilUI.openMailClient(this, ParameterProcessing.SUPPORT_EMAIL, subject, body);
        dispose();
    }
}
