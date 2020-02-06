<#--@formatter:off-->
<script>

  <@efPreloadMessages codes="cancel.label,
                             definitionEditor.addField.title,definitionEditor.editField.title,panel.label,
                             fieldName.label,fieldLabel.label,fieldFormat.label,maxLength.label,valueClassName.label,
                             definitionEditor.saveField.label,definitionEditor.saveField.tooltip
                             error.1.message,error.133.message"/>

  <@efForm id="editField" dashboard="true">
    <@efField field="FlexField.fieldName" required='true'/>
    <@efField field="FlexField.fieldLabel" />
    <@efField field="FlexField.fieldFormat" />
    <@efField field="FlexField.maxLength" />
    <@efField field="FlexField.valueClassName" />
    <@efButtonGroup>
      <@efButton id='saveField' label="definitionEditor.saveField.label" click="efd._editorFieldSave()" />
      <@efButton id='cancelField' label="cancel.label" click="efd._editorFieldCancel()"/>
    </@efButtonGroup>
  </@efForm>

  // use id:
  ${params._variable}.postScript = 'ef.focus("fieldName");$$("editField").setValues({id: "${params.id!''}"},true)';

</script>
