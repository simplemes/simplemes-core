<#assign title><@efTitle type='create'/></#assign>

<#include "../../includes/header.ftl" />
<#include "../../includes/definition.ftl" />

<@efForm id="create">
    <@efCreate product@suggest="/order/suggestOrder?workCenter=WC_XYZZY"/>
</@efForm>

<#include "../../includes/footer.ftl" />

