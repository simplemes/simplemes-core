<#--@formatter:off-->
<#assign title><@efTitle type='main' label='login.title'/></#assign>
<#include "../includes/header.ftl" />
<#if params.failed??>
  <div id="errors" class="login-message">
    <span><@efLookup key="login.failed.message"/></span>
  </div>
</#if>
<form id="dummy" action="/login" method="POST">
  <@efForm id="loginForm">
    <@efField field="User.userName" id="username" label="user.label" attributes='autocomplete:"username"'/>
    <@efField field="User.password" id="password" label="password.label" type="password"/>
      <@efButtonGroup spacerWidth="25%">
        <@efButton id="login" label='login.label' click="submitLogin()"/>
      </@efButtonGroup>
</@efForm>
</form>
<script>
  ef.focus("username");
  function submitLogin() {
    ef.submitForm('loginForm','/login')
  }
  // Attach the key handler to the whole page support the enter key
  document.onkeydown = function keyEventHandler(event) {
    if (event.key == "Enter") {
      submitLogin();
      event.stopPropagation();
      return false;
    }
  };

</script>

<#include "../includes/footer.ftl" />
