<#--@formatter:off-->
<#assign title><@efTitle label='reportFilter.title'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<@efForm id="filter" fieldDefinitions="reportFields">
    <@efButtonGroup>
        <@efButton label="reportFilter.update.label" id="updateFilter"
                   click="ef.submitForm('filter','/report/filterUpdate', {loc:'${loc}'})"/>
    </@efButtonGroup>
</@efForm>
<@efPreloadMessages codes="cancel.label"/>


<script>

  function setReadonlyDates(value) {
    // Sets the start/end dateTime fields to read only unless the reportTimeInterval is CUSTOM.
    var dateDisabled = !(value == 'CUSTOM_RANGE');
    if (dateDisabled) {
      $$('startDateTime').disable();
      $$('endDateTime').disable();
    } else {
      $$('startDateTime').enable();
      $$('endDateTime').enable();
    }
  }

  // Wait for the drop-down to be displayed before attaching the change handler.
  setTimeout(function () {
    if ($$('reportTimeInterval')) {
      setReadonlyDates($$('reportTimeInterval').getValue());
      $$('reportTimeInterval').attachEvent("onChange", function (newValue, oldValue) {
        setReadonlyDates(newValue);
      });
    }
  }, 200);


</script>


<#include "../includes/footer.ftl" />

