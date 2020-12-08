<#assign title><@efTitle type='edit'/></#assign>

<#include "../../includes/header.ftl" />
<#include "../../includes/definition.ftl" />

<@efForm id="edit">
    <@efEdit product@suggest="/order/suggestOrder?workCenter=WC1"/>
</@efForm>

<#include "../../includes/footer.ftl" />

