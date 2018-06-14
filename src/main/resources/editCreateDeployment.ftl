[@ww.password
    showPassword="true"
    labelKey="createdeployment.restapikey.label"
    name="mablRestApiKey"
    required="true"
    onchange="mabl.populateDropdowns(event)"
/]

[@s.select
    labelKey="createdeployment.uri.label"
    name="mablUri"
    list="urisList"
    required="false"
    onchange="mabl.showHideUriCustom(event)"
/]

[@ww.textfield
    labelKey="createdeployment.customuri.label"
    name="mablCustomUri"
    required="false"
/]

[@s.select
    labelKey="createdeployment.environment.label"
    name="mablEnvironment"
    list="environmentsList"
    required="false"
    onchange="mabl.showHideEnvironmentCustom(event)"
/]

[@ww.textfield
    labelKey="createdeployment.customenvironment.label"
    name="mablCustomEnvironment"
    required="false"
/]

[@s.select
    labelKey="createdeployment.application.label"
    name="mablApplication"
    list="applicationsList"
    required="false"
    onchange="mabl.showHideApplicationCustom(event)"
/]

[@ww.textfield
    labelKey="createdeployment.customapplication.label"
    name="mablCustomApplication"
    required="false"
/]

[@ww.textarea
    labelKey="createdeployment.plantags.label"
    name="mablPlanTags"
    required="false"
    rows="6"
/]

<script type="text/javascript">


var mabl =
{
    populateDropdowns: function(event) {
        var getUriData = {ACTION : "uris",  restApiKey : event.target.value };
        var url = "/bamboo/plugins/servlet/configurator";
        AJS.$.get(url, getUriData, function(data) {
            mabl.helpers.buildUriDropdown(data, AJS.$("#mablUri"), "Select Uri");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$(), "#mablUri"));

        var getEnvData = {ACTION : "environments",  restApiKey : event.target.value };
        var url = "/bamboo/plugins/servlet/configurator";
        AJS.$.get(url, getEnvData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablEnvironment"), "Select Environment");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$(), "#mablEnvironment"));

        var getAppData = {ACTION : "applications",  restApiKey : event.target.value };
        AJS.$.get(url, getAppData, function(data) {
            mabl.helpers.buildDropdown(data, AJS.$("#mablApplication"), "Select Application");
        })
        .fail(mabl.helpers.clearDropdown(AJS.$("#mablApplication"), ""));
    },

    showHideUriCustom: function(event) {
        mabl.helpers.showHide("#fieldArea_mablCustomUri", event.target.value);
    },

    showHideEnvironmentCustom: function(event) {
        mabl.helpers.showHide("#fieldArea_mablCustomEnvironment", event.target.value);
    },

    showHideApplicationCustom: function(event) {
        mabl.helpers.showHide("#fieldArea_mablCustomApplication", event.target.value);
    },

    helpers: {

        clearDropdown: function(dropdown, emptyMessage) {
            // Remove current options
            dropdown.html("");
            // Add the empty option with the empty message
            dropdown.append('<option value="">' + emptyMessage + '</option>');
            dropdown.append('<option value="_mabl_input_custom">Input Custom Value</option>');
        },

        buildDropdown: function(result, dropdown, emptyMessage) {
            mabl.helpers.clearDropdown(dropdown, emptyMessage);
            if(result !== "") {
                AJS.$.each(result, function(k, v) {
                    dropdown.append('<option value="' + v.name + '">' + v.name + '</option>');
                });
            }
        },

        buildUriDropdown: function(result, dropdown, emptyMessage) {
            mabl.helpers.clearDropdown(dropdown, emptyMessage);
            if(result !== "") {
                AJS.$.each(result, function(k, v) {
                    dropdown.append('<option value="' + v.uri + '">' + v.uri + '</option>');
                });
            }
        },

        showHide: function(field, value) {
            if (value == "_mabl_input_custom") {
                AJS.$(field).show();
            } else {
                AJS.$(field).hide();
            }
        }
    }
};

mabl.helpers.showHide("#fieldArea_mablCustomUri", AJS.$("#mablUri").val());
mabl.helpers.showHide("#fieldArea_mablCustomEnvironment", AJS.$("#mablEnvironment").val());
mabl.helpers.showHide("#fieldArea_mablCustomApplication", AJS.$("#mablApplication").val());

</script>