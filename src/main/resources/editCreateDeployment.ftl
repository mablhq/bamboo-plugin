[@ww.password
    showPassword="true"
    labelKey="createdeployment.restapikey.label"
    name="mablRestApiKey"
    required="true"
    onchange="mabl.populateDropdowns(event)"
/]
<div class="description">[@ww.text name='createdeployment.restapikey.description' /]</div>
[@s.select
    labelKey="createdeployment.environmentid.label"
    name="mablEnvironmentId"
    list="environmentsList"
    required="false"
    emptyOption="true"
/]
<div class="description">[@ww.text name='createdeployment.environmentid.description' /]</div>
[@s.select
    labelKey="createdeployment.applicationid.label"
    name="mablApplicationId"
    list="applicationsList"
    required="false"
    emptyOption="true"
/]
<div class="description">[@ww.text name='createdeployment.applicationid.description' /]</div>
[@s.select
    labelKey="createdeployment.planlabels.label"
    name="mablPlanLabels"
    list="planLabelsList"
    size="6"
    multiple="true"
    required="false"    
/]
<a style="position: relative; left: 4px;" onclick="mabl.clearPlanLabels(event)">[@ww.text name='createdeployment.planlabels.clear' /]</a>
<div class="description">[@ww.text name='createdeployment.planlabels.description' /]</div>
[@ww.textfield
    labelKey="createdeployment.mablBranch.label"
    name="mablBranch"
    required="false"
    
/]
<div class="description">[@ww.text name='createdeployment.mablBranch.description' /]</div>
<h5>[@ww.text name='createdeployment.proxy.settings' /]:</h5>
[@ww.textfield
	labelKey="createdeployment.proxyaddress.label"
	name="mablProxyAddress"
	required="false"
	onchange="mabl.populateDropdowns(event)"
/]
<div class="description">[@ww.text name='createdeployment.proxyaddress.description' /]</div>
[@ww.textfield
	labelKey="createdeployment.proxyusername.label"
	name="mablProxyUsername"
	required="false"
	onchange="mabl.populateDropdowns(event)"
/]
<div class="description">[@ww.text name='createdeployment.proxyusername.description' /]</div>
[@ww.password
	showPassword="true"
	labelKey="createdeployment.proxypassword.label"
	name="mablProxyPassword"
	required="false"
	onchange="mabl.populateDropdowns(event)"
/]
<div class="description">[@ww.text name='createdeployment.proxypassword.description' /]</div>
<script type="text/javascript">

var mabl =
{
    populateDropdowns: function(event) {
        var getEnvData = {ACTION : "environments",  
                          restApiKey : AJS.$("#mablRestApiKey").val(),
                          proxyAddress : AJS.$("#mablProxyAddress").val(),
                          proxyUsername : AJS.$("#mablProxyUsername").val(),
                          proxyPassword : AJS.$("#mablProxyPassword").val()};
        var url = "${req.contextPath}/plugins/servlet/configurator";
        AJS.$.post(url, getEnvData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablEnvironmentId"), "Select Environment");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$(), "#mablEnvironmentId"), "");

        var getAppData = {ACTION : "applications",
                          restApiKey : AJS.$("#mablRestApiKey").val(),
                          proxyAddress : AJS.$("#mablProxyAddress").val(),
                          proxyUsername : AJS.$("#mablProxyUsername").val(),
                          proxyPassword : AJS.$("#mablProxyPassword").val()};
        AJS.$.post(url, getAppData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablApplicationId"), "Select Application");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$("#mablApplicationId"), ""));

        var getLabelData = {ACTION : "labels",
                          	restApiKey : AJS.$("#mablRestApiKey").val(),
                          	proxyAddress : AJS.$("#mablProxyAddress").val(),
                          	proxyUsername : AJS.$("#mablProxyUsername").val(),
                          	proxyPassword : AJS.$("#mablProxyPassword").val()};
        AJS.$.post(url, getLabelData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablPlanLabels"));
        })
            .fail(mabl.helpers.clearDropdown(AJS.$("#mablPlanLabels")));
    },

    clearPlanLabels: function(event) {
      AJS.$("#mablPlanLabels option:selected").prop("selected", false);
    },

    helpers: {

        clearDropdown: function(dropdown, emptyMessage) {
            // Remove current options
            dropdown.html("");
            // Add the empty option with the empty message
            if(emptyMessage) {
                dropdown.append('<option value="">' + emptyMessage + '</option>');
            }
        },

        buildDropdown: function(result, dropdown, emptyMessage) {
            mabl.helpers.clearDropdown(dropdown, emptyMessage);
            // Check result isn't empty
            if(result !== "")
            {
                // Loop through each of the results and append the option to the dropdown
                AJS.$.each(result, function(k, v) {
                    // Labels don't have ids so need to use name
                    var id = v.id ? v.id : v.name;
                    dropdown.append('<option value="' + id + '">' + v.name + '</option>');
                });
            }
        }
    }
};
</script>