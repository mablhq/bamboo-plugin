![mabl logo](https://avatars3.githubusercontent.com/u/25963599?s=100&v=4)
# mabl Bamboo Plugin

This plugin allows easy launching of [mabl](https://www.mabl.com) journeys as a step in your Bamboo build. Your Bamboo build success or failure will be dependant on the success or failure of your mabl test deployment event.

## Using
1. Within Bamboo, from any plan's `Configure tasks` page, add a new task
2. Select `A Mabl Deployment` task
3. Input the ApiKey and unselect the field, this will populate the Environment and Application drop-downs
4. Select at least 1 environment or application to proceed
4. Hit Save
5. Enable the task and the plan

<img src="https://github.com/mablhq/bamboo-plugin/raw/master/src/main/resources/images/BambooTaskSelectionV2.png" alt="Select Mabl Deployment" width="40%"/>
<img src="https://github.com/mablhq/bamboo-plugin/raw/master/src/main/resources/images/BambooTaskConfigurationV3.png" alt="Input Configuration" width="40%"/>

Now builds from this plan will trigger Mabl test plan executions of the chosen configuration.

### Proxy Settings

##### Global proxy settings
If you have a proxy enabled for your bamboo installation, this plugin respects outbound proxy settings you have configured for your server as described in [Atlassian's instructions.](https://confluence.atlassian.com/kb/how-to-configure-outbound-http-and-https-proxy-for-your-atlassian-application-834000120.html)

##### Plugin specific proxy settings
For installations that require only the plugin's requests be sent through a proxy or through a different proxy server than the bamboo installations default proxy, you can configure the settings within the deployment task configuration. Basic authentication is supported if the proxy server requires credentials to handle the requests. 

##### Traffic routing
Note that once you configure a proxy either on a global level or inside the _mabl for Bamboo_ task, the plugin will
send all HTTP calls through the proxy. For example, this includes calls made to retrieve the list of environments when
configuring the task.

## Installation

### From the marketplace
See the [Atlassian Docs](https://marketplace.atlassian.com/apps/1219102/mabl-deployment?hosting=server&tab=installation) about installing from the marketplace.

### Building from source and running locally
Install the [Atlassian SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/set-up-the-atlassian-plugin-sdk-and-build-a-project/)
1. Clone this repo && cd into it
2. Build this repo by running `atlas-mvn package`
3. `atlas-run` and visit the provided url
  ```
  [INFO] bamboo started successfully in 119s at http://$USER:6990/bamboo
  [INFO] Type Ctrl-D to shutdown gracefully
  [INFO] Type Ctrl-C to exit
  ```
### Building from source and running in docker container  
Grab [this container](https://hub.docker.com/r/atlassian/bamboo-server/)
`docker pull atlassian/bamboo-server`
Run these commands
```
docker volume create --name bambooVolume
docker run -v bambooVolume:/var/atlassian/application-data/bamboo --name="bamboo" --init -d -p 54663:54663 -p 8085:8085 atlassian/bamboo-server
docker start bamboo
```

You will need to create a Bamboo trial license [here](https://my.atlassian.com/license/evaluation), which you will provide the server below.

Visit `localhost:8085` to interact with Bamboo.

  
### IDE setup
You'll need to [follow here](https://community.developer.atlassian.com/t/configure-idea-to-use-the-sdk/10610) to setup your IDE to use atlas-mvn

### Testing
You'll want to test this in the context of `atlas-run` and in a container as the way urls are built are different between the two.

### Deployment
1. Merge code into master and push
2. Run `atlas-mvn clean install`
3. Run `atlas-mvn release:prepare` This will update pom.xml with new version and tag the relase with current version minus `-SNAPSHOT`
4. Run `atlas-mvn release:perform`

### Manual Deployment
1. Merge code into master and push
2. Run `atlas-mvn clean install` 
3. Upload the resulting `target/bamboo-plugin-$VERSION.jar` to the [atlassian marketplace](https://marketplace.atlassian.com/manage/apps/1219102/versions)
Make sure your version doesn't include `-SNAPSHOT` if you're uploading manually.
Uploading will require an admin to the mablhq Atlassian vendor account. 
  
