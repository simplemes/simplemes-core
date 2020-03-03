<#assign title><@efTitle type='show'/></#assign>

<#include "../../includes/header.ftl" />
<#include "../../includes/definition.ftl" />

<script>
  function release() {
    console.log('clicked');
  }
</script>

<@efForm id="show">
    <@efShow>
        <@efMenuItem id="release" key="release" onClick="release()"/>
        <@efMenuItem/>
    </@efShow>
</@efForm>

<#include "../../includes/footer.ftl" />

