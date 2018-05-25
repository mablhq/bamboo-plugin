[@ww.textfield labelKey="createdeployment.restApiKey.label" name="restApiKey" required='true' onchange="helloworld(event)" /]

[@s.select
    updateOn=restApiKey
    labelKey='createdeployment.environmentId.label'
    name="environmentId"
    list="environmentsList"
    required="false"
/]
[@s.select
    showOn=restApiKey
    labelKey='createdeployment.applicationId.label'
    name="applicationId"
    list="applicationsList"
    required="false"
/]

<script type="text/javascript">
    function helloworld(event) {
        var getData = {"ACTION" : "environments",  "restApiKey" : event.target.value };
        var url = "/bamboo/plugins/servlet/configurator";
        AJS.$.get(url, getData, function(data) {
            helpers.buildDropdown(data, AJS.$('#environmentId'), 'Select an Environment');
        })
        .fail(function(data){console.log(data);})

        getData = {"ACTION" : "applications",  "restApiKey" : event.target.value };
        AJS.$.get(url, getData, function(data) {
            helpers.buildDropdown(data, AJS.$('#applicationId'), 'Select an Application');
        })
        .fail(function(data){console.log(data);})
    }

var helpers =
{
    buildDropdown: function(result, dropdown, emptyMessage)
    {
        console.log("inside buildDropown");
        // Remove current options
        dropdown.html('');
        // Add the empty option with the empty message
        dropdown.append('<option value="">' + emptyMessage + '</option>');
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