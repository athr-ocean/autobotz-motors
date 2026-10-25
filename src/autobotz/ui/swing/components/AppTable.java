package autobotz.ui.swing.components;

import autobotz.ui.swing.theme.AppTheme;
import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public final class AppTable {
  private AppTable() {}
  public static JTable create(String[] columns) {
    JTable table = new JTable(new DefaultTableModel(columns, 0) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    });
    table.setFillsViewportHeight(true);
    table.setRowHeight(38);
    table.setShowGrid(false);
    table.setIntercellSpacing(new Dimension(0, 1));
    table.setSelectionBackground(AppTheme.TABLE_SELECTION);
    table.setSelectionForeground(AppTheme.TEXT);
    JTableHeader h = table.getTableHeader();
    h.setBackground(AppTheme.TABLE_HEADER);
    h.setForeground(AppTheme.MUTED);
    h.setFont(AppTheme.BODY_BOLD);
    h.setBorder(new MatteBorder(0, 0, 1, 0, AppTheme.BORDER));
    return table;
  }
  public static DefaultTableModel model(JTable table) {
    return (DefaultTableModel) table.getModel();
  }
}
