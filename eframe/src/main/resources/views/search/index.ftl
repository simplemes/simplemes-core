<#--@formatter:off-->
<#assign title><@efTitle label="search.title"/></#assign>

<#include "../includes/header.ftl" />


<@efForm id="search" height="85%">
    <@efField field="query" maxLength=255 width="40em" label="searchQuery.label" value='${params.query!""}'/>
    <@efButtonGroup>
        <@efButton id='searchButton' label="searchButton.label" click="submitSearch()" spacer="before after"/>
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
  <div id="bottomContent" class="search-results" style="display:none">
      <#if params.query??>
      <#-- TODO: Layout, Localize the displayed text and paginate.-->
        <div class="search-result-header">
          ${searchResult.totalHits} results (${searchResult.elapsedTime}ms).
        </div>
          <#list searchResult.hits as hit>
            <div class="search-result-single">
              <a href="${hit.link!'/'}">${hit.displayValue}</a>
            </div>
          </#list>

      </#if>
    <div class="search-result-header">
      <#if searchStatus.configured>
        Server Status: ${searchStatus.localizedStatus}
      <#else>
        <@efLookup key="searchNotConfigured.label"/>
        <a href="http://docs.simplemes.org/latest/eframe/guide.html#searching">Searching</a>
      </#if>
    </div>
  </div>

<script>
  tk._moveElementToForm("bottomContent");
</script>


<#include "../includes/footer.ftl" />

