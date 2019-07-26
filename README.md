# org.apache.felix.configadmin.plugin.interpolation

An OSGi Configuration Admin Plugin that can interpolate values in configuration with values obtained elsewhere. Supported sources:

* Files on disk, for example to be used with Kubernetes secrets
* Environment variables
* Framework properties
* System properties

## Usage with Kubernetes secrets

The Kubernetes secrets will surface as file at a certain mountpoint, e.g.:

```
$ ls /mnt/mysecrets
db.password
my.api.key
another.secret
```

The file contents are opaque and contain the secret value as-is.

This plugin will replace OSGi Configuration Admin values declared
using secrets placeholders. These placeholders start with `$[secret:`
and end with `]`. The word inside the placeholder identifies the secret
name as found in the secrets directory on the filesystem. For example:

```
com.my.database:
"user": "my-user",
"password": "$[secret:db.password]"
```

In this example the `user` name for the database is left as-is but for the
`password` the content of the `/mnt/mysecrets/db.password` file is used.

## Interpolating environment variables

Environment variables can be substituted in configuration values by using the
`$[env:SOME_VAR]` syntax. This will use the value of the `SOME_VAR` environment variable.

The placeholder can be part of a larger configuration string, for example:

```
com.my.userinfo:
"greeting": "Hello $[env:USER]!"
```

## Interpolating properties

Properties can also be interpolated in the configuration. The properties values are
obtained through the `BundleContext.getProperty(key)` API, which will return the framework
property for the key. If the framework property is not specified, the system property 
with this key is returned. 

Property values are obtained through the `$[prop:my.property]` syntax.

## Configuration of the plugin

The plugin needs to be provided with the directory where the secrets can be
found on the local filesystem.

This is done through the following property:

* `org.apache.felix.configadmin.plugin.interpolation.dir`: specify the directory where the files used for the file-based interpolation, such as Kubernetes secrets, are mounted.

The property can be provided as an OSGi Framework property or alternatively as a Java System Property. 

