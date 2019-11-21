<#--@formatter:off-->
<script>

  <@efPreloadMessages codes="cancel.label,
                             definitionEditor.addField.title,definitionEditor.editField.title,panel.label,
                             fieldName.label,fieldLabel.label,fieldFormat.label,maxLength.label,valueClassName.label,
                             definitionEditor.saveField.label,definitionEditor.saveField.tooltip
                             error.1.message,error.133.message"/>

  <@efForm id="editField" dashboard="true">
    <@efField field="AbstractField.fieldName" required='true'/>
    <@efField field="AbstractField.fieldLabel" />
    <@efField field="AbstractField.fieldFormat" />
    <@efField field="AbstractField.maxLength" />
    <@efField field="AbstractField.valueClassName" />
    <@efButtonGroup>
      <@efButton id='saveField' label="definitionEditor.saveField.label" click="efd._editorFieldSave()" />
      <@efButton id='cancelField' label="cancel.label" click="efd._editorFieldCancel()"/>
    </@efButtonGroup>
  </@efForm>

  // use id:
  ${params._variable}.postScript = 'tk.focus("fieldName");$$("editField").setValues({id: "${params.id!''}"},true)';

</script>