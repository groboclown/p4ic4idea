# v3 Architecture

## Perforce Server Connections

To connect to the server, several layers are used:

* [Connection Information](#connection-information) defines *how* the plugin
    contacts the server.  These are constructed on a per-VCS root basis.
* [Connection Runner](#connection-runner) performs a specific request against
    the server using connection information.
* [Caches](#caches) maintain an offline data store of the known server state.
* [Error Handlers](#error-handlers) listen to problems reported by the server
    and delegate it to responsible parties.


## Connection Information

* `ConfigPart`: describes the connection setting.  Multiple of these can be
    grouped together in a `MultipleConfigPart`, to allow piecing together
    different ways of defining the connection.  The parts can change through
    user actions, or update themselves by reloading settings from their
    source, such as through reload a file or re-reading environment values.
* `ClientConfig`: a snapshot of a `ConfigPart`, which defines a `ServerConfig`
    and additional settings on top of it for a specific client workspace.
    These objects can be shared across projects to keep internal caches
    synchronized.  If the underlying `ConfigPart` elements change, then
    new `ServerConfig` and `ClientnConfig` instances must be created.
* `ClientConfigState` and `ServerConfigState` keep stateful information about
    the configs, such as whether the connection represented by the object
    is online.


## Connection Runner

The `P4CommandRunner` interface allows for a unified way to interact with
the server, or a client-side cache on top of the server.  The interface itself
is broken into methods that define the different ways to interact with the
server, while a robust type system delegates the actual behavior requested
to other objects.

The runner explicitly works with `Promise` objects in order to make clear the
commands which can potentially take a while.  Some commands also clearly reference
potential cached values, allowing for more immediate responses.  This forces
conformity to the [threading model](threading.md).

The runner sends the requestor very simple errors.  The majority of the errors
are passed as messages through the [error handlers](#error-handlers).  Code that
directly runs the connection runners only need to deal with simplified errors
to understand that an error happened, and react accordingly.  The real work
of communicating the error to the user is done through
[message listeners](#message-listeners).

### Server Command Runners

For the command runners that make connections to the server itself, they use a
`ConnectionManager` to handle potential connection pools.

The `SimpleConnectionManager` does not maintain a pool, but has the downside that
it does nothing to limit the number of concurrent connections.  For some users with
slow connections, this can potentially open too many connections to the server,
causing the server to reject connections.  It should only be used for testing
purposes.

The low-level server runners do not maintain caches.  However, when they receive
data or requests for actions, they send out the cache update messages.  This
restricts the sources for the cache updates, so that the message sending doesn't
need to be remembered to be added to a bunch of different places, but instead
just one (well, each server implementation). 
 

### Cached Command Runners

The cached command runners delegate commands to the server runners.  Updates
to the caches is done through the messaging system.  The cached server runners
know how to return immediate values for queries.  These runners should sit
on top of caches, rather than manage the cache themselves.


## Caches

Caches maintain a limited copy of information about the server state.
They are [stored at the project level](cache.md).  They must be adaptable
enough to understand how pending requests to the server can affect requests
for information.

Caches are not expected to store all the server information, but rather just
enough for a client workspace.  This means that some requests, like "add a file",
must instead be flexible to run as "add or edit".

Cache updates are sent through the application message bus, so that all open
projects can update their internal data.  Pending actions, however, are
only stored in the originating project.  This can have a possible down side,
where the same pending action is stored multiple times, but that is considered
a user problem ;) .

One potential issue to look out for is the issue of shared state between
components, as seen with the
[Facebook bug](https://www.infoq.com/news/2014/05/facebook-mvc-flux) that
caused them to redesign their client applications using the
[Flux model](https://reactjs.org/docs/lifting-state-up.html).  The key take-away
from that bug was that bi-directional data flow can cause fundamental design
issues.  Their solution with a dispatcher closely resembles the message bus,
so keeping to the cache model updates through the message bus should mitigate
these kinds of issues.


## Error Handlers

The `P4RequestErrorHandler` wraps calls to the Perforce Java API and translates
exceptions into how the plugin can deal with them.  This can mean broadcasting
error messages to the appropriate message bus, and creating simplified
`ServerResultException` errors.

The `MessageP4RequestErrorHandler` subclass handles all the dirty work of
figuring out what the thrown exception means.

### Message Listeners

The error handlers broadcast error messages to either the application
message bus or the project message bus.

Application bus error messages are
restricted to information about the connection state of a server, so that all
projects can share the information, rather than having to rediscover it
for themselves.  It also means that errors such as password problems are only
communicated to the user once for the entire application (with a big caveat here).

Project bus error messages are all the other issues, such as file write problems
or underlying API errors.  These errors relate more to the requested action,
and so are restricted to the project originating the request.

There are many categories of errors.  These are split up in the
`net.groboclown.p4.server.api.messagebus` package.
