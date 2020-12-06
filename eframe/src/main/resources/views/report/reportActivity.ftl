<#--@formatter:off-->
<script>
  <#assign panel = "${params._panel}"/>
  <#assign variable = "${params._variable}"/>

  ${variable}.clicked = function(reportLoc) {
    <#if newWindow>
      window.open(reportLoc);
    <#else>
      window.location = reportLoc;
    </#if>

    dashboard.finished("${params._panel}");
  }
  <@efForm id="reportActivity" dashboard="true" >
    <#list rows as list>
      <@efButtonGroup>
        <#list list as report>
          <@efButton id="${report.name}Button" label='${report.name}' click='${variable}.clicked("${report.uri}"+"${otherParams}")'/>
        </#list>
      </@efButtonGroup>
    </#list>
  </@efForm>


</script>
