package com.smartbear.coapsupport;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;

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

}
