# To Do List

Many of the to-dos are listed in the bug list on Github.



## 2017.1 Stuff

With 2017.1, many things put into the plugin are using old systems, or aren't doing things the right
way anymore.


### Connection Configuration

Connection configuration should be done at the per project level, and not buried on the user setup
page.


### Extended API

A few classes (*which ones? enumerate them here*) use APIs that were extended or changed from
one version to the next.  These need to be pushed into the compat APIs.



## Code Refactoring

This is a look over at the code base and ways it can be improved.  Because it can be drastically improved.


### Break Into Modules

This work has started.  Break up the plugin into smaller systems for better api / impl splits, and to
put more focus onto the testing of independent systems.


### Messages

The messages are spread throughout the system.  These should instead be contained to the UI side.  The exceptions
should be handled with a combination of fine-tuned exceptions (so that there's a 1-for-1 mapping between the message
and the exception, or close to it with little exception introspection) and sufficient context in the exception.

The `FileOperationErrorManager` also is a pattern to remove messages from the API, as well as handle the
warning managers.  From the API standpoint, this works as a user-level logging framework and splitting away
messages from the low-level code, and exceptions are for when the error happens.


### ExceptionUtil (renamed to TodoMoveMeExceptionUtil)

The code here is trying to figure out how to categorize the exception and messages that come out of the
`p4java` library.  Instead, this should be put into the `p4java` library itself.  Indeed, any code that
tries to parse p4d messages needs to instead be pushed into the `p4java` library.


### Connection

The way we connect to the server is overly complicated.  It was made as layers on top of the p4java library
to better detect issues, then layers were made on top of that.  The biggest problem is the grab the password
when necessary.  This should probably be changed to "Ask Password" configuration option (rather than the
confusing "Require Password" option that we have now).  This is at the heart of the majority of issues.


#### P4RetryAuthenticationException

If this is only used for noting that we need to retry authentication, then it should
be renamed.  If it has some other purpose, add it back to UserExceptionMessageHandler.


### Cache

The offline cache of changes should be changed to use the lvcs aspect of IDEA.  This should be moved into
its own module.  The cache was made overly complex, and is at the heart of a lot of issues.


### Component Get

Something like this pattern:

```
  private static class LocalHistoryHolder {
    static final LocalHistory ourInstance = ApplicationManager.getApplication().getComponent(LocalHistory.class);
  }

  public static LocalHistory getInstance() {
    return LocalHistoryHolder.ourInstance;
  }
```

It avoids the synchronization issues.


### Exceptions

The exceptions are a mess, and a good deal of code is spent trying to figure out
what they mean.  The p4java exceptions should be straightened out to be more
clear when they know what their differences are.  The plugin exceptions should
separate out exceptions into groups - plugin problems and server problems.

The exceptions must not have any messages in them.  This should instead be handled by the
`UserExceptionMessageHandler` class.


### More...

There's always more.
