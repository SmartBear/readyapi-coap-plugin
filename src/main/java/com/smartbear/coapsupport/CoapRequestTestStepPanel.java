package com.smartbear.coapsupport;

import com.eviware.soapui.impl.support.components.ModelItemXmlEditor;
import com.eviware.soapui.impl.support.http.HttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequestContentView;
import com.eviware.soapui.impl.support.http.HttpRequestInterface;
import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
import com.eviware.soapui.impl.wsdl.panels.teststeps.HttpTestRequestDesktopPanel;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStepInterface;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Request;
import com.eviware.soapui.model.iface.Submit;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.MessageSupport;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.editor.EditorView;
import com.eviware.soapui.support.editor.views.xml.outline.XmlOutlineEditorView;
import com.eviware.soapui.support.editor.views.xml.outline.support.XmlObjectTree;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.list.SelectionInList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

public class CoapRequestTestStepPanel extends HttpTestRequestDesktopPanel {
    private CoapRequestTestStep testStep;

    public CoapRequestTestStepPanel(CoapRequestTestStep modelItem) {
        super(modelItem);
    }

//    @Override
//    protected ModelItemXmlEditor<?, ?> buildRequestEditor() {
//        return new CoapRequestEditor(getRequest());
//    }
//
//    @Override
//    protected ModelItemXmlEditor<?, ?> buildResponseEditor() {        return new CoapResponseEditor(getRequest());
//    }

    private CoapRequestTestStep getTestStep(){return (CoapRequestTestStep) getModelItem();}

    @Override
    protected String getHelpUrl() {
        return "";
    }

    @Override
    protected void addToolbarComponents(JXToolBar toolbar) {
        JCheckBox conf = new JCheckBox("Confirmable Request");
        Bindings.bind(conf, new PropertyAdapter<CoapRequest>(getTestStep().getRequest(), "confirmable"));
        toolbar.add(conf);
    }

    @Override
    protected Submit doSubmit() throws Request.SubmitException {
        return null;
    }

    class CoapRequestEditor extends HttpRequestMessageEditor{

        public CoapRequestEditor(CoapRequest request) {
            super(request);
        }

        @Override
        public void addEditorView(EditorView editorView) {
//            if(editorView instanceof HttpRequestContentView) return;
//            if(editorView instanceof XmlObjectTree) return;
//            if(editorView instanceof XmlOutlineEditorView) return;
            super.addEditorView(editorView);
        }
    }

    class CoapResponseEditor extends HttpResponseMessageEditor{
        public CoapResponseEditor(CoapRequest request) {
            super(request);
        }

        @Override
        public void addEditorView(EditorView editorView) {
//            if(editorView instanceof HttpRequestContentView) return;
//            if(editorView instanceof XmlObjectTree) return;
//            if(editorView instanceof XmlOutlineEditorView) return;
            super.addEditorView(editorView);
        }
    }
}
