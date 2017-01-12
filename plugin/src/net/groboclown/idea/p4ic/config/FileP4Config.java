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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.server.IServerAddress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Properties;

public class FileP4Config implements P4Config {
    private final File configFile;
    private String clientName;
    private String password;
    private String simplePort;
    private String clientHostname;
    private IServerAddress.Protocol protocol;
    private String tickets;
    private String trust;
    private String user;
    private String ignoreFile;

    @NotNull
    private P4Config.ConnectionMethod connectionMethod;


    public FileP4Config(File configFile) throws IOException {
        this.connectionMethod = P4Config.ConnectionMethod.DEFAULT;
        this.configFile = configFile;
        reloadFile();
    }

    @Override
    public void reload() {
        try {
            reloadFile();
        } catch (IOException e) {
            e.printStackTrace();
            clientName = null;
            password = null;
            simplePort = null;
            protocol = null;
            tickets = null;
            trust = null;
            user = null;
            clientHostname = null;
            ignoreFile = null;
            connectionMethod = P4Config.ConnectionMethod.DEFAULT;
        }
    }


    public void reloadFile() throws IOException {

        // The Perforce config file is NOT the same as a Java
        // config file.  Java config files will read the "\" as
        // an escape character, whereas the Perforce config file
        // will keep it.
        Properties props = readConfigFile();

        /* All P4 client parameters.
   P4CHARSET        Client's local character set    p4 help charset
   P4COMMANDCHARSET Client's local character set
                    (for command line operations)   p4 help charset
   P4CLIENT         Name of client workspace        p4 help client
   P4CLIENTPATH     Directories client can access   Perforce Command Reference
   P4CONFIG         Name of configuration file      Perforce Command Reference
   P4DIFF           Diff program to use on client   p4 help diff
   P4DIFFUNICODE    Diff program to use on client   p4 help diff
   P4EDITOR         Editor invoked by p4 commands   p4 help change, etc
   P4ENVIRO         Name of enviroment file         Perforce Command Reference
   P4HOST           Name of host computer           p4 help usage
   P4IGNORE         Name of ignore file             Perforce Command Reference
   P4LANGUAGE       Language for text messages      p4 help usage
   P4LOGINSSO       Client side credentials script  p4 help triggers
   P4MERGE          Merge program to use on client  p4 help resolve
   P4MERGEUNICODE   Merge program to use on client  p4 help resolve
   P4PAGER          Pager for 'p4 resolve' output   p4 help resolve
   P4PASSWD         User password passed to server  p4 help passwd
   P4PORT           Port to which client connects   p4 help info
   P4SSLDIR         SSL server credential directory Perforce Command Reference
   P4TICKETS        Location of tickets file        Perforce Command Reference
   P4TRUST          Location of ssl trust file      Perforce Command Reference
   P4USER           Perforce user name              p4 help usage
   PWD              Current working directory       p4 help usage
   TMP, TEMP        Directory for temporary files   Perforce Command Reference
         */
        // Look into parsing these:
        // "P4ENVIRO" (see bug #94)
        // "P4LOGINSSO"
        // "P4SSLDIR"
        // "TMP", "TEMP"


        clientName = props.getProperty("P4CLIENT");
        password = props.getProperty("P4PASSWD");
        String port = props.getProperty("P4PORT");
        simplePort = P4ConfigUtil.getSimplePortFromPort(port);
        protocol = P4ConfigUtil.getProtocolFromPort(port);
        tickets = props.getProperty("P4TICKETS");
        trust = props.getProperty("P4TRUST");
        user = props.getProperty("P4USER");
        clientHostname = props.getProperty("P4HOST");
        ignoreFile = props.getProperty("P4IGNORE");

        // ignore P4CONFIG value
        // trust tickets are used for SSL server validation only; not authentication.

        if (tickets != null) {
            connectionMethod = P4Config.ConnectionMethod.AUTH_TICKET;
        } else {
            connectionMethod = P4Config.ConnectionMethod.CLIENT;
        }

    }

    private Properties readConfigFile() throws IOException {
        Properties props = new Properties();
        FileReader reader = new FileReader(configFile);
        try {
            BufferedReader inp = new BufferedReader(reader);
            String line;
            while ((line = inp.readLine()) != null) {
                int pos = line.indexOf('=');
                if (pos > 0) {
                    final String key = line.substring(0, pos).trim();
                    final String value = line.substring(pos + 1).trim();
                    if (key.length() > 0) {
                        props.setProperty(key, value);
                    }
                }
            }
        } finally {
            reader.close();
        }
        return props;
    }

    @Override
    public boolean hasIsAutoOfflineSet() {
        return false;
    }

    @Override
    public boolean isAutoOffline() {
        return false;
    }

    @Override
    public boolean hasPortSet() {
        return simplePort != null;
    }

    @Override
    public String getPort() {
        return simplePort;
    }

    @Override
    public boolean hasProtocolSet() {
        return protocol != null;
    }

    @Override
    public IServerAddress.Protocol getProtocol() {
        return protocol;
    }

    @Override
    public boolean hasClientnameSet() {
        return clientName != null;
    }

    @Override
    public String getClientname() {
        return clientName;
    }

    @Override
    public boolean hasUsernameSet() {
        return user != null;
    }

    @Override
    public String getUsername() {
        return user;
    }

    @NotNull
    @Override
    public ConnectionMethod getConnectionMethod() {
        return connectionMethod;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getAuthTicketPath() {
        return tickets;
    }

    @Override
    public boolean hasTrustTicketPathSet() {
        return trust != null;
    }

    @Override
    public String getTrustTicketPath() {
        return trust;
    }

    @Override
    public boolean hasServerFingerprintSet() {
        return false;
    }

    @Nullable
    @Override
    public String getServerFingerprint() {
        return null;
    }

    @Override
    public String getConfigFile() {
        // A config file does not have a child config file
        return null;
    }

    //@Override
    //public boolean isPasswordStoredLocally() {
    //    return false;
    //}

    @Nullable
    @Override
    public String getClientHostname() {
        return clientHostname;
    }

    @Override
    public String getIgnoreFileName() {
        return ignoreFile;
    }

    public File getSource() {
        return configFile;
    }

    @Override
    public String toString() {
        return P4ConfigUtil.getProperties(this).toString();
    }
}
