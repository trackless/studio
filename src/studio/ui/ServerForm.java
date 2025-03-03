
package studio.ui;

import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.core.AuthenticationManager;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.*;

import static javax.swing.GroupLayout.Alignment.*;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

public class ServerForm extends EscapeDialog {
    private final Server s;

    public ServerForm(Window frame, String title, Server server) {
        super(frame, title);
        s = new Server(server);

        initComponents();

        logicalName.setText(s.getName());
        hostname.setText(s.getHost());
        username.setText(s.getUsername());
        port.setText("" + s.getPort());
        password.setText(s.getPassword());
        jCheckBox2.setSelected(s.getUseTLS());
        DefaultComboBoxModel dcbm = (DefaultComboBoxModel) authenticationMechanism.getModel();
        String[] am;
        am = AuthenticationManager.getInstance().getAuthenticationMechanisms();
        for (String string : am) {
            dcbm.addElement(string);
            if (s.getAuthenticationMechanism().equals(string))
                dcbm.setSelectedItem(string);
        }

        authenticationMechanism.addItemListener(e -> {
            String auth = Objects.requireNonNull(authenticationMechanism.getSelectedItem()).toString();
            Credentials credentials = Config.getInstance().getDefaultCredentials(auth);
            username.setText(credentials.getUsername());
            password.setText(credentials.getPassword());
        });

        logicalName.setToolTipText("The logical name for the server");
        hostname.setToolTipText("The hostname or ip address for the server");
        port.setToolTipText("The port for the server");
        jCheckBox2.setToolTipText("Use TLS for a secure connection");
        username.setToolTipText("The username used to connect to the server");
        password.setToolTipText("The password used to connect to the server");
        authenticationMechanism.setToolTipText("The authentication mechanism to use");

//        testConnection.setSelected(true);

        SampleTextOnBackgroundTextField.setBackground(s.getBackgroundColor());
        SampleTextOnBackgroundTextField.setEditable(false);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                logicalName.requestFocus();
            }
        });
        getRootPane().setDefaultButton(okButton);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    //
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logicalNameLabel = new javax.swing.JLabel();
        logicalName = new javax.swing.JTextField();
        hostnameLabel = new javax.swing.JLabel();
        hostname = new javax.swing.JTextField();
        portLabel = new javax.swing.JLabel();
        port = new javax.swing.JTextField();
        usernameLabel = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        passwordLabel = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        password = new javax.swing.JPasswordField();
        jLabel1 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        EditColorButton = new javax.swing.JButton();
        SampleTextOnBackgroundTextField = new javax.swing.JTextField();
        authenticationMechanism = new javax.swing.JComboBox();
        passwordLabel1 = new javax.swing.JLabel();
        jCheckBox2 = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();

        logicalNameLabel.setText("Name");

        hostnameLabel.setText("Host");

        portLabel.setText("Port");

        usernameLabel.setText("Username");

        passwordLabel.setText("Password");

        okButton.setText("Ok");
        okButton.addActionListener(this::onOk);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(this::onCancel);

        jLabel1.setText("Color");

        EditColorButton.setText("Edit Color");
        EditColorButton.addActionListener(this::onColor);

        SampleTextOnBackgroundTextField.setText("Sample text on background");
        SampleTextOnBackgroundTextField.addActionListener(this::SampleTextOnBackgroundTextFieldActionPerformed);

        passwordLabel1.setText("Auth. Method");

        jLabel2.setText("Use TLS");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(logicalNameLabel)
                                        .addComponent(hostnameLabel)
                                        .addComponent(portLabel)
                                        .addComponent(jLabel2)
                                        .addComponent(usernameLabel)
                                        .addComponent(passwordLabel)
                                        .addComponent(passwordLabel1)
                                        .addComponent(jLabel1))
                                .addPreferredGap(RELATED, 21, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(EditColorButton)
                                                .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(cancelButton)
                                                .addPreferredGap(RELATED)
                                                .addComponent(okButton)
                                                .addGap(6, 6, 6))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(LEADING)
                                                        .addComponent(SampleTextOnBackgroundTextField, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(authenticationMechanism, 0, 418, Short.MAX_VALUE)
                                                        .addComponent(password, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(username, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(jCheckBox2)
                                                        .addComponent(port, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(hostname, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(logicalName, DEFAULT_SIZE, 418, Short.MAX_VALUE))
                                                .addContainerGap())))
                        .addGroup(TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addGroup(TRAILING, layout.createSequentialGroup()
                                                .addContainerGap(45, Short.MAX_VALUE)
                                                .addComponent(jSeparator2, DEFAULT_SIZE, 471, Short.MAX_VALUE))
                                        .addGroup(TRAILING, layout.createSequentialGroup()
                                                .addContainerGap(45, Short.MAX_VALUE)
                                                .addComponent(jSeparator3, DEFAULT_SIZE, 471, Short.MAX_VALUE))
                                        .addGroup(TRAILING, layout.createSequentialGroup()
                                                .addContainerGap(45, Short.MAX_VALUE)
                                                .addComponent(jSeparator1, DEFAULT_SIZE, 471, Short.MAX_VALUE)))
                                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(logicalNameLabel)
                                        .addComponent(logicalName, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addComponent(jSeparator1, PREFERRED_SIZE, 10, PREFERRED_SIZE)
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(hostnameLabel)
                                        .addComponent(hostname, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(portLabel)
                                        .addComponent(port, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(jLabel2, TRAILING)
                                        .addComponent(jCheckBox2, TRAILING))
                                .addPreferredGap(RELATED)
                                .addComponent(jSeparator2, PREFERRED_SIZE, 10, PREFERRED_SIZE)
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(usernameLabel)
                                        .addComponent(username, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE, false)
                                        .addComponent(passwordLabel)
                                        .addComponent(password, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(authenticationMechanism, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                                        .addComponent(passwordLabel1))
                                .addPreferredGap(RELATED)
                                .addComponent(jSeparator3, PREFERRED_SIZE, 10, PREFERRED_SIZE)
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(SampleTextOnBackgroundTextField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(okButton)
                                        .addComponent(cancelButton)
                                        .addComponent(EditColorButton))
                                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void onOk(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onOk
        logicalName.setText(logicalName.getText().trim());
        hostname.setText(hostname.getText().trim());
        username.setText(username.getText().trim());
        port.setText(port.getText().trim());
        password.setText(new String(password.getPassword()).trim());

        if (logicalName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "The server's name cannot be empty",
                    "Studio for kdb+",
                    JOptionPane.ERROR_MESSAGE);
            logicalName.requestFocus();
            return;
        }

        s.setName(logicalName.getText().trim());
        s.setHost(hostname.getText().trim());
        s.setUsername(username.getText().trim());
        if (port.getText().isEmpty())
            s.setPort(0);
        else
            s.setPort(Integer.parseInt(port.getText()));

        s.setPassword(new String(password.getPassword()).trim());
        s.setUseTLS(jCheckBox2.isSelected());
        DefaultComboBoxModel dcbm = (DefaultComboBoxModel) authenticationMechanism.getModel();
        s.setAuthenticationMechanism((String) dcbm.getSelectedItem());
        accept();
    }//GEN-LAST:event_onOk


    public Server getServer() {
        return s;
    }

    private void onCancel(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCancel
        cancel();
    }//GEN-LAST:event_onCancel

    Color c;

    private void onColor(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onColor
        final JColorChooser chooser = new JColorChooser();
        //      chooser.setPreviewPanel(new CustomPane());
        c = SampleTextOnBackgroundTextField.getBackground();

        JDialog dialog = JColorChooser.createDialog(this,
                "Select background color for editor", true, chooser, e -> c = chooser.getColor(), null);

        dialog.setVisible(true);

        SampleTextOnBackgroundTextField.setBackground(c);
        s.setBackgroundColor(c);
    }//GEN-LAST:event_onColor

    private void SampleTextOnBackgroundTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SampleTextOnBackgroundTextFieldActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_SampleTextOnBackgroundTextFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton EditColorButton;
    private javax.swing.JTextField SampleTextOnBackgroundTextField;
    private javax.swing.JComboBox authenticationMechanism;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField hostname;
    private javax.swing.JLabel hostnameLabel;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField logicalName;
    private javax.swing.JLabel logicalNameLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JPasswordField password;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel passwordLabel1;
    private javax.swing.JTextField port;
    private javax.swing.JLabel portLabel;
    private javax.swing.JTextField username;
    private javax.swing.JLabel usernameLabel;
    // End of variables declaration//GEN-END:variables

}
