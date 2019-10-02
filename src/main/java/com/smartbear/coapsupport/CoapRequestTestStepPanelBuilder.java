package com.smartbear.coapsupport;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.support.components.JPropertiesTable;
import com.eviware.soapui.ui.desktop.DesktopPanel;

import java.awt.Component;

@PluginPanelBuilder(targetModelItem = CoapRequestTestStep.class)
public class CoapRequestTestStepPanelBuilder extends EmptyPanelBuilder<CoapRequestTestStep> {
    @Override
    public DesktopPanel buildDesktopPanel(CoapRequestTestStep testStep) {
        return new CoapRequestTestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }

    @Override
    public boolean hasOverviewPanel() {
        return true;
    }

    @Override
    public Component buildOverviewPanel(CoapRequestTestStep modelItem) {
        CoapRequest request = modelItem.getRequest();
        JPropertiesTable<CoapRequest> table = new JPropertiesTable<CoapRequest>("CoAP Request Properties");

        // basic properties
        table.addProperty("Name", "name", true);
        table.addProperty("Description", "description", true);
        // specific properties
        table.addProperty("Auto Size1 Option", CoapRequest.AUTO_SIZE1_BEAN_PROP, JPropertiesTable.BOOLEAN_OPTIONS);
        table.addProperty("Multi-Value Delimiter", "multiValueDelimiter", true);
        table.addProperty("Send Empty Parameters", "sendEmptyParameters", JPropertiesTable.BOOLEAN_OPTIONS);
        table.addProperty("Timeout", "timeout", true);
        table.setPropertyObject(request);

        return table;
    }
}
