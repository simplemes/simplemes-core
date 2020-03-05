<#assign title><@efTitle type='list'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<@efDefinitionList columns="userName,displayName,enabled,accountExpired,accountLocked,passwordExpired,email,authoritySummary"/>
<@efPreloadMessages codes="cancel.label"/>

<#include "../includes/footer.ftl" />

