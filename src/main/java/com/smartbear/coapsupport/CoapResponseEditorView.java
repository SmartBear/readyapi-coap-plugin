package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.Response;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.plugins.auto.PluginResponseEditorView;
import com.eviware.soapui.support.editor.Editor;
import com.eviware.soapui.support.editor.views.AbstractXmlEditorView;
import com.eviware.soapui.support.editor.xml.XmlEditor;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.lang.management.MemoryManagerMXBean;

@PluginResponseEditorView(viewId = "CoAP Message", targetClass = CoapRequest.class)
public class CoapResponseEditorView extends AbstractXmlEditorView<CoapResponseDocument> {

    private JTextArea memo;
    private JScrollPane scrollPane;

    public CoapResponseEditorView(Editor<?> editor, CoapRequest request) {
        super("CoAP Message", (XmlEditor<CoapResponseDocument>)editor, "CoAP Message");
    }

    @Override
    public boolean saveDocument(boolean validate) {
        return false;
    }

    @Override
    public JComponent getComponent() {
        if(scrollPane == null) {
            memo = new JTextArea();
            memo.setEditable(false);
            memo.setText(formText());
            scrollPane = new JScrollPane(memo);
        }
        return scrollPane;
    }

    @Override
    public void setEditable(boolean enabled) {

    }

    @Override
    public void documentUpdated(){
       if(memo != null) memo.setText(formText());
    }

    private String formText(){
        if(getDocument() == null) return "";
        Response message = getDocument().getResponseMessage();
        if(message == null) return "";
        StringBuilder result = new StringBuilder();

        result.append("Type: ");
        result.append(message.getType().toString());
        result.append("\n");

        result.append("Code: ");
        result.append(message.getCode().toString());
        result.append("\n");

        result.append("Token: ");
        result.append(message.getTokenString());
        result.append("\n");

        result.append("Message ID: ");
        result.append(message.getMID());

        return result.toString();
    }
}
