package com.smartbear.coapsupport;

import com.eviware.soapui.impl.rest.actions.support.NewRestResourceActionBase;
import com.eviware.soapui.impl.rest.panels.resource.RestParamsTable;
import com.eviware.soapui.impl.rest.panels.resource.RestParamsTableModel;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
import com.eviware.soapui.plugins.auto.PluginRequestEditorView;
import com.eviware.soapui.plugins.auto.PluginResponseEditorView;
import com.eviware.soapui.support.editor.Editor;
import com.eviware.soapui.support.editor.views.AbstractXmlEditorView;
import com.eviware.soapui.support.editor.xml.XmlEditor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@PluginRequestEditorView(viewId = "CoAP Request", targetClass = CoapRequest.class)
public class CoapRequestEditorView extends AbstractXmlEditorView<AbstractHttpXmlRequestDesktopPanel.HttpRequestDocument>{
    private JComponent component = null;
    private CoapRequest request;

    public CoapRequestEditorView(Editor<?> editor, CoapRequest request) {
        super("CoAP Request", (CoapRequestTestStepPanel.CoapRequestEditor) editor, "CoAP Request");
        this.request = request;
    }

    @Override
    public boolean saveDocument(boolean validate) {
        return false;
    }

    @Override
    public JComponent getComponent() {
        if(component == null){

            JPanel mainPanel = new JPanel(new GridBagLayout());
            RestParamsTable paramsTable = buildParamsTable();
//            paramsTable.setPreferredSize(new Dimension(500, 400));
//            paramsTable.setMinimumSize(new Dimension(500, 400));
//            paramsTable.setPreferredSize(new Dimension(Integer.MAX_VALUE, 400));
            Expander paramsExpander = new Expander("Query Parameters", paramsTable, false, 200, 400);
            //paramsExpander.setFont(paramsExpander.getFont().deriveFont(Font.BOLD));
            mainPanel.add(paramsExpander, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

            OptionsEditingPane optionsEditor = new OptionsEditingPane();
            optionsEditor.setData(request, true);
            final Expander optionsExpander = new Expander("Options", optionsEditor, true, 200, 400);
          //  optionsExpander.setFont(paramsExpander.getFont().deriveFont(Font.BOLD));
            mainPanel.add(optionsExpander, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));


            JPanel dummyPanel = new JPanel();
            dummyPanel.setPreferredSize(new Dimension(0, 0));
            mainPanel.add(dummyPanel, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            component = new JScrollPane(mainPanel);

        }
        return component;
    }

    @Override
    public void setEditable(boolean enabled) {

    }

    protected RestParamsTable buildParamsTable() {
        RestParamsTableModel restParamsTableModel = new RestParamsTableModel(request.getParams()) {
            @Override
            public String getColumnName(int column) {
                return column == 0 ? "Name" : "Value";
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                RestParamProperty prop = params.getPropertyAt(rowIndex);
                return columnIndex == 0 ? prop.getName() : prop.getValue();
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                RestParamProperty prop = params.getPropertyAt(rowIndex);
                if (columnIndex == 0) {
                    prop.setName(value.toString());
                } else {
                    prop.setValue(value.toString());
                }
            }
        };
        return new RestParamsTable(request.getParams(), false, restParamsTableModel, NewRestResourceActionBase.ParamLocation.RESOURCE, true, false);
    }

}
