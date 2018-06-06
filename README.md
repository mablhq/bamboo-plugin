![mabl logo](https://avatars3.githubusercontent.com/u/25963599?s=100&v=4)
# mabl Bamboo Plugin

This plugin allows easy launching of [mabl](https://www.mabl.com) journeys as a step in your Bamboo build. Your Bamboo build success or failure will be dependant on the success or failure of your mabl test deployment event.

## Using
1. From any plan's 'Configre tasks' page, add a new task
2. Select the 'Mabl Deployment' task
3. Input the ApiKey and unselect the field, this will populate the Environment and Application drop-downs
4. Select at least 1 environment or application to proceed
4. Hit Save
5. Enable the task and the plan

<img src="https://github.com/mablhq/bamboo-plugin/raw/master/src/main/resources/images/BambooTaskSelectionV1.png" alt="Select Mabl Deployment" width="40%"/>
<img src="https://github.com/mablhq/bamboo-plugin/raw/master/src/main/resources/images/BambooTaskSelectionV2.png" alt="Input Configuration" width="40%"/>

Now builds from this plan will trigger Mabl test plan executions of the chosen configuration.

## Installation

### From the marketplace
See the [Atlassian Docs](https://marketplace.atlassian.com/apps/1219102/mabl-deployment?hosting=server&tab=installation) about installing from the marketplace.

### Building from source
Install the [Atlassian SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/set-up-the-atlassian-plugin-sdk-and-build-a-project/)
1. Clone this repo && cd into it
2. Build this repo by running `atlas-mvn package`
3. `atlas-run` and visit the provided url
  ```
  [INFO] bamboo started successfully in 119s at http://$USER:6990/bamboo
  [INFO] Type Ctrl-D to shutdown gracefully
  [INFO] Type Ctrl-C to exit
  ```
### IDE setup
You'll need to [follow here](https://community.developer.atlassian.com/t/configure-idea-to-use-the-sdk/10610) to setup your IDE to use atlas-mvn

### Deployment
1. Merge code into master and push
2. Run `atlas-mvn release:prepare`
3. Upload the resulting `target/bamboo-plugin-$VERSION.jar` to the [atlassian marketplace](https://marketplace.atlassian.com/manage/apps/1219102/versions)

Uploading will require an admin to the mablhq Atlassian vendor account. 
  
