<#assign title><@efTitle type='create'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<@efForm id="create">
    <@efCreate fields@label="" fields@sequence@default="tk.findMaxGridValue(gridName, 'sequence')+10"/>
</@efForm>

<#include "../includes/footer.ftl" />

