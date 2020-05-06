<@efPreloadMessages codes="remove.label,cancel.label"/>

<@efForm id="removeComponent" dashboard="true">
    <@efHTML spacer="before" width="40%">
      <ul>
          <#list components as i>
            <li id="removeCompText${i?counter}"><input type="checkbox" checked id="removeComp${i?counter}"> ${i}</input>
            </li>
          </#list>
      </ul>
    </@efHTML>
</@efForm>
