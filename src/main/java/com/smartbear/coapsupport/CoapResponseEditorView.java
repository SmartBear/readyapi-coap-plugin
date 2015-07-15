package com.smartbear.coapsupport;

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
            scrollPane = new JScrollPane(memo);
        }
        return scrollPane;
    }

    @Override
    public void setEditable(boolean enabled) {

    }

    @Override
    public void documentUpdated(){
       // memo.setText(formText());
    }

    private String formText(){
        return "";
    }
}
