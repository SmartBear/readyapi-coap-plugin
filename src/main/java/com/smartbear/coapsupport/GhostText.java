package com.smartbear.coapsupport;


import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class GhostText<TextComponent extends JTextComponent> implements FocusListener, DocumentListener, PropertyChangeListener, PopupMenuListener
{
    protected final TextComponent textComp;
    protected boolean isEmpty;
    private Color ghostColor;
    private Color foregroundColor;
    protected final String ghostText;
    private boolean isPopupOpen = false;

    public GhostText(final TextComponent textComp, String ghostText)
    {
        super();
        this.textComp = textComp;
        this.ghostText = ghostText;
        this.ghostColor = Color.LIGHT_GRAY;
        textComp.addFocusListener(this);
        textComp.addPropertyChangeListener("componentPopupMenu", this);
        JPopupMenu popupMenu = textComp.getComponentPopupMenu();
        if(popupMenu != null) popupMenu.addPopupMenuListener(this);
        registerListeners();
        updateState(true);
    }

    public void delete()
    {
        unregisterListeners();
        textComp.removePropertyChangeListener("componentPopupMenu", this);
        JPopupMenu popupMenu = textComp.getComponentPopupMenu();
        if(popupMenu != null) popupMenu.removePopupMenuListener(this);
        textComp.removeFocusListener(this);
    }

    public TextComponent getComponent(){return textComp;}
    public String getText(){return isEmpty ? "" : textComp.getText();}

    private void registerListeners()
    {
        textComp.getDocument().addDocumentListener(this);
        textComp.addPropertyChangeListener("foreground", this);
    }

    private void unregisterListeners()
    {
        textComp.getDocument().removeDocumentListener(this);
        textComp.removePropertyChangeListener("foreground", this);
    }

    public Color getGhostColor()
    {
        return ghostColor;
    }

    public void setGhostColor(Color ghostColor)
    {
        this.ghostColor = ghostColor;
    }

    private void updateState(boolean textChanged)
    {
        if(textChanged) isEmpty = textComp.getText().length() == 0;
        if(isEmpty) {
            if (!textComp.hasFocus() && !isPopupOpen) {
                doSetGhostText();
            } else {
                clearGhostText();
            }
        }
        else{
            restoreForegroundColor();
        }
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        updateState(false);
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        updateState(false);
    }

    protected void doSetGhostText(){
        unregisterListeners();
        try {
            textComp.setText(ghostText);
            textComp.setForeground(ghostColor);
        }
        finally {
            registerListeners();
        }
    }

    protected void clearGhostText(){
        unregisterListeners();
        try {
            textComp.setText("");
            textComp.setForeground(foregroundColor);
        }
        finally {
            registerListeners();
        }
    }

    protected void restoreForegroundColor(){
        unregisterListeners();
        try {
            textComp.setForeground(foregroundColor);
        }
        finally {
            registerListeners();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if("foreground".equals(evt.getPropertyName())) {
            foregroundColor = textComp.getForeground();
        }
        else if("componentPopupMenu".equals(evt.getPropertyName())){
            JPopupMenu old = (JPopupMenu)evt.getOldValue();
            JPopupMenu cur = (JPopupMenu)evt.getNewValue();
            if(old != null) old.removePopupMenuListener(this);
            if(cur != null) cur.addPopupMenuListener(this);
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateState(true);
            }
        });
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateState(true);
            }
        });
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateState(true);
            }
        });
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if(isEmpty) textComp.setText("");
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if(isEmpty) textComp.setText(ghostText);
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {

    }
}