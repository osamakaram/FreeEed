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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

import org.freeeed.main.ParameterProcessing;
import org.freeeed.main.Version;
import org.freeeed.services.Settings;

/**
 * Free, one-time user registration. This is identification, not licensing:
 * FreeEed stays free and open, and nothing here unlocks or locks any feature.
 *
 * The dialog is "soft-required" - it re-appears on each launch until the user
 * either registers or explicitly clicks "Don't ask again". It never hard-blocks
 * the application, in keeping with the "no dark patterns" guardrail of the
 * registration feature (FreeEed issue #549).
 *
 * On registration we open the user's own email client with a pre-filled message
 * so they stay in full control - no data is transmitted silently.
 *
 * @author mark
 */
public class UserRegistrationDialog extends JDialog {

    private JTextField nameField;
    private JTextField companyField;
    private JTextField emailField;

    public UserRegistrationDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Stay in touch with FreeEed");
        setLocationRelativeTo(parent);

        // Close (= remind me later) when Esc is pressed
        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        getRootPane().getActionMap().put(cancelName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remindLater();
            }
        });
    }

    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        content.add(buildMessagePanel(), BorderLayout.NORTH);
        content.add(buildFormPanel(), BorderLayout.CENTER);
        content.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setMinimumSize(getSize());
    }

    private JComponent buildMessagePanel() {
        JLabel heading = new JLabel("Help us help you");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize() + 4f));

        JTextArea message = new JTextArea(
                "Right now we have no way to reach you. Register (it's free, and always "
                + "will be) so we can send you bug fixes, security patches, and the "
                + "occasional eDiscovery tip - and so you can reach us back.\n\n"
                + "This is identification, not licensing. The open-source FreeEed stays "
                + "free and open; registering unlocks nothing and locks nothing. We never "
                + "collect your case data. The paid complete build and AI add-ons are "
                + "optional - they fund this work, they don't gate the core.\n\n"
                + "We care about our users, not profit - or at least we want the chance "
                + "to show it.");
        message.setEditable(false);
        message.setOpaque(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(UIManager.getFont("Label.font"));
        message.setColumns(42);
        message.setRows(9);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(message, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        nameField = new JTextField(24);
        companyField = new JTextField(24);
        emailField = new JTextField(24);

        // Pre-fill if the user partially registered before
        Settings settings = Settings.getSettings();
        nameField.setText(settings.getUserName());
        companyField.setText(settings.getUserCompany());
        emailField.setText(settings.getUserEmail());

        addRow(form, c, 0, "Name:", nameField);
        addRow(form, c, 1, "Company:", companyField);
        addRow(form, c, 2, "Email:", emailField);

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    private JComponent buildButtonPanel() {
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> register());

        JButton remindButton = new JButton("Remind me later");
        remindButton.addActionListener(e -> remindLater());

        JButton declineButton = new JButton("Don't ask again");
        declineButton.addActionListener(e -> decline());

        getRootPane().setDefaultButton(registerButton);

        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        panel.add(declineButton);
        panel.add(remindButton);
        panel.add(registerButton);
        return panel;
    }

    private void register() {
        String name = nameField.getText().trim();
        String company = companyField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter at least your name and email.",
                    "A little more, please", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                    "That email address doesn't look right. Please check it.",
                    "Check email", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Settings settings = Settings.getSettings();
        settings.setUserName(name);
        settings.setUserCompany(company);
        settings.setUserEmail(email);
        settings.setRegistrationStatus(ParameterProcessing.REGISTRATION_REGISTERED);
        try {
            settings.save();
        } catch (Exception ex) {
            // Non-fatal: we still proceed and still open the email client.
        }

        String subject = "FreeEed registration: " + name;
        String body = "Hi FreeEed team,\n\n"
                + "I'd like to register and stay in touch.\n\n"
                + "Name: " + name + "\n"
                + "Company: " + company + "\n"
                + "Email: " + email + "\n"
                + "Version: " + Version.getVersionAndBuild() + "\n"
                + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n";
        UtilUI.openMailClient(this, ParameterProcessing.SUPPORT_EMAIL, subject, body);

        JOptionPane.showMessageDialog(this,
                "Thank you! Your email client should be opening with your details.\n"
                + "Just press Send and you'll be on our list for updates.",
                "Welcome aboard", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    /** Skip for now; status stays empty so we ask again next launch. */
    private void remindLater() {
        dispose();
    }

    /** Persist the user's choice never to be asked again. */
    private void decline() {
        Settings settings = Settings.getSettings();
        settings.setRegistrationStatus(ParameterProcessing.REGISTRATION_DECLINED);
        try {
            settings.save();
        } catch (Exception ex) {
            // Non-fatal.
        }
        dispose();
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    /**
     * Show the registration dialog if the user has neither registered nor
     * declined yet. Best-effort: any failure is swallowed so it can never
     * prevent the application from starting.
     *
     * @param parent the main application frame
     */
    public static void promptIfNeeded(Frame parent) {
        try {
            if (Settings.getSettings().needsRegistrationPrompt()) {
                new UserRegistrationDialog(parent, true).setVisible(true);
            }
        } catch (Exception e) {
            // Registration must never block startup.
        }
    }
}
