<#assign title><@efTitle type='create'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<@efForm id="create">
    <@efCreate qtyReleased@readOnly=true qtyInQueue@readOnly=true qtyInWork@readOnly=true qtyDone@readOnly=true/>
</@efForm>

<#include "../includes/footer.ftl" />

