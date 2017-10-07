# Installer Test

This project tests the BAR Installer in an OSGi container and utilizes the [PaxExam](https://ops4j1.jira.com/wiki/display/PAXEXAM4/Pax+Exam ) framework. Because this test is an integration test rather than a unit test, it must be in a separate project. 

### Dependencies
This project depends on [**org.apache.felix.fileinstall.plugin.installer**](../installer) and [**org.apache.felix.fileinstall.plugin.resolver**](../resolver).