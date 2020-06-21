<#--@formatter:off-->
<#assign title><@efTitle label="search.title"/></#assign>

<#include "../includes/header.ftl" />

<#--Use size as the height of the result form, min 15.-->
<#assign pgHeight=15/>
<#if params.size??>
  <#assign pgHeight=params.size?number/>
  <#if pgHeight < 15>
    <#assign pgHeight=15/>
  </#if>
</#if>

<@efForm id="search" height="${pgHeight}em">
    <@efField field="query" maxLength=255 width="40em" label="searchQuery.label" value='${params.query!""}'/>
    <@efButtonGroup>
        <@efButton id='searchButton' label="searchButton.label" click="submitSearch()" spacer="before after"/>
    </@efButtonGroup>
</@efForm>
<script>
  function submitSearch() {
    var values = $$('search').getValues();
    window.location = ef.addArgToURI('/search', 'query', values.query);
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
        <div class="search-result-header">
          <@efLookup key="searchResultSummary.label" arg1="${searchResult.totalHits}" arg2="${searchResult.elapsedTime}"/>
        </div>
          <#list searchResult.hits as hit>
            <div class="search-result-single">
              <a href="${hit.link!'/'}">${hit.displayValue}</a>
            </div>
          </#list>
          <br>
          <@efPager uri="/search?query=${params.query}" total="${searchResult.totalHits}" size="10" from="${params.from!'0'}"/>
      </#if>
    <div class="search-result-footer">
      <#if searchStatus.configured>
        <#if searchStatus.status=="red">
          <@efLookup key="searchServerStatus.label" arg1="${searchStatus.localizedStatus}"/>
        </#if>
        <#if searchStatus.status=="timeout">
          <@efLookup key="searchServerStatus.label" arg1="${searchStatus.localizedStatus}"/>
        </#if>
      <#else>
        <@efLookup key="searchNotConfigured.label"/>
        <a href="http://docs.simplemes.org/latest/eframe/guide.html#searching">Doc (Searching)</a>
      </#if>
    </div>
  </div>

<script>
  tk._moveElementToForm("bottomContent");
</script>


<#include "../includes/footer.ftl" />

