<script>

  <@efPreloadMessages codes="ok.label,cancel.label,
                             definitionEditor.addPanel.title,panel.label,
                             error.1.message,error.133.message "/>

  <@efForm id="addPanel" dashboard="true">
  <@efField field="panel" value="custom" width=20 required="true"/>
  </@efForm>

  ${params._variable}.postScript = 'ef.focus("panel")';

</script>
