<#assign title><@efTitle type='dashboard' category="${params.category!'OPERATOR'}" dashboard="${params.dashboard!}"/></#assign>
<#assign head>
  <script src="<@efAsset uri="/assets/dashboard.js"/>" type="text/javascript"></script>
</#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />


<@efDashboard category="${params.category!'OPERATOR'}" dashboard="${params.dashboard!}" />

<#include "../includes/footer.ftl" />

