<@efPreloadMessages codes="ok.label,cancel.label,
                           definitionEditor.addPanel.title,panel.label,error.1.message"/>

<@efForm id="changeWorkCenter" dashboard="true">
    <@efField field="WorkCenter.workCenter" id="wcdChangeWorkCenter" value="${params.workCenter!''}" required="true"/>
</@efForm>
${params._variable}.postScript = 'ef.focus("wcdChangeWorkCenter")';
