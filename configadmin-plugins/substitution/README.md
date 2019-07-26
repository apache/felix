# org.apache.felix.configadmin.plugin.substitution

An OSGi Configuration Admin Plugin that can substitute values in configuration with values obtained elsewhere. Supported sources:

* Files on disk, for example to be used with Kubernetes secrets

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

## Configuration

The plugin needs to be provided with the directory where the secrets can be
found on the local filesystem.

This is done through the following property:

* `org.apache.felix.configadmin.plugin.substitution.dir`: specify the directory where the Kubernetes secrets are mounted.

The property can be provided as an OSGi Framework property or alternatively as a Java System Property. 

If configuration is not provided the plugin will be disabled.
