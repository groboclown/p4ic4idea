# Management of State Information

The plugin must manage state data that is persisted between IDE executions.
This means adding a `@State` annotation to the cached data, but more than
that, it means being careful about where the state is stored, and what state
we store.


## Configuration State

Each project maintains a list of the VCS roots and their associated VCS.
For the perforce plugin, this means keeping track of the `ConfigPart` list
used to compose each Perforce VCS root, each root in a `ClientConfigRoot`.

If a VCS root is removed, then the corresponding list of `ConfigPart` must
be removed, and the `ClientConfig` deregistered.

When a `ClientConfigRoot` syncs with the server, and the corresponding Client
workspace has changed, then the `ClientConfigRoot` will need to reverify
itself, and the related cache must be cleared.
