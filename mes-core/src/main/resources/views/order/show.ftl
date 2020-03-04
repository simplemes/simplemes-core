<#--@formatter:off-->
<#assign title><@efTitle type='show'/></#assign>

<#include "../includes/header.ftl" />
<#include "../includes/definition.ftl" />

<script>
  function release() {
    console.log('clicked release');
    ef.submitForm(undefined, '/order/releaseUI',{uuid: '${order.uuid}'})
  }
</script>
<@efForm id="show">
  <@efShow>
    <@efMenuItem id="release" key="release" onClick="release()"/>
    <@efMenuItem/>
  </@efShow>
</@efForm>

<#include "../includes/footer.ftl" />

