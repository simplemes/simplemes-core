<#assign title><@efTitle type='list'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<@efDefinitionList columns="product,title,lsnTrackingOption,lsnSequence,lotSize,masterRouting"/>
<@efPreloadMessages codes="cancel.label"/>

<#include "../includes/footer.ftl" />

