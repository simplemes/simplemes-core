<#--@formatter:off-->
<#assign title><@efLookup key='home.label'/></#assign>
<#include "../includes/header.ftl" />

<div id="w" class="title"></div>
  <@efForm id="main" >
    <@efHTML height="5%">Setup </@efHTML>
    <@efButtonGroup spacerWidth="5%">
      <@efButton id="ProductButton" label='Products' click="window.location='/product'"/>
      <#if !_loggedIn>
          <@efButton id="Login" label='Login' click="window.location='/login/auth'" css="caution-button" spacer="before"/>
      </#if>
    </@efButtonGroup>
    <@efButtonGroup spacerWidth="5%">
      <@efButton id="OrderButton" label='Orders' click="window.location='/order'"/>
      <@efButton id="GlobalSearchButton" label='Search' click="window.location='/search'"/>
    </@efButtonGroup>
    <@efHTML height="5%">Production</@efHTML>
    <@efButtonGroup spacerWidth="5%">
      <@efButton id="Dashboard1Button" label='Operator Dashboard' click="window.location='/dashboard?category=OPERATOR'"/>
    </@efButtonGroup>
    <@efButtonGroup spacerWidth="5%">
      <@efButton id="Dashboard2Button" label='Manager Dashboard' click="window.location='/dashboard?category=MANAGER'"/>
    </@efButtonGroup>
    <@efHTML height="5%">Admin</@efHTML>
    <@efButtonGroup spacerWidth="5%">
      <@efButton id="LoggingButton" label='Logging' click="window.location='/logging'"/>
      <@efButton id="UserButton" label='Users' click="window.location='/user'"/>
    </@efButtonGroup>
  </@efForm>
<#include "../includes/footer.ftl" />

