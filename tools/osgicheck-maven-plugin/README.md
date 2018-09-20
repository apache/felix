# osgicheck-maven-plugin
Maven Plugin checking several OSGi aspects of your project

## Perform Checks

Add this plugin to your plugins section to enable checking your project:

```xml
   <plugin>
        <groupId>org.apache.felix</groupId>
        <artifactId>osgicheck-maven-plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>
            <execution>
                <id>check-bundle</id>
                <goals>
                    <goal>check</goal>
                </goals>
                <configuration>
                     <mode>[DEFAULT|STRICT|ERRORS_ONLY|OFF]</mode>
                     <config>
                        <check>
                            <name>[name_of_the_check]</name>
                            <mode>[DEFAULT|STRICT|ERRORS_ONLY|OFF]</mode>
                            <!-- Additional check configuration -->
                        </check>
                     </config>
                </configuration>
            </execution>
        </executions>
    </plugin>    
```

## Available Checks

### Import / Export Check

* Name: package
* Configuration: none

The following checks are performed:
* Exports without a version (ERROR)
* Import without a version (range) (WARNING)
* Dynamic import without a version (range) (WARNING)
* Dynamic import * (WARNING)
* Export of private looking package (WARNING)

### Declarative Services Check

* Name: scr
* Configuration: none

The following checks are performed:
* Immediate flag
* Unary references should be greedy
* References ordering (Not finished yet)

### Usage of ProviderType / ConsumerType

* Name: exportannotation
* Configuration: none

The following checks are performed:
* If a package is exported, the classes must be marked with either ConsumerType or ProviderType

