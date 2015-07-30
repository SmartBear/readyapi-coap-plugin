package com.smartbear.coapsupport;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.XFormFieldValidator;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class OptionsPane extends JPanel {
    private ModelItem owningModelItem;
    private OptionsTable grid;
    private final ImageIcon moveDownIcon;
    private final ImageIcon moveUpIcon;
    private ImageIcon addIcon;
    private ImageIcon deleteIcon;
    private DeleteOptionAction deleteOptionAction;
    private AddOptionAction addOptionAction;
    private MoveOptionUpAction moveOptionUpAction;
    private MoveOptionDownAction moveOptionDownAction;



    public OptionsPane(ModelItem owningModelItem){
        this.owningModelItem = owningModelItem;
        addIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/add.png");
        deleteIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/delete.png");
        moveDownIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/down_arrow.gif");
        moveUpIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/up_arrow.gif");
        buildUI();

    }

    private JComponent buildToolbar(){
        JXToolBar toolBar = UISupport.createToolbar();
        addOptionAction = new AddOptionAction();
        toolBar.add(UISupport.createToolbarButton(addOptionAction, addOptionAction.isEnabled()));
        deleteOptionAction = new DeleteOptionAction();
        toolBar.add(UISupport.createToolbarButton(deleteOptionAction, deleteOptionAction.isEnabled()));
        moveOptionDownAction = new MoveOptionDownAction();
        toolBar.add(UISupport.createToolbarButton(moveOptionDownAction, moveOptionDownAction.isEnabled()));
        moveOptionUpAction = new MoveOptionUpAction();
        toolBar.add(UISupport.createToolbarButton(moveOptionUpAction, moveOptionUpAction.isEnabled()));
        return toolBar;
    }

    private JComponent buildGrid(){
        grid = new OptionsTable(owningModelItem);
        return new JScrollPane(grid);
    }


    private void buildUI(){
        setLayout(new BorderLayout(0, 0));
        add(buildGrid(), BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.NORTH);
        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (deleteOptionAction == null) return;
                deleteOptionAction.updateState();
                moveOptionDownAction.updateState();
                moveOptionUpAction.updateState();
            }
        });

    }

    private boolean isActionOnSelectedOptionAllowed(){
        return grid.isEditable() && getData() != null && grid.getSelectedRows() != null && grid.getSelectedRows().length != 0;
    }

    private void updateActions(){
        addOptionAction.updateState();
        deleteOptionAction.updateState();
        moveOptionDownAction.updateState();
        moveOptionUpAction.updateState();
    }

    public CoapOptionsDataSource getData(){return grid.getData();}

    public void setData(CoapOptionsDataSource data){
        grid.setData(data);
        updateActions();
    }

    public boolean isEditable(){return grid.isEditable();}
    public void setEditable(boolean newValue){
        grid.setEditable(newValue);
        updateActions();
    }

    private boolean hasOption(int optionNumber) {
        CoapOptionsDataSource dataSource = getData();
        if(dataSource == null) return false;
        int rowCount = dataSource.getOptionCount();
        for(int i = 0; i < rowCount; i++){
            if(dataSource.getOptionNumber(i) == optionNumber) return true;
        }
        return false;
    }


    private class AddOptionAction extends AbstractAction implements Action {
        private Component valueEditingComponent;
        private TableCellEditor editor;

        public AddOptionAction(){
            super();
            putValue(Action.SHORT_DESCRIPTION, "Add Option");
            putValue(Action.SMALL_ICON, addIcon);
            updateState();
        }

        private int getOptionNumber(XFormOptionsField combo){
            if(combo.getSelectedIndexes().length == 0){
                String s = combo.getValue();
                if(StringUtils.isNullOrEmpty(s)) return -1;
                s = s.trim();
                int radix = 10;
                if(s.startsWith("0x") || s.startsWith("0X")) {
                    radix = 16;
                    s = s.substring(2);
                }
                try {
                    return Integer.parseInt(s, radix);
                } catch (NumberFormatException ignored ) {
                    return -1;
                }
            }
            else{
                return KnownOptions.editableRequestOptions[combo.getSelectedIndexes()[0]];
            }

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            valueEditingComponent = null;
            XFormDialog dialog = ADialogBuilder.buildDialog(ChooseOptionNumberForm.class);
            try{
                final XFormField valueField = dialog.getFormField(ChooseOptionNumberForm.VALUE);
                final JTextField dummyValueComponent = new JTextField(20);
                valueField.setProperty("component", dummyValueComponent);
                dummyValueComponent.setEnabled(false);
                valueField.setProperty("preferredSize", dummyValueComponent.getPreferredSize());

                final XFormOptionsField combo = (XFormOptionsField) dialog.getFormField(ChooseOptionNumberForm.OPTION);
                final String[] optionNames = KnownOptions.getEditableRequestOptionsNames();
                String[] comboItems = new String[KnownOptions.editableRequestOptions.length + 1];
                System.arraycopy(optionNames, 0, comboItems, 1, optionNames.length);
                combo.setOptions(comboItems);
                combo.addFormFieldValidator(new XFormFieldValidator() {
                    @Override
                    public ValidationMessage[] validateField(XFormField formField) {
                        int optionNumber = getOptionNumber(combo);
                        if(optionNumber <= 0) return new ValidationMessage[]{new ValidationMessage("Please type a valid positive decimal or hexadecimal number (starting from 0x) or choose an option from the list.", combo)};
                        if(OptionNumberRegistry.isSingleValue(optionNumber) && hasOption(optionNumber)) return new ValidationMessage[]{new ValidationMessage("Unable to add this option because it is already specified and it does not allow multiple values", combo)};
                        if(editor != null){
                            String value = (String)editor.getCellEditorValue();
                            if(value == null) return new ValidationMessage[]{new ValidationMessage("Please input a valid option value", valueField)};
                        }
                        return new ValidationMessage[0];
                    }
                });
                combo.addFormFieldListener(new XFormFieldListener() {
                    @Override
                    public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                        int optionNumber = getOptionNumber(combo);
                        if(optionNumber <= 0){
                            dummyValueComponent.setEnabled(false);
                            dummyValueComponent.setText("");
                            valueEditingComponent = dummyValueComponent;
                            editor = null;
                        }
                        else{
                            Class<? extends TableCellEditor> clazz = KnownOptions.getOptionEditor(optionNumber);
                            if(clazz == null){
                                dummyValueComponent.setEnabled(true);
                                dummyValueComponent.setText("");
                                valueEditingComponent = dummyValueComponent;
                                editor = null;
                            }
                            else {
                                try {
                                    editor = clazz.getConstructor(ModelItem.class).newInstance(owningModelItem);
                                }
                                catch(Throwable e){
                                    return;
                                }
                                valueEditingComponent = editor.getTableCellEditorComponent(null, null, true, -1, -1);
                            }
                        }
                        valueField.setProperty("component", valueEditingComponent);
                        valueField.setProperty("preferredSize", valueEditingComponent.getPreferredSize());
                    }
                });
                if(dialog.show()){
                    int optionNumber = getOptionNumber(combo);
                    String value;
                    if(editor != null) {
                        value = (String) editor.getCellEditorValue();
                    }
                    else{
                        value = dummyValueComponent.getText();
                    }
                    int index = getData().addOption(optionNumber, value);
                    grid.getSelectionModel().clearSelection();
                    grid.getSelectionModel().addSelectionInterval(index, index);

                }
            }
            finally {
                dialog.release();
            }
        }

        public void updateState() {
            setEnabled(isEditable() && getData() != null);
        }
    }

    private class DeleteOptionAction extends AbstractAction{
        public DeleteOptionAction(){
            putValue(Action.SHORT_DESCRIPTION, "Remove Option");
            putValue(Action.SMALL_ICON, deleteIcon);
            updateState();
        }

        private void updateState() {
            setEnabled(isActionOnSelectedOptionAllowed());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = grid.getSelectedRows();
            Arrays.sort(rows);
            for(int i = rows.length - 1; i >= 0; --i){
                getData().removeOption(rows[i]);
            }
            if(rows.length == 1){
                int newOptionCount = getData().getOptionCount();
                grid.getSelectionModel().clearSelection();
                if(rows[0] < newOptionCount) {
//                    grid.getSelectionModel().setLeadSelectionIndex(rows[0]);
                    grid.getSelectionModel().addSelectionInterval(rows[0], rows[0]);

                }
                else if(newOptionCount != 0){
//                    grid.getSelectionModel().setLeadSelectionIndex(newOptionCount - 1);
                    grid.getSelectionModel().addSelectionInterval(newOptionCount - 1, newOptionCount - 1);
                }
            }
        }
    }

    private class MoveOptionUpAction extends AbstractAction{
        public MoveOptionUpAction(){
            putValue(Action.SHORT_DESCRIPTION, "Move Option Up");
            putValue(Action.SMALL_ICON, moveUpIcon);
            updateState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int optionNo = grid.getSelectionModel().getLeadSelectionIndex();
            getData().moveOption(optionNo, -1);
//            grid.getSelectionModel().setLeadSelectionIndex(optionNo - 1);
            grid.getSelectionModel().clearSelection();
            grid.getSelectionModel().addSelectionInterval(optionNo - 1, optionNo - 1);
        }

        public void updateState() {
            setEnabled(isActionOnSelectedOptionAllowed() && grid.getSelectionModel().getLeadSelectionIndex() > 0);
        }
    }

    private class MoveOptionDownAction extends AbstractAction{
        public MoveOptionDownAction(){
            putValue(Action.SHORT_DESCRIPTION, "Move Option Down");
            putValue(Action.SMALL_ICON, moveDownIcon);
            updateState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int optionNo = grid.getSelectionModel().getLeadSelectionIndex();
            getData().moveOption(optionNo, +1);
            //grid.getSelectionModel().setLeadSelectionIndex(optionNo + 1);
            grid.getSelectionModel().clearSelection();
            grid.getSelectionModel().addSelectionInterval(optionNo + 1, optionNo + 1);
        }

        public void updateState() {
            setEnabled(isActionOnSelectedOptionAllowed() && grid.getSelectionModel().getLeadSelectionIndex() >= 0 && grid.getSelectionModel().getLeadSelectionIndex() != grid.getRowCount() - 1);
        }
    }

}
