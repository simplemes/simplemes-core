<#--@formatter:off-->
<#assign title><@efTitle label="search.title"/></#assign>

<#include "../includes/header.ftl" />

<@efForm id="search" >
    <@efField field="query" maxLength=255 width="40em" label="searchQuery.label" value='${params.query!""}'/>
    <@efButtonGroup>
        <@efButton id='searchButton' label="searchButton.label" click="submitSearch()" />
    </@efButtonGroup>
</@efForm>
<script>
  function submitSearch() {
    var values = $$('search').getValues();
    var url = ef.addArgToURI('/search', 'query', values.query);
    // from/size
    window.location = url;
  }
  ef.focus("query");
  window.addEventListener("keydown", function(event){
    if (event.key=='Enter') {
      submitSearch();
    }
  });
</script>
<#if !searchStatus.configured>
  <div>
    <@efLookup key="searchNotConfigured.label"/>
    <a href="http://docs.simplemes.org/latest/eframe/guide.html#searching">Searching</a>
  </div>
</#if>
${searchStatus.status}
${searchStatus.localizedStatus}
${searchResult.query!""}
${params.query!""}

<#include "../includes/footer.ftl" />

