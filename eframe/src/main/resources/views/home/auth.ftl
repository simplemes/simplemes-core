<!DOCTYPE html>
<html lang="en">
<head>
    <#if errors??>
      <title>Login Failed</title>
    <#else>
      <title>Login ${errors!''}</title>
    </#if>
</head>
<body>
<#if errors??>
  <div id="errors" class="login-message">
    <span>Login Failed</span>
  </div>
</#if>
<form action="/login" method="POST">
  <ol>
    <li>
      <label for="username">Username</label>
      <input type="text" name="username" id="username"/>
    </li>
    <li>
      <label for="password">Password</label>
      <input type="password" name="password" id="password"/>
    </li>
    <li>
      <input type="submit" value="Login" id="submit"/>
    </li>
  </ol>
  <input id="target" name="target" type="hidden" value="${target!''}"/>
</form>
</body>
</html>
