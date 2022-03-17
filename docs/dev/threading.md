# Threading Issues and Solutions

## EDT vs Worker Threads

The EDT is the *event dispatch thread*, which handles all the Swing events.  The Swing UI events are handled in a single event, all the things from handling a keystroke to drawing on the screen.  This means all execution in the EDT must be as quick as possible.  Anything that takes time to run must execute in a worker thread.

Where possible, all API calls should know their execution thread context, whether they run in the EDT or in a worker thread.


## EDT Required Calls

Some API invoked functions from the IDE forces the plugin code to run in the EDT.  We need to make sure that these are clearly marked, and that the code paths are closely examined to restrict what they can do.


## Preventing Accidental Blocks

One way to prevent accidental invocation of blocking code is to force blocking code to return a Promise instance.  This clearly marks that the command will potentially take a long time, and that the handling of the invocation will be handled in a worker thread.

If an EDT equivalent invocation is required, then it should be in a "sync" version of the method, and should have several modes of invoking:

* *A quick glimpse:* just return the last cached value, if available.  The invocation doesn't really care that much to get a perfect answer.
* *Last and Future:* it returns an object that contains the most recent cached answer, and a Promise for when the most recent data is fetched.  This has a potential for extreme optimization by sharing a single promise for all requests while a long running call is in progress.

Some EDT invocations are required, such as to obtain a write lock on a file.  In these cases, the IDEA API should be used to run really small pieces of work.  Don't take advantage of this generosity by throwing a bunch of work into it in a loop.


### Promises and the EDT

Promises can help limit the time in the EDT if it's necessary.  You can chain promises together with different pieces of work running in the EDT and others in worker threads.


## Requiring User Interaction

User interaction shouldn't be required, unless the user explicitly declares that they want to do something and the UI tells them through iconography that it will require additional interaction, like open a file dialog or typing in a password.

If an API call requires immediate response, we *must not* go ask the user.  User preferences are fine, but not a dialog.  If, without user input, we don't know what to do, then report a failure.

If the API call comes from a UI component that we have some control over, then we should indicate on the UI component that user feedback is required.


## Old Version Bugs

One of the big problems with the old versions of the plugin was requesting user feedback.  This would turn into nightmare scenarios when called from the EDT.
