(This file is in context of the new **v2** design.)


# Perforce Server State Caching

The Perforce server state is locally cached on the user's computer in order
to speed up query requests, and to better support working in both connected
and offline mode.

Due to this, there are two states that must be maintained - the last
queried server state (the "system of record"), and the locally stored
state, which is the cached server state with pending server updates.

* **Cached Server State** - the last queried state of the server.
  For the elements that require multiple queries to load (such as
  changelist states), then all queries must be successful in order
  to mark the element as loaded.  In some cases, there may be no
  known state for the element.
* **Local Pending State** - this is a union of the cached server
  state, along with the predicted results of server updates that have
  not been committed yet, or that have been committed, but the
  Cached Server State hasn't caught up with the changes yet.
  We have a goal of keeping the Cached Server State and
  update committing together, but this may not always be possible.


# Perforce Server Updates

Server updates are interactions with the Perforce server that cause *changes*
to the server, as opposed to queries, which inquire as to the state of the
server.

In order to support working both connected and offline modes, the updates are
put into a queue for processing.  The queue will be processed (each item
executed and removed) as long as there are no *User Action Items* pending.
If the user is disconnected, or in some way unable to push requests to the
server before the project is closed, the update queue will be persisted with
the user's IDEA workspace.

When a server update is committed, the Cached Server State should be updated
to reflect the new state.  This can be smart, such that if several updates
affecting the same object run, then the cache update could wait until all
the updates complete.


# Cached States

Because the cached state stores both the last queried server state and
the local pending changes, the cache must be carefully managed.  The system
must be able to reconcile changes made with the plugin that haven't been
sent to the sever, along with updates from the server.

For example, if a user moves a file from the plugin while working offline,
then adds the destination file (rather than a move) directly through the
Perforce tools, the plugin must behave appropriately.


## Connected Mode Updates

When the plugin is working in connected mode, it will assume that all updates
are valid, and will attempt to run them directly.  Errors from the server
may result in Cached Server State refreshes and a synchronization action.

All Cached Server State refreshes must be done while the updates are inactive.
If the plugin is required to run an update in-thread, then it must be synchronized
with the update queue; the update queue should not lock the server state while it
runs over all the updates, but rather once per update.


## On Reconnect

When the server connection is reestablished, any Cached Server State that has a
related pending update must perform a synchronization action.  All further server
actions should be put on hold until these are all resolved.


## Synchronization Action

When there is a pending action that affects a Cached Server State that needs
refreshing, the two need to reconcile any differences.

First, a new Cached Server State is loaded to reflect the current state.
This is to be compared against the old state, so that differences can be
compared against the list of pending updates.  The two Cached Server State
objects have a delta which is reflected by a set of committed updates.

Second, an action must be taken to determine the validity of a pending update.
These updates are either ignored, re-queued, or marked as invalid for the user
to acknowledge.

Last, the Cached Server State is changed to reference the current state.

The general priority of actions is:

1. If there are no differences in the current and previous server
   cached states, then all pending updates are valid and is
   queued for execution.  This also means that the Local Pending
   State is valid.
2. If the current and old Cached Server State do not affect the
   state which the update affects (say, the update adds a file to
   a changelist, while the new Changed Server State only changes the
   changelist description), then the update is valid and is queued for
   execution.
3. If the change in the Cached Server State is the same
   as the pending update (say, they both change the changelist
   description to the same thing), then the update is ignored.
4. If the update conflicts with the change in the current
   Cached Server State (say, they both change the changelist
   description to different values), then the update is
   considered an error, and the user is notified.  Some notifications
   can be acted upon, others are simply informative.


## Server Communication Threading

Due to caching issues, only one connection per server is allowed.

The execution of server requests is controlled by these factors:

* **Pending Updates** - these are sorted per server.  They are the lowest
  priority items, and should only run when nothing else requires running.
  These run asynchronously.  The IDE may require the plugin to perform
  VCS actions from within the called thread, but that will be simulated via
  the caching mechanism; the actual call-out will happen in the
  server thread handler.
* **Server Blocks** - alerts for a specific server, which the user must
  respond to before any other action can happen.  These are behavior
  stoppers - as long as there are any of these blocks, no server communication
  is allowed.  For example, an invalid login, or a server disconnect.
  In the case of working offline, this is considered a silent Server Block;
  the user must act upon this (reconnect to the server), but the user isn't
  given a big message for it.  Silent Server Blocks are a way of representing
  an indefinitely postponed required user action.  Any server communication
  that causes a Server Block will remain in the queue for retry.
* **Server Alerts** - alerts for a specific server, which the user is
  notified about.  These do not prevent asynchronous communication from
  occurring, but the user may be required to perform an action
  (such as click on an "ok" dialog).  For example, a commit request
  that fails is considered an alert, because other server updates
  and queries can continue even though this action failed.  If an update
  causes a Server Alert, then the update is still removed from the queue;
  it's assumed that this kind of problem indicates that an operation failed,
  and the user must perform some kind of action in order to retry the
  operation; it cannot be restarted on its own.
* **Project Blocks** - project-wide blocks.  These block all the server
  communications with all the servers in the project.
* **Project Alerts** - project-wide alerts.  *These might not exist at all.*
* **Cached Server State Synchronize** - perform the
  [Synchronize Action](#Synchronize Action) discussed above.  This may occur
  in the background thread, or in the IDEA requested thread.  In either case,
  the Pending Updates must complete the current update execution, and wait
  to run others, while all pending synchronize actions run.

All the blocks and alerts run in the event dispatch thread (EDT).  By way of
the construction of the factors above, there will be no `invokeAndWait` calls
in the system.  Likewise, because of the Local Pending State, there is no
need to wait on update requests to complete before other communications run.

The Pending Updates queue and Cached Server State Synchronize actions (when run
in the background) can be consolidated where appropriate, much like how the
AWT consolidates `paint` requests in the EDT.


# Reconciling Differences

There are two types of reconciliation - server reconciliation, where changes
on the server side are loaded and must be reconciled with the local changes,
and local reconciliation, where the local changes must be reconciled with
the server version.

Changes are managed on a per-object basis, where the objects are one of the
following groups:

* **Job** - changes to the description of the job.
* **Workspace** - changes to the root or view mappings.  Either one of these
  changes implies that the currently stored mappings between the depot
  and file system must be reloaded.
* **Changelist** - changes to the description, job association, and
  fix state.  The files associated with changelists are handled on their own,
  with the changelist association on those files being an indirect relationship
  to the corresponding changelist object.
* **File** - each file is managed on its own (note that *move* operations
  have an implicit relationship between two files, particularly that they
  must be in the same changelist, but that should be handled at a higher level).
* **Ignore** - the list of file globs or regular expressions that the
  plugin must ignore.  Changes to this ignore mean changes to the handling
  of files.  Even though this is a client-stored file, reloads from the file
  (or just file editing) versus UI operations to update the file need to be
  managed; specifically, the internal cached state needs to be updated when
  the file is updated, and UI actions on this file ("ignore file") need to
  be pushed to the file.
