Alfresco Demo Data
---

This module (Alfresco Repository AMP) loads some sample sites, users and groups into Alfresco at boot time; it can be used for demo purposes, but also for initial Repository configuration.

The project allows you to run Alfresco locally and create your content; following the steps below you will be able to simply export your data, update the project and (re)generate the AMP with the updated data that needs to be bootstrapped.

**IMPORTANT: this module MUST be used with an EMPTY repository.**

The module is composed by two Alfresco repo AMP:

- **alfresco-demo-data-repo-amp**: (*DEMO DATA BOOTSTRAP*) to import data - install this just where custom data have to be loaded
- **alfresco-demo-data-exporter-repo-amp**: (*EXPORTER*) to export Users/Groups and RM sites - install this just where data have to be exported

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

- `accounting`
- `engineering-projects`
- `executive-team`
- `hr-team`
- `marketing-site`
- `sales-contracts`
- `sales-enablement`
- `social-media`
- `support-case-files`
- `virtual-folders`


And a number of users and groups listed here:

- [Users](http://localhost:8080/share/page/console/admin-console/users#state=panel%3Dsearch%26search%3D%255C*)
- [Groups] (http://localhost:8080/share/page/console/admin-console/groups#state=panel%3Dsearch%26refresh%3Dfalse)

Sites & Authorities Update
---

In order to make any change on the importer:

- Download the project, perform the necessary changes there and and run ***mvn package*** to create a new AMP


Let's have a look on how to do it in more details:

--

##### Add a new Site

- It is first necessary to export the site from an existing Alfresco instance using the already existing [site-export Rest API](http://docs.alfresco.com/community/references/RESTful-SiteSite-exportGet.html) with admin user credentials

```
curl -u admin:admin http://localhost:8080/alfresco/s/api/sites/$SITE_NAME/export > site-export.zip
```
- Unzip the export file
- Create a folder into **demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/sites**  with the site name (it has to be the exact same shortname used by the Share url)
- Move the **Contents.acp** into the previously created folder (Other ACPs are not needed)
- No other configuration needed



##### Add Authorities
- It is first necessary to export the authorities from an existing Alfresco instance using a custom Authorities export Rest API (included in this repo-amp) with admin user credentials

```
curl -u admin:admin localhost:8080/alfresco/service/api/people-groups/export > authorities-export.zip
```
- Unzip the export file
- Move the **People.acp**,**Users.acp**,**Groups.json** into **demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/authorities**
- No other configuration needed
                                                                                                                              
Repo Folders Update
---
Using the *alfresco-demo-data-exporter-repo-amp* it is possible to export folders inside the **Company Home**.

The url to export a folder is

```
http://<host>:<port>/alfresco/service/api/file-folder/export?path=/Data Dictionary/Scripts/custom-scripts
```

This will download an ACP with the folder and its content

The full url of the webscript is:

```
<url>/api/file-folder/export?path={path}&crawlSelf={crawlSelf}&crawlChildNodes={crawlChildNodes}&crawlContent={crawlContent}&crawlAssociations={crawlAssociations}</url>
```

So it is possible to set:

- **path**: path of the folder/file to export
- **crawlSelf**: (true/false) whether to include the element passed in the path or not (default: true)
- **crawlChildNodes**: (true/false) whether to include the child nodes of the element passed in the path or not (default: true)
- **crawlContent**: (true/false) whether to include the contents or not (default: true)
- **crawlAssociations**: (true/false) whether to include the associations or not (default: true)

To import a file or folder is necessary to move the acp into the desired folder of the repo.
Example: to place some javascripts inside the folder Company Home\Data Dictionary\Scripts just place the ACP containing the scripts into the folder of the project **demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/repo/app:company_home/app:dictionary/app:scripts**.

**Note:** Inside the repo folder of the project all the folders MUST have the alfresco name following the NodeBrowser PATH naming. ( e.g. /app:company_home/app:dictionary/app:scripts)

                                                                                                                              
Add a new Model dinamically
---
Just place your xml model definition into 

**demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/models/xml**

and the labels into 

**demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/models/labels**

*No bean configuration is needed!!*

Add a new Workflow dinamically
---
Just place your xml worfklow definition into 

**demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/workflows/workflowDefinitions**

the models associated into:

**demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/workflows/models**

and the labels into 

**demo-data/alfresco-demo-data/alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/dynamic/workflows/labels**

*No bean configuration is needed!!*

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
To export authorities the **alfresco-demo-data-export-repo-amp** AMP should be used in your Alfresco instance.


#### More about the Authorities Exporter
The url of the webscript is:

```
  <url>/api/people-groups/export?usersToExport={usersToExport}&amp;groupsToExport={groupsToExport}&amp;excludeSiteGroups={excludeSiteGroups}&amp;groupsToExclude={groupsToExclude}&amp;usersToExclude={usersToExclude}</url>
```

In the url you can set:

- **usersToExport**: comma separated list of users to export, if not present all the users will be exported
- **groupsToExport**: comma separated list of groups to export, if not present all the groups will be exported
- **excludeSiteGroups**: boolean (TRUE/FALSE), determines whether to exclude or not Site Groups
- **groupsToExclude**: comma separated list of groups to export, if not present all the groups will be exported. IGNORED if groupsToExport is present
- **usersToExclude**: comma separated list of users to export, if not present all the users will be exported. IGNORED if usersToExport is present
