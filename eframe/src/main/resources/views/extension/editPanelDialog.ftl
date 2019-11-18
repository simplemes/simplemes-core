<script>

  <@efPreloadMessages codes="ok.label,cancel.label,
                             definitionEditor.editPanel.title,panel.label,
                             error.1.message,error.133.message"/>

  <@efForm id="editPanel" dashboard="true">
  <@efField field="panel" value="${params.panel!''}" width=20 required="true"/>
  </@efForm>

  ${params._variable}.postScript = 'tk.focus("panel");$$("editPanel").setValues({originalPanel: "${params.panel!''}"},true)';

</script>
