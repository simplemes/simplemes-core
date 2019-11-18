<#assign title><@efTitle type='main' label='error.title'/></#assign>
<#include "../includes/header.ftl" />

<h2><@efTitle type='main' label='error.title'/></h2>

<div id="errors" class="error-message message">
    ${_flash!""}
</div>

${_flashDetails!"NONE"}

<#include "../includes/footer.ftl" />

