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
    multiple="true"
    required="false"
    emptyOption="false"
/]
<script type="text/javascript">

var mabl =
{
    populateDropdowns: function(event) {
        var getEnvData = {ACTION : "environments",  restApiKey : event.target.value };
        var url = "${req.contextPath}/plugins/servlet/configurator";
        AJS.$.get(url, getEnvData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablEnvironmentId"), "Select Environment");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$(), "#mablEnvironmentId"));

        var getAppData = {ACTION : "applications",  restApiKey : event.target.value };
        AJS.$.get(url, getAppData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablApplicationId"), "Select Application");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$("#mablApplicationId"), ""));

        var getLabelData = {ACTION : "labels",  restApiKey : event.target.value };
        AJS.$.get(url, getLabelData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablPlanLabels"));
        })
            .fail(mabl.helpers.clearDropdown(AJS.$("#mablPlanLabels"), ""));
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
                    var id = v.id ? v.id : v.name;
                    dropdown.append('<option value="' + id + '">' + v.name + '</option>');
                });
            }
        }
    }
};
</script>