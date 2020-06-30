<#--@formatter:off-->
<#assign title><@efTitle label="searchAdmin.title"/></#assign>

<#include "../includes/header.ftl" />

<@efForm id="search">
    <@efHTML height="5%">
        <div>Some HTML</div>
    </@efHTML>
    <@efButtonGroup>
        <@efButton id='searchResetCounters' label="searchResetCounters.label" click="resetCounters()" spacer="before after"/>
        <@efButton id='searchRebuildIndices' label="searchRebuildIndices.label" click="rebuildIndices()" spacer="before after"/>
    </@efButtonGroup>
</@efForm>
<script>
  function resetCounters() {
    var values = $$('search').getValues();
    window.location = ef.addArgToURI('/search', 'query', values.query);
  }
  function rebuildIndices() {
    var values = $$('search').getValues();
    window.location = ef.addArgToURI('/search', 'query', values.query);
  }
</script>
<#include "../includes/footer.ftl" />

