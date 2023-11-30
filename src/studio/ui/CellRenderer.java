package studio.ui;

import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.LimitedWriter;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

class CellRenderer extends DefaultTableCellRenderer {
    private static final Color keyColor = new Color(220, 255, 220);
    private static final Color altColor = new Color(220, 220, 255);
    private static final Color nullColor = new Color(255, 150, 150);
    private static final Color selColor = UIManager.getColor("Table.selectionBackground");
    private final Color fgColor;
    private final JTable table;

    private void initLabel(JTable table) {
        setHorizontalAlignment(SwingConstants.LEFT);
        setOpaque(true);
    }

    public CellRenderer(JTable t) {
        super();
        table = t;
        table.addPropertyChangeListener(propertyChangeEvent -> {
            if ("zoom".equals(propertyChangeEvent.getPropertyName()))
                setFont(table.getFont());
        });

        initLabel(t);
        setFont(UIManager.getFont("Table.font"));
        setBackground(UIManager.getColor("Table.background"));
        fgColor = UIManager.getColor("Table.foreground");
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        if (value instanceof K.KBase) {
            K.KBase kb = (K.KBase) value;
            LimitedWriter w = new LimitedWriter(Config.getInstance().getMaxCharsInTableCell());

            try {
                kb.toString(w, kb instanceof K.KBaseVector);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (LimitedWriter.LimitException ignored) {
            }

            setText(w.toString());
            setForeground(kb.isNull() ? nullColor : fgColor);
        }

        if (!isSelected) {
            KTableModel ktm = (KTableModel) table.getModel();
            column = table.convertColumnIndexToModel(column);
            if (ktm.isKey(column))
                setBackground(keyColor);
            else if (row % 2 == 0)
                setBackground(altColor);
            else
                setBackground(UIManager.getColor("Table.background"));
        } else {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(selColor);
        }
        /*
        int availableWidth= table.getColumnModel().getColumn(column).getWidth();
        availableWidth -= table.getIntercellSpacing().getWidth();
        Insets borderInsets = getBorder().getBorderInsets((Component)this);
        availableWidth -= (borderInsets.left + borderInsets.right);
        String cellText = getText();
        FontMetrics fm = getFontMetrics( getFont() );

        if (fm.stringWidth(cellText) > availableWidth)
        {
        String dots= "...";
        int textWidth = fm.stringWidth( dots );
        int nChars = cellText.length() - 1;
        for (; nChars > 0; nChars--)
        {
        textWidth += fm.charWidth(cellText.charAt(nChars));

        if (textWidth > availableWidth)
        {
        break;
        }
        }

        setText( dots + cellText.substring(nChars + 1));
        }
         **/

        return this;
    }
}
