# IDEA Community VCS Integration for Perforce

This is the location for the Perforce VCS integration into the [IntelliJ IDEA Community Edition IDE](https://www.jetbrains.com/idea/).

**Currently Supported IDEA versions: 13.5 (Android Studio), 14.0 (build 14.2118 and above)**

*Other versions within that IDEA build range may work, but they haven't been tested.*


# Getting Started

1. Install the plugin by one of these methods:
    * From within IDEA
        1. Open the IDE settings dialog ( **File** -> **Settings...** ).
        1. Navigate to the **Plugins** panel.
        1. Click the **Browse repositories...** button.
        1. Select the **Perforce IDEA Community Integration** plugin.
    * Download and install from disk:
        1. Download from the
           [Jetbrains plugin center](http://plugins.jetbrains.com/plugin/7685)
           or from the [Github releases](https://github.com/groboclown/p4ic4idea/releases/latest)
        1. In IDEA, open the IDE settings dialog ( **File** -> **Settings...** ).
        1. Navigate to the Plugins panel.
        1. Click the **Install plugin from disk...** button.
        1. Select the downloaded zip file.
    * Build it yourself:
        1. You'll need Ant 1.8 or better, a JDK 1.7, a version of
           IntelliJ IDEA v 14.1 or better, and the plugin code from
           GitHub on your local computer. You'll need a copy of
           p4d (or p4s for Windows) to run the tests.
        1. Copy the `local.properties.template` to `local.properties`
           in the source root directory, and edit the values to
           match your configuration.
        1. Run `ant all` from the source root directory.
        1. In IDEA, open the IDE settings dialog ( **File** -> **Settings...** ).
        1. Navigate to the Plugins panel.
        1. Click the **Install plugin from disk...** button.
        1. Select `p4ic4idea.zip` from the source root directory.
1. In IDEA, select the VCS root directory managed by Perforce.
    1. Open the IDE settings dialog ( **File** -> **Settings...** ).
    1. Select the **Version Control** panel.
    1. Select the root directory from the list (or add a new one), and choose
       "Perforce" from the drop-down selection.
1. Choose the Perforce connection method.
    1. From either the **Version Control** panel (select the pencil
       icon with the Perforce root selected), or the Perforce panel
       under the Version Control.
    1. Select the connection type and the parameters,
       and click **OK**.
    

# Connecting to Your Perforce Server

In the Perforce configuration panel, you must choose the way in which the
plugin connects to the Perforce server.  There are several methods available.

For any of these methods, the Perforce server may request a password from
you.  If you choose the **Save passwords** option, the password will be
stored application-wide, associated with that server and user name.


## Direct Declaration

There are two methods available that allow you to directly declare the
connection parameters, *Client Password* and *Authorization Ticket*.

Both of these connection types require you to enter the server connection
string ("Perforce port") and the username.  If a password is required, you will
be prompted for it.

The "Perforce port" is usually just in the form "hostname:port number".  SSL
connections will need to use "ssl://hostname:port number".


## Indirect Declaration

Alternatively, you can use connection methods that simulate how the Perforce
command-line clients connect to the server.  All of these use the following
standard keys:

* `P4PORT`: server connection string.  SSL connections need to use the
   "ssl://hostname:port number" format.
* `P4USER`: username used to connect to the server.
* `P4PASSWD`: password used to connect to the server.  If not provided,
  you may be required to enter it.
* `P4TICKETS`: storage location for the Perforce ticket files.  If present,
  the plugin assumes that the connection will perform a "p4 login" and
  store the associated ticket in that file.
* `P4TRUST`: location of the trust ticket. *Currently not supported.*
* `P4SSLDIR`: directory containing the SSL connection information.
  *Currently not supported.*
* `P4CONFIG`: these settings can be loaded from a configuration file.
  Note: P4CONFIG settings are only loaded from one file; you can't have
  config files referencing other config files.
* `P4CLIENT`: Default client workspace name.  You can override this
  in the UI.


### Environment Variables

This connection method uses the environment variables that launched the IDE
as the connection properties.  For Windows users, it also checks the
registry entries for the corresponding values.

This currently doesn't support Mac OSX local settings.


## Specific P4CONFIG File

Reads the connection settings from a single configuration file.


## Relative P4CONFIG File

This is the only connection method that allows for multiple servers and
clients to be referenced from a single project.  This works by finding
a P4CONFIG file (with the name given in the UI) which is in the current
directory, or some parent directory, of each file being referenced.
This replicates how the Perforce command-line tool searches for settings.

For example, with the file name set to `.p4config`, you can put one
copy of this file into each module root directory.  Then, each module will
have its files managed in their own client and server.

Some parts of the normal operation will act a bit differently.  For instance,
IDEA changelists can be associated with multiple Perforce changelists, if
files from different clients are added to them.  You can move files across
servers, but these will be turned into simple add/delete operations
(see [bug #15](https://github.com/groboclown/p4ic4idea/issues/15)).


# Workflow

With your working Perforce connections specified, you can use the IDE
as usual.  Editing files will check them out from the server.  Moving
or renaming files will trigger a P4 rename operation.  Deleting files
will delete them from the server.  All edited files will show up in
the IntelliJ change lists, and those will be associated with the
corresponding Perforce change.

From the change lists, you can view a difference against the head
revision.  You can also view the file history, and compare different
revisions against each other.

Currently, submitting a changelist is not enabled.

