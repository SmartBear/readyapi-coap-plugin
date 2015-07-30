package com.smartbear.coapsupport;

import com.eviware.soapui.impl.rest.actions.support.NewRestResourceActionBase;
import com.eviware.soapui.impl.rest.panels.resource.RestParamsTable;
import com.eviware.soapui.impl.rest.panels.resource.RestParamsTableModel;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.rest.support.handlers.JsonXmlSerializer;
import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequest;
import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
import com.eviware.soapui.model.iface.Request;
import com.eviware.soapui.plugins.auto.PluginRequestEditorView;
import com.eviware.soapui.plugins.auto.PluginResponseEditorView;
import com.eviware.soapui.support.DocumentListenerAdapter;
import com.eviware.soapui.support.editor.Editor;
import com.eviware.soapui.support.editor.views.AbstractXmlEditorView;
import com.eviware.soapui.support.editor.xml.XmlEditor;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.eviware.soapui.support.xml.XmlUtils;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.beans.PropertyConnector;
import net.sf.json.JSON;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

import static com.eviware.soapui.support.JsonUtil.seemsToBeJsonContentType;

@PluginRequestEditorView(viewId = CoapRequestEditorView.VIEW_ID, targetClass = CoapRequest.class)
public class CoapRequestEditorView extends AbstractXmlEditorView<AbstractHttpXmlRequestDesktopPanel.HttpRequestDocument>{
    private JComponent component = null;
    private CoapRequest request;
    private RSyntaxTextArea contentEditor;
    private RestParamsTable paramsTable;
    private boolean updatingRequest;
    private Expander bodyExpander;

    public final static String VIEW_ID = "CoAP Request";
    private KnownOptions.MediaTypeComboBox contentFormatCombo;
    private OptionsEditingPane optionsEditor;

    public CoapRequestEditorView(Editor<?> editor, CoapRequest request) {
        super("Request", (CoapRequestTestStepPanel.CoapRequestEditor) editor, VIEW_ID);
        this.request = request;
        this.request.addPropertyChangeListener(this);
    }

    @Override
    public boolean saveDocument(boolean validate) {
        return false;
    }

    @Override
    public JComponent getComponent() {
        if(component == null){

            JPanel mainPanel = new JPanel(new GridBagLayout());
            paramsTable = buildParamsTable();
            Expander paramsExpander = new Expander("Query Parameters", paramsTable, false, 200, 250);
            mainPanel.add(paramsExpander, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, getDefInsets(), 0, 0));

            optionsEditor = new OptionsEditingPane(request);
            optionsEditor.setData(request);
            optionsEditor.setEditable(true);
            final Expander optionsExpander = new Expander("Options", optionsEditor, true, 200, 250);
            mainPanel.add(optionsExpander, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, getDefInsets(), 0, 0));


            JPanel bodyPanel = buildBodyPanel();
            bodyExpander = new Expander("Payload", bodyPanel, true, 200, 300);
            mainPanel.add(bodyExpander, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, getDefInsets(), 0, 0));
            bodyExpander.setVisible(request.hasRequestBody());

            JPanel dummyPanel = new JPanel();
            dummyPanel.setPreferredSize(new Dimension(0, 0));
            mainPanel.add(dummyPanel, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

            component = new JScrollPane(mainPanel);

        }
        return component;
    }

    private Insets getDefInsets() {
        return new Insets(4, 4, 4, 4);
    }

    private JPanel buildBodyPanel() {
        JPanel bodyPanel = new JPanel(new GridBagLayout());

        JLabel label = new JLabel("Content format:");
        contentFormatCombo = new KnownOptions.MediaTypeComboBox();
        label.setLabelFor(contentFormatCombo);
        //Bindings.bind(contentFormatCombo, KnownOptions.MediaTypeComboBox.VALUE_BEAN_PROP, new PropertyAdapter<CoapRequest>(request, CoapRequest.CONTENT_FORMAT_OPTION_BEAN_PROP));
        PropertyConnector connector = PropertyConnector.connect(request, CoapRequest.CONTENT_FORMAT_OPTION_BEAN_PROP, contentFormatCombo, KnownOptions.MediaTypeComboBox.VALUE_BEAN_PROP);
        connector.updateProperty2();
        bodyPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, getDefInsets(), 0, 0));
        bodyPanel.add(contentFormatCombo, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, getDefInsets(), 0, 0));

        contentEditor = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        SyntaxEditorUtil.setMediaType(contentEditor, request.getMediaType());
        contentEditor.setText(request.getRequestContent());

        contentEditor.getDocument().addDocumentListener(new DocumentListenerAdapter() {

            @Override
            public void update(Document document) {
                if (!updatingRequest) {
                    updatingRequest = true;
                    request.setRequestContent(getText(document));
                    updatingRequest = false;
                }
            }
        });

        PropertyExpansionPopupListener.enable(contentEditor, request);

        bodyPanel.add(new JScrollPane(contentEditor), new GridBagConstraints(0, 1, 3, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, getDefInsets(), 0, 0));

        return bodyPanel;
    }

    @Override
    public void setEditable(boolean enabled) {
        contentEditor.setEditable(enabled);
        contentFormatCombo.setEnabled(enabled);
        optionsEditor.setEditable(enabled);
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

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(AbstractHttpRequest.REQUEST_PROPERTY) && !updatingRequest) {
            updatingRequest = true;
            String requestBodyAsXml = (String) evt.getNewValue();
            String mediaType = request.getMediaType();
            if (XmlUtils.seemsToBeXml(requestBodyAsXml) &&
                    seemsToBeJsonContentType(mediaType)) {
                JSON jsonObject = new JsonXmlSerializer().read(requestBodyAsXml);
                contentEditor.setText(jsonObject.toString(3, 0));
            } else {
                contentEditor.setText(requestBodyAsXml);
            }
            updatingRequest = false;
        }
        else if (evt.getPropertyName().equals("method")) {
            bodyExpander.setVisible(request.hasRequestBody());
        }
        else if (evt.getPropertyName().equals(HttpRequest.MEDIA_TYPE)) {
            SyntaxEditorUtil.setMediaType(contentEditor, request.getMediaType());
        }
        super.propertyChange(evt);
        if (paramsTable != null) {
            paramsTable.refresh();
        }
    }

    @Override
    public void release() {
        super.release();
        request.removePropertyChangeListener(this);
        paramsTable.release();
    }

}
