<#assign title><@efTitle type='list'/></#assign>

<#include "../../includes/header.ftl" />

<#include "../../includes/definition.ftl" />


<@efDefinitionList id="rmaGrid" columns="rma,status,product,qty,returnDate,rmaType,rmaSummary"/>

<@efPreloadMessages codes="cancel.label"/>

<#include "../../includes/footer.ftl" />

