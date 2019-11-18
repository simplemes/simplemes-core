<#assign title><@efTitle type='main' label='denied.title'/></#assign>
<#include "../includes/header.ftl" />

<h2>Access Denied</h2>

<div id="errors" class="error-message message">
    ${_flash!""}
</div>

<#include "../includes/footer.ftl" />

