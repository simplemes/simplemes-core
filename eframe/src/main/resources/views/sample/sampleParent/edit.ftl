<#assign title><@efTitle type='edit'/></#assign>

<#include "../../includes/header.ftl" />
<#include "../../includes/definition.ftl" />

<@efForm id="edit">
    <@efEdit sampleChildren@label="" sampleChildren@sequence@default="tk.findMaxGridValue(gridName, 'sequence')+10"
    allFieldsDomain@onChange="eframe.displayMessage({ info: ['allFieldsDomain changed'] })"
    />
</@efForm>

<#include "../../includes/footer.ftl" />

