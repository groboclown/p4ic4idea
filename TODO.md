# To Do List

Many of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view of Github.

## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### SSL, SSO, and Manual Passwords

The SSL, SSO, and asking the user for passwords

### Revert Files

Reverting files - revert unchanged and normal revert - still needs to be implemented.

### Error Reporting

Need better display for errors, and all promise-like behaviors need on-error catches to report problems.

The Active Connection panel should show errors beside pending actions if there was an error sending it to the server.

### Repository View

The repository view doesn't create committed changelists (it throws an exception because it's not implemented).

### File Annotation

The file annotation needs a lot of work.

### Submit Change

The submit change needs to be implemented correctly.

### Show File History

Just like the repository view, the file history view needs to be implemented (`P4CommittedChangesProvider`).

### Go Offline / Go Online

Add go offline / online buttons in the active connection panel.

### Manage Pending Operations

If there are pending actions that failed to go through due to errors, the user needs a way to manage these operations.  This should be done through the active connection panel.

### Swarm Integration

Swarm integration needs to be re-instated.

## Near-Term Functionality

These pieces of functionality are not requried for the 0.10 release, but should be implemented soon after release.

### Caching File Contents

The cache mechanism should support making a copy of a file when an operation happens, to allow for better offline support.

### Relative P4CONFIG support

The choice was made to eliminate the use of relative P4CONFIG files, and instead managed through the VCS Root mechanism.
However, without this, the full environment support won't work.  This needs to be re-added, with support of the VSC Root
mapping mechanism in `P4Vcs`.  However, the user needs to be able to manage it, and that requires new UI support.  This
is a big feature, and will require some careful planning to handle correctly.

### Load Project from VCS

An old feature request.  Still needs to be added.

### Use Windows Registry Passwords

The passwords stored in the Windows Registry (through the `p4 set PASSWD` command and other password commands) is stored
in an encrypted way.  The code should include handling the encrypted values.  The publicly available p4 cli C code
contains the implementation for how it's done.  However, that code is non-trivial. 
