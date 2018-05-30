# bamboo-plugin
A mabl bamboo plugin

## Using
1. From any plan's 'Configre tasks' page, add a new task.
2. Select the 'Mabl Deployment' task
3. Input the ApiKey, EnvironmentId, and ApplicationId for your deployment
4. Hit Save
5. Enable the task and the plan.

<img src="https://github.com/mablhq/bamboo-plugin/raw/master/src/main/resources/images/BambooTaskSelectionV1.png" alt="Select Mabl Deployment" width="40%"/>
<img src="https://github.com/mablhq/bamboo-plugin/raw/master/src/main/resources/images/BambooTaskConfigurationV1.png" alt="Input Configuration" width="40%"/>

Now builds from this plan will trigger Mabl test plan executions of the chosen configuration.

## Installation

### Installing Locally
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
