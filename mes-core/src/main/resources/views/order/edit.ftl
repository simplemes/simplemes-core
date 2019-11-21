<#assign title><@efTitle type='edit'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<@efForm id="edit">
    <@efEdit qtyReleased@readOnly=true qtyInQueue@readOnly=true qtyInWork@readOnly=true qtyDone@readOnly=true/>
</@efForm>

<#include "../includes/footer.ftl" />

