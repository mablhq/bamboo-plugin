[@ww.password
    showPassword="true"
    labelKey="createdeployment.restapikey.label"
    name="mablRestApiKey"
    required="true"
    onchange="mabl.populateDropdowns(event)"
/]

[@s.select
    labelKey="createdeployment.environmentid.label"
    name="mablEnvironmentId"
    list="environmentsList"
    required="false"
    emptyOption="true"
/]
[@s.select
    labelKey="createdeployment.applicationid.label"
    name="mablApplicationId"
    list="applicationsList"
    required="false"
    emptyOption="true"
/]
[@s.select
    labelKey="createdeployment.planlabels.label"
    name="mablPlanLabels"
    list="planLabelsList"
    size="6"
    multiple="true"
    required="false"
/]
<a style="position: relative; bottom: 12px; left: 4px;" onclick="mabl.clearPlanLabels(event)">clear labels</a>
<h5>Optional proxy settings:</h5>
[@ww.textfield
	labelKey="createdeployment.proxyaddress.label"
	name="mablProxyAddress"
	required="false"
	onchange="mabl.populateDropdowns(event)"
/]
[@ww.textfield
	labelKey="createdeployment.proxyusername.label"
	name="mablProxyUsername"
	required="false"
	onchange="mabl.populateDropdowns(event)"
/]
[@ww.password
	showPassword="true"
	labelKey="createdeployment.proxypassword.label"
	name="mablProxyPassword"
	required="false"
	onchange="mabl.populateDropdowns(event)"
/]
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