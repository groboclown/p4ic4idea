# Management of State Information

The plugin must manage state data that is persisted between IDE executions.  This means adding a `@State` annotation to the cached data, but more than that, it means being careful about where the state is stored, and what state we store.


## Configuration State

Each VCS root in each project has a corresponding `ClientConfigRoot`, which are stored in the `ProjectConfigRegistry` component (specifically, the `ProjectConfigRegistryImpl` implementation).

However, the `ClientConfigRoot` only has meaning for properly configured clients.  The low-level state backing these clients is persisted to the user's `workspace.xml` file by the `PersistentRootConfigComponent` class, which stores that as a list of `ConfigPart` instances.  The user manages these through the `P4VcsRootConfigurable` GUI component. 

If a VCS root is removed, then the corresponding list of `ConfigPart` must be removed, and the `ClientConfig` deregistered.

When a `ClientConfigRoot` syncs with the server, and the corresponding Client workspace has changed, then the `ClientConfigRoot` will need to reverify itself, and the related cache must be cleared.
