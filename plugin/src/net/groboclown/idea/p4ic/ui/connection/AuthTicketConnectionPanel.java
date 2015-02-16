/*
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
package net.groboclown.idea.p4ic.ui.connection;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ResourceBundle;

public class AuthTicketConnectionPanel implements ConnectionPanel {
    private JTextField myPort;
    private JTextField myUsername;
    private TextFieldWithBrowseButton myAuthTicket;
    private JPanel myRootPanel;
    private JLabel myAuthTicketMessage;

    public AuthTicketConnectionPanel() {
        myAuthTicket.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateAuthTicket();
            }
        });
    }

    @Override
    public boolean isModified(@NotNull P4Config config) {
        if (P4ConfigUtil.isPortModified(config, myPort.getText())) {
            return true;
        }
        if (config.getUsername() == null) {
            if (getUsername() != null) {
                return true;
            }
        } else {
            if (!config.getUsername().equals(getUsername())) {
                return true;
            }
        }
        if (config.getAuthTicketPath() == null) {
            if (getAuthTicket() != null) {
                return true;
            }
        } else {
            String ticket = getAuthTicket();
            if (ticket == null) {
                return true;
            }
            if (!FileUtil.filesEqual(new File(ticket), new File(config.getAuthTicketPath()))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return P4Bundle.message("configuration.connection-choice.picker.auth-ticket");
    }

    @Override
    public String getDescription() {
        return P4Bundle.message("connection.auth-ticket.description");
    }

    @Override
    public P4Config.ConnectionMethod getConnectionMethod() {
        return P4Config.ConnectionMethod.AUTH_TICKET;
    }

    @Override
    public void loadSettingsIntoGUI(@NotNull P4Config config) {
        myUsername.setText(config.getUsername());
        myPort.setText(P4ConfigUtil.toFullPort(config.getProtocol(), config.getPort()));
        setAuthTicket(config.getAuthTicketPath());
    }

    @Override
    public void saveSettingsToConfig(@NotNull ManualP4Config config) {
        config.setUsername(getUsername());
        config.setProtocol(P4ConfigUtil.getProtocolFromPort(myPort.getText()));
        config.setPort(P4ConfigUtil.getSimplePortFromPort(myPort.getText()));
        config.setAuthTicketPath(getAuthTicket());
    }

    /*
    @Nullable
    @Override
    public P4Config loadChildConfig(@NotNull P4Config config) throws P4DisconnectedException {
        return null;
    }
    */


    private void setAuthTicket(@Nullable String ticket) {
        if (ticket == null || ticket.length() <= 0) {
            ticket = P4ConfigUtil.getDefaultTicketFile().getAbsolutePath();
        }
        myAuthTicket.setText(ticket);
        validateAuthTicket();
    }


    @Nullable
    private String getAuthTicket() {
        String ticket = myAuthTicket.getText();
        if (ticket == null || ticket.length() <= 0) {
            return null;
        }
        return ticket;
    }


    @Nullable
    private String getUsername() {
        String ret = myUsername.getText();
        if (ret == null || ret.length() <= 0) {
            return null;
        }
        return ret.trim();
    }


    private void validateAuthTicket() {
        String ticket = getAuthTicket();
        boolean existingMessage = myAuthTicketMessage.getText().length() > 0;
        boolean givenMessage = false;
        if (ticket == null) {
            myAuthTicketMessage.setText(P4Bundle.message("configuration.authticket.none"));
            givenMessage = true;
        } else {
            File f = new File(ticket);
            if (f.exists()) {
                if (f.isDirectory()) {
                    myAuthTicketMessage.setText(P4Bundle.message("configuration.authticket.is-dir"));
                    givenMessage = true;
                } else if (!f.canRead()) {
                    myAuthTicketMessage.setText(P4Bundle.message("configuration.authticket.cant-read"));
                    givenMessage = true;
                }
            } else {
                myAuthTicketMessage.setText(P4Bundle.message("configuration.authticket.not-exist"));
                givenMessage = true;
            }
        }

        if (!givenMessage) {
            myAuthTicketMessage.setText("");
        }
        if (givenMessage != existingMessage) {
            myAuthTicketMessage.setVisible(givenMessage);
            myRootPanel.validate();
        }
    }

}
