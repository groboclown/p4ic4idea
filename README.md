# Perforce IDEA Community Integration

### VCS support for Perforce

The plugin allows for associating an [IntelliJ IDEA Community Edition IDE](https://www.jetbrains.com/idea/)
project with a Perforce repository through IDEA's built-in VCS support.

**Currently Supported IDEA versions: 13.5 through 2016.2** *which includes Android Studio 1.0 and many other products.*
For a full list of supported products, please see
[the JetBrains plugin page](https://plugins.jetbrains.com/update/index?pr=&updateId=22898)



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
        1. Follow [the building guide](BUILDING.md) for details on setting up your
           environment and building the `p4ic4idea.zip` file.
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

(FIXME)

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

When submitting a changelist, you may associate Perforce jobs with the
changelist, and set the job status.  This association will only be used
when the changelist is actually submitted.
