[@ww.password
    showPassword="true"
    labelKey="createdeployment.restapikey.label"
    name="mablRestApiKey"
    required="true"
    onchange="populateDropdowns(event)"
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

<script type="text/javascript">
    function populateDropdowns(event) {
        var getEnvData = {ACTION : "environments",  restApiKey : event.target.value };
        var url = "/bamboo/plugins/servlet/configurator";
        AJS.$.get(url, getEnvData, function(data) {
            helpers.buildDropdown(data, AJS.$("#mablEnvironmentId"), "Select Environment");
        })
        .fail(helpers.clearDropdown(AJS.$(), "#mablEnvironmentId"));

        var getAppData = {ACTION : "applications",  restApiKey : event.target.value };
        AJS.$.get(url, getAppData, function(data) {
            helpers.buildDropdown(data, AJS.$("#mablApplicationId"), "Select Application");
        })
        .fail(helpers.clearDropdown(AJS.$("#mablApplicationId"), ""));
    }

var helpers =
{

    clearDropdown: function(dropdown, emptyMessage) {
        // Remove current options
        dropdown.html("");
        // Add the empty option with the empty message
        dropdown.append('<option value="">' + emptyMessage + '</option>');
    },

    buildDropdown: function(result, dropdown, emptyMessage)
    {
        helpers.clearDropdown(dropdown, emptyMessage);
        // Check result isn't empty
        if(result !== "")
        {
            // Loop through each of the results and append the option to the dropdown
            AJS.$.each(result, function(k, v) {
                dropdown.append('<option value="' + v.id + '">' + v.name + '</option>');
            });
        }
    }
}
</script>