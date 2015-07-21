package com.smartbear.coapsupport;

import com.eviware.soapui.support.UISupport;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Expander extends JPanel {
//    private final static ExpanderLayout layout = new ExpanderLayout();
    private JComponent content;
    private JToggleButton header;
//    private boolean expanded;

    private static ImageIcon expandedIcon;
    private static ImageIcon collapsedIcon;

    public static ImageIcon getExpandedIcon(){
        if(expandedIcon == null){
            expandedIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/arrow-expanded.png");

        }
        return expandedIcon;
    }

    public static ImageIcon getCollapsedIcon(){
        if(collapsedIcon == null){
            collapsedIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/arrow-collapsed.png");
        }
        return collapsedIcon;
    }

    public Expander(String title, final JComponent content, boolean initiallyExpanded) {
        super(new GridBagLayout());
        this.header = new JToggleButton(title){

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics = (Graphics2D) g;
                graphics.setPaint(getBackground());
                graphics.fillRect(0, 0, getWidth(), getHeight());
                //int textWidth = graphics.getFontMetrics().stringWidth(getText());
                int textX = getIcon().getIconWidth();
                int asc = graphics.getFontMetrics().getAscent();
                int desc = graphics.getFontMetrics().getDescent();
                int textY = (getHeight() + asc - desc) / 2;
                int iconY = textY - asc + (asc + desc - getIcon().getIconHeight()) / 2;
                if(textY < 0) textY = 0;
                if(isEnabled()) {
                    graphics.setPaint(getForeground());
                    getIcon().paintIcon(this, graphics, 0, iconY);
                    graphics.drawString(getText(), textX, textY);
                }
                else {
                    graphics.setPaint(Color.LIGHT_GRAY);
                    graphics.drawString(getText(), textX, textY);
                }
            }

        };
        this.header.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.content = content;
        add(header, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        add(content, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
//        JPanel dummyPanel = new JPanel();
//        dummyPanel.setPreferredSize(new Dimension(0, 0));
//        add(dummyPanel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.header.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                content.setVisible(header.isSelected());
                if(header.isSelected()) header.setIcon(getExpandedIcon()); else header.setIcon(getCollapsedIcon());
            }
        });
        setExpanded(initiallyExpanded);
    }

    public Expander(String title, final JComponent content, boolean initiallyExpanded, int minWidth, int height) {
        this(title, content, initiallyExpanded);
        content.setPreferredSize(new Dimension(minWidth, height));
    }

    public JComponent getContent(){return content;}
    public String getTitle(){return header.getText();}
    public boolean isExpanded(){
        //return expanded;
        return header.isSelected();
    }

    public void setExpanded(boolean newValue){
        header.setSelected(newValue);
        content.setVisible(newValue);
        if(header.isSelected()) header.setIcon(getExpandedIcon()); else header.setIcon(getCollapsedIcon());
    }

//    private static class ExpanderLayout implements LayoutManager{
//        private final static int vGap = 4;
//
//        @Override
//        public void addLayoutComponent(String name, Component comp) {
//
//        }
//
//        @Override
//        public void removeLayoutComponent(Component comp) {
//
//        }
//
//        @Override
//        public Dimension preferredLayoutSize(Container target) {
//            synchronized (target.getTreeLock()) {
//                Expander expander = (Expander)target;
//                Dimension headerSize = expander.header.getPreferredSize();
//                Dimension contentSize = expander.content.getPreferredSize();
//                if(expander.isExpanded()){
//                    Dimension result = new Dimension();
//                    result.setSize(Math.max(headerSize.getWidth(), contentSize.getWidth()), headerSize.getHeight() + vGap + contentSize.getHeight());
//                    return result;
//                }
//                else{
//                    return headerSize;
//                }
//            }
//        }
//
//        @Override
//        public Dimension minimumLayoutSize(Container target) {
//            synchronized (target.getTreeLock()) {
//                Expander expander = (Expander)target;
//                Dimension headerSize = expander.header.getMinimumSize();
//                Dimension contentSize = expander.content.getMinimumSize();
//                if(expander.isExpanded()){
//                    Dimension result = new Dimension();
//                    result.setSize(Math.max(headerSize.getWidth(), contentSize.getWidth()), headerSize.getHeight() + vGap + contentSize.getHeight());
//                    return result;
//                }
//                else{
//                    return headerSize;
//                }
//            }
//        }
//
//        @Override
//        public void layoutContainer(Container target) {
//            synchronized (target.getTreeLock()) {
//                Expander expander = (Expander)target;
//                Insets insets = target.getInsets();
//                int top = insets.top;
//                int bottom = target.getHeight() - insets.bottom;
//                int left = insets.left;
//                int right = target.getWidth() - insets.right;
//
//                Dimension headerSize = expander.header.getPreferredSize();
//                expander.header.setBounds(left, top, Math.min(right - left, headerSize.width), Math.min(bottom - top, headerSize.height));
//                top += expander.header.getHeight() + vGap;
//                if(expander.isExpanded()){
//                    expander.content.setVisible(true);
//                    expander.content.setBounds(left, top, Math.min(right - left, expander.content.getPreferredSize().width), Math.min(bottom - top, expander.content.getPreferredSize().height));
//                }
//                else{
//                    expander.content.setVisible(false);
//                }
//            }
//        }
//    }
}
