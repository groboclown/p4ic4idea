# Configuration and Connection Management

The big idea to keep in mind with connection management is that
the server layer will, by necessity, need to maintain a cache of pending commands.
Also, a user can potentially have multiple projects open that work with the
same workspace.  However, [cache data must be owned by the project](cache.md).


## Configuration

The user can declare their connection setup as collections of `ConfigPart` instances.
These have the potential to be mutable.  For example, a file-based configuration
could change, making the `ConfigPart` reload different values.

The stores of connection information must keep immutable configuration objects.
These are stored as `P4ServerName`, `ServerConfig`, and `ClientConfig` instances.
These need to properly handle `equals(Object)` semantics so that they can be shared
and compared between projects.

### Configuration in the UI

With the newer IDEA versions, each root directory for the version control plugin can
have its specific configuration.  This needs to be used for our plugin, so that the
user can more easily have different roots point to different servers or workspaces.

With this change, we can remove support for "relative file" configuration, and instead
just require the user to specify the setup in each root.

### Requesting the User Password

This directly ties into the [threading model](threading.md).


## Connections

The server will need to maintain a store of connections and their caches that should
be persisted from one execution to the next.  This means that care must be taken
to generate messages that distinguish between when a project is closed and it no longer
needs to keep its connections in memory, and when the user removes or changes
connection settings.


## The Old Way

Connections to the server, and the configuration of the connections caused all
kinds of issues in the old versions.
