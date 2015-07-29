package com.smartbear.coapsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.support.components.ModelItemXmlEditor;
import com.eviware.soapui.impl.support.http.HttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequestContentView;
import com.eviware.soapui.impl.support.http.HttpRequestContentViewFactory;
import com.eviware.soapui.impl.support.http.HttpRequestInterface;
import com.eviware.soapui.impl.support.panels.AbstractHttpRequestDesktopPanel;
import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
import com.eviware.soapui.impl.wsdl.panels.teststeps.AssertionsPanel;
import com.eviware.soapui.impl.wsdl.panels.teststeps.HttpTestRequestDesktopPanel;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestRunContext;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStepInterface;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestInterface;
import com.eviware.soapui.impl.wsdl.teststeps.actions.AddAssertionAction;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Request;
import com.eviware.soapui.model.iface.Submit;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.*;
import com.eviware.soapui.monitor.support.TestMonitorListenerAdapter;
import com.eviware.soapui.security.SecurityTestRunner;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.MessageSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.editor.EditorView;
import com.eviware.soapui.support.editor.views.xml.outline.XmlOutlineEditorView;
import com.eviware.soapui.support.editor.views.xml.outline.support.XmlObjectTree;
import com.eviware.soapui.support.editor.views.xml.raw.RawXmlEditorFactory;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.ui.desktop.DesktopPanel;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.list.SelectionInList;


import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

public class CoapRequestTestStepPanel extends AbstractHttpXmlRequestDesktopPanel<CoapRequestTestStep, CoapRequest> {
    private JLogList logArea;
    private InternalTestMonitorListener testMonitorListener = new InternalTestMonitorListener();
    private JInspectorPanel inspectorPanel;
    private AssertionsPanel assertionsPanel;
    private JComponentInspector<JComponent> assertionInspector;
    private JComponentInspector<JComponent> logInspector;
    private ImageIcon failedIcon;
    private ImageIcon unknownStatusIcon;
    private ImageIcon successIcon;
    private InternalAssertionsListener assertionsListener = new InternalAssertionsListener();
    private long startTime;
    private JButton addAssertionButton;

    public CoapRequestTestStepPanel(CoapRequestTestStep modelItem) {
        super(modelItem, modelItem.getRequest());
        SoapUI.getTestMonitor().addTestMonitorListener(testMonitorListener);
        setEnabled(!SoapUI.getTestMonitor().hasRunningTest(getTestStep().getTestCase()));
        getTestStep().getTestRequest().addAssertionsListener(assertionsListener);
        getSubmitButton().setEnabled(getSubmit() == null && StringUtils.hasContent(getRequest().getEndpoint()));

    }


    private CoapRequestTestStep getTestStep(){return (CoapRequestTestStep) getModelItem();}
    protected JComponent buildLogPanel() {
        logArea = new JLogList("Request Log");

        logArea.getLogList().getModel().addListDataListener(new ListDataChangeListener() {
            @Override
            public void dataChanged(ListModel model) {
                logInspector.setTitle("Request Log (" + model.getSize() + ")");
            }
        });

        return logArea;
    }

    protected AssertionsPanel buildAssertionsPanel() {
        failedIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/failed_assertion.png");
        unknownStatusIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/unknown_assertion.png");
        successIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/valid_assertion.png");
        return new AssertionsPanel(getRequest()) {
            @Override
            protected void selectError(com.eviware.soapui.model.testsuite.AssertionError error) {
                ModelItemXmlEditor<?, ?> editor = getResponseEditor();
                editor.requestFocus();
            }
        };
    }

    @Override
    public void setContent(JComponent content) {
        inspectorPanel.setContentComponent(content);
    }

    @Override
    public void removeContent(JComponent content) {
        inspectorPanel.setContentComponent(null);
    }

    @Override
    protected String getHelpUrl() {
        return "";
    }

    @Override
    protected JComponent buildContent() {
        JComponent component = super.buildContent();

        inspectorPanel = JInspectorPanelFactory.build(component);
        assertionsPanel = buildAssertionsPanel();

        assertionInspector = new JComponentInspector<JComponent>(assertionsPanel, "Assertions ("
                + getModelItem().getAssertionCount() + ")", "Assertions for this Request", true);

        inspectorPanel.addInspector(assertionInspector);

        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), "Request Log (0)", "Log of requests", true);
        inspectorPanel.addInspector(logInspector);
        inspectorPanel.setDefaultDividerLocation(0.6F);
        inspectorPanel.setCurrentInspector("Assertions");

        updateStatusIcon();

        getSubmitButton().setEnabled(getSubmit() == null && StringUtils.hasContent(getRequest().getEndpoint()));

        return inspectorPanel.getComponent();
    }

    @Override
    protected JComponent buildEndpointComponent() {
        return null;
    }

    protected void updateStatusIcon() {
        Assertable.AssertionStatus status = getModelItem().getTestRequest().getAssertionStatus();
        switch (status) {
            case FAILED: {
                assertionInspector.setIcon(failedIcon);
                inspectorPanel.activate(assertionInspector);
                break;
            }
            case UNKNOWN: {
                assertionInspector.setIcon(unknownStatusIcon);
                break;
            }
            case VALID: {
                assertionInspector.setIcon(successIcon);
                inspectorPanel.deactivate();
                break;
            }
        }
    }

    @Override
    protected void insertButtons(JXToolBar toolbar) {
        addAssertionButton = createActionButton(new AddAssertionAction(getRequest()), true);
        toolbar.add(addAssertionButton);
    }

    @Override
    protected JComponent buildToolbar() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(super.buildToolbar(), BorderLayout.NORTH);

        JXToolBar toolbar = UISupport.createToolbar();
        addToolbarComponents(toolbar);

        panel.add(toolbar, BorderLayout.SOUTH);
        return panel;

    }

    protected void addToolbarComponents(JXToolBar toolbar) {
        JComboBox<RestRequestInterface.HttpMethod> methodCombo = new JComboBox<>();
        Bindings.bind(methodCombo, new SelectionInList<RestRequestInterface.HttpMethod>(new RestRequestInterface.HttpMethod[]{RestRequestInterface.HttpMethod.GET, RestRequestInterface.HttpMethod.POST, RestRequestInterface.HttpMethod.PUT, RestRequestInterface.HttpMethod.DELETE}, new PropertyAdapter<CoapRequest>(getTestStep().getRequest(), "method")));
        toolbar.addLabeledFixed("Method:", methodCombo);

        JUndoableTextField endpointEdit = new JUndoableTextField(50);
        Bindings.bind(endpointEdit, new PropertyAdapter<CoapRequest>(getTestStep().getRequest(), "endpoint"));
        toolbar.addLabeledFixed("Request Endpoint:", endpointEdit);

        JCheckBox conf = new JCheckBox("Confirmable Request");
        Bindings.bind(conf, new PropertyAdapter<CoapRequest>(getTestStep().getRequest(), "confirmable"));
        toolbar.add(conf);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            enabled = !SoapUI.getTestMonitor().hasRunningLoadTest(getModelItem().getTestCase())
                    && !SoapUI.getTestMonitor().hasRunningSecurityTest(getModelItem().getTestCase());
        }

        super.setEnabled(enabled);
        addAssertionButton.setEnabled(enabled);
        assertionsPanel.setEnabled(enabled);

        if (SoapUI.getTestMonitor().hasRunningLoadTest(getRequest().getTestCase())
                || SoapUI.getTestMonitor().hasRunningSecurityTest(getModelItem().getTestCase())) {
            getRequest().removeSubmitListener(this);
        } else {
            getRequest().addSubmitListener(this);
        }
    }

    @Override
    protected Submit doSubmit() throws Request.SubmitException {
        return getRequest().submit(new WsdlTestRunContext(getModelItem()), true);
    }

    private final class InternalAssertionsListener implements AssertionsListener {
        public void assertionAdded(TestAssertion assertion) {
            assertionInspector.setTitle("Assertions (" + getModelItem().getAssertionCount() + ")");
        }

        public void assertionRemoved(TestAssertion assertion) {
            assertionInspector.setTitle("Assertions (" + getModelItem().getAssertionCount() + ")");
        }

        public void assertionMoved(TestAssertion assertion, int ix, int offset) {
            assertionInspector.setTitle("Assertions (" + getModelItem().getAssertionCount() + ")");
        }
    }

    @Override
    public boolean beforeSubmit(Submit submit, SubmitContext context) {
        boolean result = super.beforeSubmit(submit, context);
        startTime = System.currentTimeMillis();
        return result;
    }

    private boolean disableMessageLogging = false;

    @Override
    protected void logMessages(String message, String infoMessage) {
        if(disableMessageLogging) return;
        super.logMessages(message, infoMessage);
        logArea.addLine(DateUtil.formatFull(new Date(startTime)) + " - " + message);
    }

    @Override
    public void afterSubmit(Submit submit, SubmitContext context) {
        if (submit.getRequest() != getRequest()) {
            return;
        }
        if(submit.getStatus() == Submit.Status.ERROR && submit.getError() instanceof InvocationTargetException){
            disableMessageLogging = true;
            try {
                super.afterSubmit(submit, context);
            }
            finally {
                disableMessageLogging = false;
            }
            String actualError = ((InvocationTargetException) submit.getError()).getTargetException().getMessage();
            String msg = String.format("Error getting response: %s", actualError);
            String infoMsg = "Error getting response for [" + submit.getRequest().getName() + "]; " + actualError;
            logMessages(msg, infoMsg);
        }
        else{
            super.afterSubmit(submit, context);
        }
        if (!isHasClosed()) {
            updateStatusIcon();
        }
    }

    @Override
    public boolean onClose(boolean canCancel) {
        if (super.onClose(canCancel)) {
            assertionsPanel.release();
            inspectorPanel.release();
            SoapUI.getTestMonitor().removeTestMonitorListener(testMonitorListener);
            getModelItem().getTestRequest().removeAssertionsListener(assertionsListener);
            return true;
        }

        return false;
    }

    @Override
    public boolean dependsOn(ModelItem modelItem) {
        if (getRequest().getOperation() == null) {
            return modelItem == getRequest() || modelItem == getModelItem()
                    || ModelSupport.getModelItemProject(getRequest()) == modelItem
                    || modelItem == getModelItem().getTestCase() || modelItem == getModelItem().getTestCase().getTestSuite();
        } else {
            return modelItem == getRequest() || modelItem == getModelItem() || modelItem == getRequest().getOperation()
                    || modelItem == getRequest().getOperation().getInterface()
                    || modelItem == getRequest().getOperation().getInterface().getProject()
                    || modelItem == getModelItem().getTestCase() || modelItem == getModelItem().getTestCase().getTestSuite();
        }
    }


    private class InternalTestMonitorListener extends TestMonitorListenerAdapter {
        @Override
        public void loadTestFinished(LoadTestRunner runner) {
            setEnabled(!SoapUI.getTestMonitor().hasRunningTest(getModelItem().getTestCase()));
        }

        @Override
        public void loadTestStarted(LoadTestRunner runner) {
            if (runner.getLoadTest().getTestCase() == getModelItem().getTestCase()) {
                setEnabled(false);
            }
        }

        public void securityTestFinished(SecurityTestRunner runner) {
            setEnabled(!SoapUI.getTestMonitor().hasRunningTest(getModelItem().getTestCase()));
        }

        public void securityTestStarted(SecurityTestRunner runner) {
            if (runner.getSecurityTest().getTestCase() == getModelItem().getTestCase()) {
                setEnabled(false);
            }
        }

        @Override
        public void testCaseFinished(TestCaseRunner runner) {
            setEnabled(!SoapUI.getTestMonitor().hasRunningTest(getModelItem().getTestCase()));
        }

        @Override
        public void testCaseStarted(TestCaseRunner runner) {
            if (runner.getTestCase() == getModelItem().getTestCase()) {
                setEnabled(false);
            }
        }
    }


    @Override
    protected ModelItemXmlEditor<?, ?> buildRequestEditor() {
        return new CoapRequestEditor(getRequest());
    }

    @Override
    protected ModelItemXmlEditor<?, ?> buildResponseEditor() {
        return new CoapResponseEditor(getRequest());
    }


    class CoapRequestEditor extends HttpRequestMessageEditor{

        public CoapRequestEditor(CoapRequest request) {
            super(request);
        }

        @Override
        public void addEditorView(EditorView editorView) {
            if(getView(editorView.getViewId()) != null) return;
//            if(HttpRequestContentViewFactory.VIEW_ID.equals(editorView.getViewId())) return;
//            if(RawXmlEditorFactory.VIEW_ID.equals(editorView.getViewId())) return;
            super.addEditorView(editorView);
        }

        @Override
        protected void createMissingInspectors() {

        }

    }

    class CoapResponseEditor extends AbstractHttpRequestDesktopPanel.AbstractHttpResponseMessageEditor{
        public CoapResponseEditor(CoapRequest request) {
            super(new CoapResponseDocument(request));
        }

        @Override
        public void addEditorView(EditorView editorView) {
            if(getView(editorView.getViewId()) != null) return;
            super.addEditorView(editorView);
        }

        @Override
        protected void createMissingInspectors() {

        }
    }

}
