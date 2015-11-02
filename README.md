Alfresco Demo Data
---

This module (Alfresco Repository AMP) loads some sample sites, users and groups into Alfresco at boot time; it can be used for demo purposes, but also for initial Repository configuration.

The project allows you to run Alfresco locally and create your content; following the steps below you will be able to simply export your data, update the project and (re)generate the AMP with the updated data that needs to be bootstrapped.

Prerequisites
---
- [Alfresco Knowledge](http://docs.alfresco.com/5.0/concepts/system-about.html)
- [Alfresco Maven SDK 2.1.1 Knowledge](http://docs.alfresco.com/5.0/concepts/alfresco-sdk-intro.html)
- [JDK 1.8](http://docs.alfresco.com/5.0/tasks/alfresco-sdk-install-java.html)
- [Maven 3.2.5+](http://docs.alfresco.com/5.0/tasks/alfresco-sdk-install-maven.html)
- [Maven Setttings](http://docs.alfresco.com/5.0/tasks/alfresco-sdk-install-maven-opts.html)


How to run it
---

From the project parent folder execute **run.sh** or **run.bat** and it will start an Alfresco Community version on an embedded Tomcat with embedded [H2 Database](http://www.h2database.com/html/main.html)

What's already inside
---

Once the Alfresco SDK is running is possible to access it through the URL [http://localhost:8080/share](http://localhost:8080/share).

By default the Alfresco Demo Data will create the following **Share Sites**:

- `2014-company-rebranding`
- `brand-awereness-compaign`
- `company-reports`
- `contracts`
- `customer-service-case-files`
- `engineering-development`
- `global-sales-enablment`
- `hr-files`
- `marketing`
- `marketing-image-gallery`
- `rm (Records-Management)`
- `sales-operation`

And a number of users and groups listed here:

- [Users](http://localhost:8080/share/page/console/admin-console/users#state=panel%3Dsearch%26search%3D%255C*)
- [Groups] (http://localhost:8080/share/page/console/admin-console/groups#state=panel%3Dsearch%26refresh%3Dfalse)

Updating content
---
Updating/creating new content is available in two different ways:

- *Dynamic way* (No much configuration needed)
- *Standard way* (Some Spring xml configuration need)

Let's have a look at them:

### Dynamic way
--

##### Add a new Site (non RM)

- It is first necessary to export the site from an existing Alfresco instance using the already existing [site-export Rest API](http://docs.alfresco.com/community/references/RESTful-SiteSite-exportGet.html) with admin user credentials

```
curl -u admin:admin http://localhost:8080/alfresco/s/api/sites/$SITE_NAME/export > site-export.zip
```
- Unzip the export file
- Create a folder into **demo-data/repo-amp/src/main/amp/config/alfresco/module/repo-amp/bootstrap/dynamic/sites**  with the site name (it has to be the exact same shortname used by the Share url)
- Move the **Contents.acp** into the previously created folder (Other ACPs are not needed)
- No other configuration needed


##### Add a new RM Site
- It is first necessary to export the RM site from an existing Alfresco instance using a custom RM site-export Rest API (included in this repo-amp) with admin user credentials

```
curl -u admin:admin http://localhost:8080/alfresco/s/api/rm-site/rm/export > rm-site-export.zip
```
- Unzip the export file
- Move the **Contents.acp** into **demo-data/repo-amp/src/main/amp/config/alfresco/module/repo-amp/bootstrap/dynamic/sites/rm** (Other ACPs are not needed)
- No other configuration needed

##### Add Authorities
- It is first necessary to export the authorities from an existing Alfresco instance using a custom Authorities export Rest API (included in this repo-amp) with admin user credentials

```
curl -u admin:admin localhost:8080/alfresco/service/api/people-groups/export > authorities-export.zip
```
- Unzip the export file
- Move the **People.acp**,**Users.acp**,**Groups.json** into **demo-data/repo-amp/src/main/amp/config/alfresco/module/repo-amp/bootstrap/dynamic/authorities**
- No other configuration needed
                                                                                                                              
### Standard way
--

##### Add a new Site (non RM)

- It is first necessary to export the site from an existing Alfresco instance using the already existing [site-export Rest API](http://docs.alfresco.com/community/references/RESTful-SiteSite-exportGet.html) with admin user credentials

```
curl -u admin:admin http://localhost:8080/alfresco/s/api/sites/$SITE_NAME/export > site-export.zip
```
- Unzip the export file
- Create a folder into **demo-data/repo-amp/src/main/amp/config/alfresco/module/repo-amp/bootstrap/sites/standard/**  with the site name (it has to be the exact same shortname used by the Share url)
- Move the **Contents.acp** into the previously created folder (Other ACPs are not needed)
- Create a Spring Bean into repo-amp/src/main/amp/config/alfresco/module/repo-amp/context/bootstrap-context.xml similar to the following example

```
	<bean id="patch.siteLoadPatch.mysite" class="org.alfresco.repo.admin.patch.impl.SiteLoadPatch" parent="patch.siteLoadPatch.generic">
        <property name="id"><value>patch.siteLoadPatch.mysite</value></property>
        <property name="disabled"><value>false</value></property>
        <property name="siteName">
            <value>mysite</value>
        </property>
        <property name="bootstrapViews">
            <map>
                <entry key="contents">
                    <props><prop key="location">alfresco/module/${project.artifactId}/bootstrap/sites/standard/mysite/Contents.acp</prop></props>
                </entry>
            </map>
        </property>
    </bean>
```
- **Note:** replace every occurance of 'mysite' with the real site shortname

##### Add a new RM Site
- It is first necessary to export the RM site from an existing Alfresco instance using a custom RM site-export Rest API (included in this repo-amp) with admin user credentials

```
curl -u admin:admin http://localhost:8080/alfresco/s/api/rm-site/rm/export > rm-site-export.zip
```
- Unzip the export file
- Move the **Contents.acp** into **demo-data/repo-amp/src/main/amp/config/alfresco/module/repo-amp/bootstrap/standard/sites/rm** (Other ACPs are not needed)
- Create a Spring Bean into repo-amp/src/main/amp/config/alfresco/module/repo-amp/context/bootstrap-context.xml similar to the following example

```
	<bean id="patch.siteLoadPatch.rm" class="org.alfresco.repo.admin.patch.impl.SiteLoadPatch" parent="patch.siteLoadPatch.generic">
        <property name="id"><value>patch.siteLoadPatch.rm</value></property>
        <property name="disabled"><value>false</value></property>
        <property name="siteName">
            <value>rm</value>
        </property>
        <property name="bootstrapViews">
            <map>
                <entry key="contents">
                    <props><prop key="location">alfresco/module/${project.artifactId}/bootstrap/sites/standard/rm/Contents.acp</prop></props>
                </entry>
            </map>
        </property>
    </bean>
```


##### Add Authorities
- It is first necessary to export the authorities from an existing Alfresco instance using a custom Authorities export Rest API (included in this repo-amp) with admin user credentials

```
curl -u admin:admin localhost:8080/alfresco/service/api/people-groups/export > authorities-export.zip
```
- Unzip the export file
- Move the **People.acp**,**Users.acp**,**Groups.json** into **demo-data/repo-amp/src/main/amp/config/alfresco/module/repo-amp/bootstrap/standard/authorities**
- Create a Spring Bean into repo-amp/src/main/amp/config/alfresco/module/repo-amp/context/bootstrap-context.xml similar to the following example

```
   <bean id="patch.groupsUsersPeople.demoData" class="org.alfresco.devops.importers.UsersGroupsImporterPatch" parent="patch.authoritiesPatch.generic">
        <property name="id"><value>patch.groupsUsersPeople.demoData</value></property>
        <property name="disabled"><value>false</value></property>
        <property name="bootstrapViews">
            <map>
                <entry key="users">
                    <props>
						<prop key="location">alfresco/module/${project.artifactId}/bootstrap/standard/authorities/Users.acp</prop>
                    </props>
                </entry>
                <entry key="people">
                    <props>
                        <prop key="location">alfresco/module/${project.artifactId}/bootstrap/standard/authorities/People.acp</prop>
                    </props>
                </entry>
                <entry key="groups">
                    <props>
                        <prop key="location">alfresco/module/${project.artifactId}/bootstrap/standard/authorities/Groups.json</prop>
                    </props>
                </entry>
            </map>
        </property>
    </bean>
```



Release
---
The release process will create the AMP artifact and upload it to Alfresco Internal Nexus; you can change `pom.xml` settings to use your own Maven Repository.

Maven is configured to only deploy the AMP artifact, saving time and bandwidth.

```
mvn release:prepare release:perform
```

Make sure that the following item is defined in your `~/.m2/settings.xml`

```
    <server>
      <id>alfresco-internal</id>
      <username>...</username>
      <password>...</password>
    </server>
```

Notes
---
#### Why are both Users.acp and People.acp necessary?
Alfresco uses both cm:person and usr:user objects to store users information, so they are both needed when importin/exporting users info.

#### How do the importers work?
Users,People and Site contents are imported using the Alfresco [ImporterBootstrap](http://dev.alfresco.com/resource/docs/java/org/alfresco/repo/importer/ImporterBootstrap.html).
This means that it will import the nodes using the exact same nodeRef used in the previous Alfresco instance.

It is not possible to import Groups at the moment using the ImporterBootstrap, so a new Exporter/Impoter logic has been implemented that will export and import new groups, handle the membership and the zones.

#### How do the dynamic importers work?
They use the [BeanDefinitionRegistryPostProcessor](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/support/BeanDefinitionRegistryPostProcessor.html) utility to dinamically create Spring bean to load data

#### Spring Beans Order Matters!!!!
- Authority importers have to be loaded after Site content importer, otherwise the site creation will fail (because the site groups would have been already created). So it is necessary to define the Site importer Spring bean before the authorities one in the context xml file.
- Beans for standard import are authomatically loaded before the dynamic ones regardless of the order on the xml context file, no way to change this behaviour at the moment

#### Export Data
To export authorities or RM-Sites the demo-data amp should be used in your Alfresco instance.
***IMPORTANT: before applying it remove all the existing Sites and Authorities from the AMP otherwise they will be loaded at bootstrap.***

#### More about the Authorities Exporter
The url of the webscript is:

```
<url>/api/people-groups/export?usersToExport={usersToExport}&amp;groupsToExport={groupsToExport}&amp;excludeSiteGroups={excludeSiteGroups}&amp;groupsToExclude={groupsToExclude}&amp;usersToExclude={usersToExclude}</url>
```

In the url you can set:

- usersToExport: comma separated list of users to export, if not present all the users will be exported
- groupsToExport: comma separated list of groups to export, if not present all the groups will be exported
- excludeSiteGroups: boolean (TRUE/FALSE), determines whether to exclude or not Site Groups
- groupsToExclude: comma separated list of groups to export, if not present all the groups will be exported. IGNORED if groupsToExport is present
- usersToExclude: comma separated list of users to export, if not present all the users will be exported. IGNORED if usersToExport is present

####org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'webscript.org.alfresco.devops.exporters.rm-site-export.get' defined in class path resource [alfresco/module/repo-amp/context/service-context.xml]: Instantiation of bean failed; nested exception is java.lang.NoSuchFieldError: r$sfields
This exception seems to be caused by SpringLoaded, so if this exception is hit when running the SDK please remove the Spring Loaded agent