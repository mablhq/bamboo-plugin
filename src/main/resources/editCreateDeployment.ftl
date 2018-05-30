[@ww.password labelKey="createdeployment.restApiKey.label" name="restApiKey" required='true' onchange="populateDropdowns(event)" /]

[@s.select
    updateOn=restApiKey
    labelKey='createdeployment.environmentId.label'
    name="environmentId"
    list="environmentsList"
    required="false"
    emptyOption="true"
/]
[@s.select
    showOn=restApiKey
    labelKey='createdeployment.applicationId.label'
    name="applicationId"
    list="applicationsList"
    required="false"
    emptyOption="true"
/]

<script type="text/javascript">
    function populateDropdowns(event) {
        var getData = {"ACTION" : "environments",  "restApiKey" : event.target.value };
        var url = "/bamboo/plugins/servlet/configurator";
        AJS.$.get(url, getData, function(data) {
            helpers.buildDropdown(data, AJS.$('#environmentId'), 'Select Environment');
        })
        .fail(helpers.clearDropdown(AJS.$('#environmentId'), ''));

        getData = {"ACTION" : "applications",  "restApiKey" : event.target.value };
        AJS.$.get(url, getData, function(data) {
            helpers.buildDropdown(data, AJS.$('#applicationId'), 'Select Application');
        })
        .fail(helpers.clearDropdown(AJS.$('#applicationId'), ''));
    }

var helpers =
{

    clearDropdown: function(dropdown, emptyMessage) {
        // Remove current options
        dropdown.html('');
        // Add the empty option with the empty message
        dropdown.append('<option value="">' + emptyMessage + '</option>');
    },

    buildDropdown: function(result, dropdown, emptyMessage)
    {
        helpers.clearDropdown(dropdown, emptyMessage);
        // Check result isnt empty
        if(result != '')
        {
            // Loop through each of the results and append the option to the dropdown
            AJS.$.each(result, function(k, v) {
                dropdown.append('<option value="' + v.id + '">' + v.name + '</option>');
            });
        }
    }
}
</script>