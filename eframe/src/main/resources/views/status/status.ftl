<#--@formatter:off-->
<#assign title><@efLookup key='status.label'/></#assign>
<#include "../includes/header.ftl" />

<#if checkRequests??>
  <script>
    function getMetricFromJSON(data,statisticName) {
      //console.log(data.measurements);
      if (data.measurements) {
        var countElements = data.measurements.filter(function(it) {return it.statistic==statisticName});
        //console.log(countElements);
        if (countElements.length>0) {
          return countElements[0].value;
        }
      }
    }

    function updateCounts() {
      ef.get("/metrics/http.server.requests", {},
        function (responseText) {
          var data = JSON.parse(responseText);
          var value = getMetricFromJSON(data,'COUNT');
          if (value) {
            ef._setInnerHTML('httpRequestCount',ef._formatBigNumber(value),value);
          } else {
            ef._setInnerHTML('httpRequestCount','??','Request Failed');
          }
          var max = getMetricFromJSON(data,'MAX');
          if (max) {
            max = Math.round(max*1000);
            ef._setInnerHTML('httpRequestMax',max);
          } else {
            ef._setInnerHTML('httpRequestMax','??','Request Failed');
          }
        }
      );
    }
    setTimeout(updateCounts,1000);   
    setInterval(updateCounts,30000);
  </script>

</#if>

<div class="webix_view webix_layout_space webix_form" style="margin-left:60px">
  <h2><@efLookup key="status.label"/></h2>
  <div class="indent">
    <h3><@efLookup key="requests.label"/></h3>
      <div class="indent">
        <@efLookup key="totalRequests.label"/>:&nbsp;&nbsp;<span id="httpRequestCount">?</span>
        (<span id="httpRequestMax">?</span>&nbsp;<@efLookup key="totalRequestsMax.label"/>)
      </div>
    <h3><@efLookup key="modules.label"/></h3>
    <div class="indent">
      <table >
          <#list modules as module>
            <tr><td>${module}</td></tr>
          </#list>
      </table>
    </div>
  </div>
</div>
<#include "../includes/footer.ftl" />

