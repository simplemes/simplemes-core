<#assign title><@efTitle label="Demo Data Loader"/></#assign>
<#assign head>
  <link rel="stylesheet" href="<@efAsset uri="/assets/logging.css"/>" type="text/css">
</#assign>

<#include "../includes/header.ftl" />


<div id="loggerDiv"></div>

<#list list as record>
  <div class="search-result-single">
    <a href="${record.uri!'/'}">${record.name} ${record.count}/${record.possible} Loaded</a>
  </div>
</#list>



<#include "../includes/footer.ftl" />

