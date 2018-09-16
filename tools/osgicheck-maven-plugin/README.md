# osgicheck-maven-plugin
Maven Plugin checking several OSGi aspects of your project

Add this plugin to your plugins section:

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
            </execution>
        </executions>
    </plugin>
```
