# Caching and Data Storage



## Object Scope

Maintenance of system state is carefully managed.  We have these separate concerns:

* If multiple projects share the same server and method for connecting to the server, then they also
  need to share whether the server is available for contact.  That is, if the server goes offline, the
  user is off of the VPN connection to the server, or the credentials fail for login, then setting that
  state should be done once for the entire application.
* We don't want to keep clutter in the application-stored state about all the Perforce server setups
  the user has ever connected to, along with the cached offline data.  Instead, that should be kept with the
  projects so that it's easy to clean up connection information.

It would be nice to maintain cache cohesion between projects, but we don't want that data shared between projects,
for the storage reasons.

In order to accommodate the first point and the second, the state should be entirely stored in the project,
with an application-wide message sent to notify on changes to state.



## Cache Update

Because the cache may reflect the same client/server data across multiple projects, it would be
efficient to perform updates once, and share the update across the projects.  Thus, project
cache updates occur from the message bus.

This means queries that update the cache as well as actions that change the stored state
both need to be sent to the message bus.  However, this should not be the purview of the direct
P4 server connection code, as it may be hidden behind a cache or other system.  Instead, the
code that calls out to the P4 server API (the `P4CommandRunner`) must update the cache.

There are some IDE API callbacks that require the action run within the callback, and it is
designed to run in-thread.  For these calls, the cache update needs to be called into the
message bus, but also the callback needs to wait for the message bus to complete executing for the
current project.

This means that the callback must both run the message bus and update the project cache.  In order
to avoid double cache updates, each message bus cache update must be delivered as an event object
with a store of the visited cache object ID in the event itself.  An increasing ID could mean that
out-of-order delivery of events would be missed, and storing encountered GUIDs means memory waste.
By storing visited IDs in the event, the memory storage of visited places is garbage collected when
the event object completes. 



## Storing of the Cached Data

(TODO need a nice way to store the cached data without having to implement custom serialization)



## Problems In The Past

When the cache was stored in the application, attempts were made to clean it out when a project removed a
reference to the server.  This means that the server cache would be removed when one project changed its
server setup, and other projects would then lose it.
