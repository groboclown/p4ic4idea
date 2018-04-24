# Error Management and Reporting

## Errors vs Reporting vs Correcting

Errors are split into usage categories.  In general, code that requires
exception handling (such as reporting back to the IDEA caller API) is
used for error signaling, meaning that it should report a simple error.
It shouldn't attempt corrective behavior.

Error reporting, on the other hand, is handled through the message
bus.  This allows for generating a detailed report about the source of
the error and the characteristics of the error.  At this point, different
systems can respond to the error in their own way.  There doesn't need
to be a generic exception handler that tells each system about the problem.
It also means that reporting the issue to the user can be handled by the
UI code in a way that is best for the user to manage.

Corrective actions from the errors should be handled either by the
user indicating a desired action through the UI code (or defaults in
the options), or through message bus error listeners that are crafted
to handle specific cases.


## When To Use Exceptions

Exceptions should be used when an error occurs that the result object
doesn't need to concern itself about.  If the code is written to perform
actions on a result of an invocation, then it shouldn't need to worry about
error cases.  If the code is needing to perform the action and perform
complex error handling, then it's doing the wrong thing.  Exception handling
needs to be kept *minimal* and *optional*, such as wrapping exceptions in
other types or logging.  ("optional" here meaning allowing the exception
to bubble up, rather than making all exceptions RuntimeException types.)

Exceptions should also be used with Promises, but very carefully.  Promises
don't have good ways to enforce trapping of certain messages.  Therefore,
the Promises exceptions should be *limited* and *clearly documented*.


## Pushing Exceptions to Messages

Some code needs to deal with translating low-level exceptions into messages,
such as the Perforce server API interaction.  Where possible, this needs
to be contained in a wrapper.  Lambdas can improve clairy here by allowing a
declaration like

```(java)
    return MyExceptionUtil.handle(() -> runRequest(data));
```

and have all the underlying messiness of exception translation and message
bus communication be handled out-of-sight.


## The Old Approaches

The old versions of the plugin heavily relied upon Exceptions to tell
other components about errors.  This lead to repeated, messy code that
essentially turned the "catch" block into a case statement, with all kinds
of attempts at interpreting intent.
