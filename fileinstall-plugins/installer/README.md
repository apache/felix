Bundle Archive Installer Plugin
===============================

The purpose of this plugin is to provide an Artifact Installer for Bundle Archive (BAR) files.


Bundle Archive (BAR) Files
--------------------------

A Bundle Archive is a new file format for installable functionality represented as a collection of one or more OSGi bundles. The purpose is to provide a single file that can be easily deployed onto a server or remote device without conflicting with existing installed bundles.

The purpose of a BAR is similar to a "Deployment Package" as defined by the OSGi Deployment Admin Specification (see OSGi Release 6 Compendium, Chapter 114). However, two or more Deployment Packages cannot include or reference the same resource, which is problematic because real-world software units often have overlapping requirements, e.g. shared libraries. In contrast multiple BAR files can refer to the same resource, and this resource will be installed only once at runtime. A reference-counting mechanism is used to ensure that shared resources are not uninstalled until the last BAR using that resource is uninstalled.

A BAR is physically a JAR file with a META-INF/MANIFEST.MF. It must be named with the `.bar` extension and it must contain one or more index files in the format specified by OSGi Release 6 Compendium, Section 132.5 "XML Repository Format".

The manifest should contain the following headers:

* `Deployment-SymbolicName` (mandatory): the symbolic name of the deployable unit;
* `Deployment-Version` (optional, defaults to `0.0.0`): the version of the deployable unit;
* `Deployment-Name`: (optional): a human-readable name for the deployable unit;
* `Index-Path`: (optional, defaults to "./index.xml"): a comma-separated list of paths to index files within the BAR.
* `Require-Bundle` **AND/OR** `Require-Capability` (at least one must be present): a root list of requirements which shall be resolved against the indexes listed under `Index-Path`.


Resolving Process
-----------------

A BAR file can be made available for installation by dropping a file with the extension `.bar` into a FileInstall-monitored directory. The BAR installer plugin will then resolve the requirements of the BAR listed under `Require-Bundle` and `Require-Capability` against the indexed listed under `Index-Path`, and using the context of existing installed bundles in the OSGi Framework.

If resolution fails, e.g. due to missing resources, then an `InstallableUnitEvent` with an `ERROR` state will be sent to all registered `InstallableListener` services.

If the resolution succeeds then an `InstallableUnitEvent` with a state of `RESOLVED` is sent to all registered `InstallableListener` services. The event shall include an `InstallableUnit` object listing all of the artifacts that would need to be installed into the present OSGi Framework in order to successfully install the BAR.

Note that the plugin DOES NOT automatically install any resources into the OSGi Framework, it merely makes the unit available to be installed. In order to actually install, an application or management agent is required to implement the `InstallableListener` service and/or interact with the provided `InstallableManager` service.


Installation Process
--------------------

If installation of an installable unit is requested by the application or management agent, then the list of external bundles shall be installed into the OSGi Framework. Reference counting is used for all bundles installed via this path: if a second BAR requires a bundle that was installed from a previous BAR then we do not attempt to install a second copy, but only increment the reference count for that bundle. When a BAR is uninstalled, the reference count for all bundles it requires is decremented, and if that reference count reaches zero then the bundle is actually uninstalled from the OSGi Framework. In this way we allow multiple BARs to share depedencies, but uninstallation of one BAR does not affect other BARs that depend on the same bundles.


Resolve Invalidation
--------------------

A resolved BAR may remain in the `RESOLVED` state for some time, depending on the behaviour of the application. If the state of the OSGi Framework later changes, then the resolution result may no longer be valid. Similarly a BAR that is in `ERROR` state because it failed to resolve (e.g. because a dependency was not available) may later succeed if the context changes.

Therefore the plugin invalidates all BARs in the `RESOLVED` or `ERROR` state in the event of any OSGi bundle being installed, resolved, unresolved, updated or uninstalled. This would include e.g. a refresh operation initiated from the shell. Invalidated BARs are placed back into a queue to be re-resolved in due course.

Note that the plugin does not attempt to ensure that an installed BAR remains in a consistent state. If the OSGi Framework context is changed outside of our control (e.g. the user uninstalls a bundle manually) then the bundles forming the BAR may be in an invalid state.