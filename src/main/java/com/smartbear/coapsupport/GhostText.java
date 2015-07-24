package com.smartbear.ready;


import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class GhostText<TextComponent extends JTextComponent> implements FocusListener, DocumentListener, PropertyChangeListener
{
    protected final TextComponent textComp;
    protected boolean isEmpty;
    private Color ghostColor;
    private Color foregroundColor;
    protected final String ghostText;

    public GhostText(final TextComponent textComp, String ghostText)
    {
        super();
        this.textComp = textComp;
        this.ghostText = ghostText;
        this.ghostColor = Color.LIGHT_GRAY;
        textComp.addFocusListener(this);
        registerListeners();
        updateState();
        if (!this.textComp.hasFocus())
        {
            focusLost(null);
        }
    }

    public void delete()
    {
        unregisterListeners();
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

    private void updateState()
    {
        isEmpty = textComp.getText().length() == 0;
        foregroundColor = textComp.getForeground();
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        if (isEmpty)
        {
            unregisterListeners();
            try
            {
                clearGhostText();
                textComp.setForeground(foregroundColor);
            }
            finally
            {
                registerListeners();
            }
        }

    }

    @Override
    public void focusLost(FocusEvent e)
    {
        if (isEmpty)
        {
            unregisterListeners();
            try
            {
                doSetGhostText();
                textComp.setForeground(ghostColor);
            }
            finally
            {
                registerListeners();
            }
        }
    }

    protected void doSetGhostText(){
        textComp.setText(ghostText);

    }

    protected void clearGhostText(){
        textComp.setText("");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        updateState();
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        updateState();
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        updateState();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        updateState();
    }

}