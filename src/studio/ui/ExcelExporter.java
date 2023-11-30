package studio.ui;

import studio.kdb.K;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableModel;


class ExcelExporter {
    private static final SimpleDateFormat formatter = new SimpleDateFormat();

    private static synchronized String sd(String s, java.util.Date x) {
        formatter.applyPattern(s);
        return formatter.format(x);
    }

    public static String escape(String s) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(s);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '<') {
                result.append("&lt;");
            } else if (character == '>') {
                result.append("&gt;");
            } else if (character == '\"') {
                result.append("&quot;");
            } else if (character == '\'') {
                result.append("&apos;");
            } else if (character == '&') {
                result.append("&amp;");
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    public void exportTableX(final JFrame frame, final JTable table, final File file, final boolean openIt) {

        final TableModel model = table.getModel();
        final String message = "Exporting data to " + file.getAbsolutePath();
        final String note = "0% complete";
        String title = "Studio for kdb+";
        UIManager.put("ProgressMonitor.progressText", title);

        final int min = 0;
        final int max = 100;
        final ProgressMonitor pm = new ProgressMonitor(frame, message, note, min, max);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            try {
                Writer writer = new BufferedWriter(new PrintWriter(Files.newOutputStream(file.toPath())));
                writer.write("<?xml version=\"1.0\"?>\n<ss:Workbook xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
                writer.write("<ss:Styles>");
                writer.write("<ss:Style ss:ID=\"Default\" ss:Name=\"Normal\">");
                writer.write("<ss:Alignment ss:Vertical=\"Bottom\"/>");
                writer.write("<ss:Borders/>");
                writer.write("<ss:Font/>");
                writer.write("<ss:Interior/>");
                writer.write("<ss:NumberFormat/>");
                writer.write("<ss:Protection/>");
                writer.write("</ss:Style>");
                writer.write("<ss:Style ss:ID=\"bold\"><ss:Font ss:Bold=\"1\"/></ss:Style>");
                writer.write("<ss:Style ss:ID=\"time\"><ss:NumberFormat ss:Format=\"hh:mm:ss.000\"/></ss:Style>");
                writer.write("<ss:Style ss:ID=\"minute\"><ss:NumberFormat ss:Format=\"hh:mm\"/></ss:Style>");
                writer.write("<ss:Style ss:ID=\"month\"><ss:NumberFormat ss:Format=\"yyyy\\-mm\"/></ss:Style>");
                writer.write("<ss:Style ss:ID=\"second\"><ss:NumberFormat ss:Format=\"hh:mm:ss\"/></ss:Style>");
                writer.write("<ss:Style ss:ID=\"date\"><ss:NumberFormat ss:Format=\"yyyy\\-mm\\-dd\"/></ss:Style>");
                writer.write("<ss:Style ss:ID=\"datetime\"><ss:NumberFormat ss:Format=\"yyyy\\-mm\\-dd hh:mm:ss.000\"/></ss:Style>");
                writer.write("</ss:Styles>");

                writer.write("<ss:Worksheet ss:Name=\"Sheet1\">\n<ss:Table>\n");
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.write("<ss:Column ss:Width=\"80\"/>");
                }

                writer.write("\n<ss:Row>");
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.write("<ss:Cell><ss:Data ss:Type=\"String\">");
                    writer.write(escape(model.getColumnName(i)));
                    writer.write("</ss:Data></ss:Cell>");
                }
                writer.write("</ss:Row>\n");

                int maxRow = model.getRowCount();
                int lastProgress = 0;
                for (int i = 0; i < model.getRowCount(); i++) {
                    writer.write("<ss:Row>");

                    for (int j = 0; j < model.getColumnCount(); j++) {

                        K.KBase b = (K.KBase) model.getValueAt(i, j);
                        if (!b.isNull()) {
                            if (table.getColumnClass(j) == K.KSymbolVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"String\">" + escape(b.toString(false)));
                            } else if (table.getColumnClass(j) == K.KDateVector.class) {
                                writer.write("<ss:Cell ss:StyleID=\"date\"><ss:Data ss:Type=\"DateTime\">" + sd("yyyy-MM-dd", ((K.KDate) b).toDate()));
                            } else if (table.getColumnClass(j) == K.KTimeVector.class) {
                                writer.write("<ss:Cell ss:StyleID=\"time\"><ss:Data ss:Type=\"DateTime\">" + "1899-12-31T" + sd("HH:mm:ss.SSS", ((K.KTime) b).toTime()));
                            } else if (table.getColumnClass(j) == K.KTimestampVector.class) {
                                char[] cs = sd("yyyy-MM-dd HH:mm:ss.SSS", ((K.KTimestamp) b).toTimestamp()).toCharArray();
                                cs[10] = 'T';
                                writer.write("<ss:Cell ss:StyleID=\"datetime\"><ss:Data ss:Type=\"DateTime\">" + new String(cs));
                            } else if (table.getColumnClass(j) == K.KMonthVector.class) {
                                writer.write("<ss:Cell ss:StyleID=\"month\"><ss:Data ss:Type=\"DateTime\">" + sd("yyyy-MM", ((K.Month) b).toDate()));
                            } else if (table.getColumnClass(j) == K.KMinuteVector.class) {
                                writer.write("<ss:Cell ss:StyleID=\"minute\"><ss:Data ss:Type=\"DateTime\">" + "1899-12-31T" + sd("HH:mm", ((K.Minute) b).toDate()));
                            } else if (table.getColumnClass(j) == K.KSecondVector.class) {
                                writer.write("<ss:Cell ss:StyleID=\"second\"><ss:Data ss:Type=\"DateTime\">" + "1899-12-31T" + sd("HH:mm:ss", ((K.Second) b).toDate()));
                            } else if (table.getColumnClass(j) == K.KBooleanVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"Boolean\">" + (((K.KBoolean) b).b ? "1" : "0"));
                            } else if (table.getColumnClass(j) == K.KDoubleVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"Number\">" + ((K.KDouble) b).d);
                            } else if (table.getColumnClass(j) == K.KFloatVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"Number\">" + ((K.KFloat) b).f);
                            } else if (table.getColumnClass(j) == K.KLongVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"Number\">" + ((K.KLong) b).j);
                            } else if (table.getColumnClass(j) == K.KIntVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"Number\">" + ((K.KInteger) b).i);
                            } else if (table.getColumnClass(j) == K.KShortVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"Number\">" + ((K.KShort) b).s);
                            } else if (table.getColumnClass(j) == K.KCharacterVector.class) {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"String\">" + escape(String.valueOf(((K.KCharacter) b).c)));
                            } else {
                                writer.write("<ss:Cell><ss:Data ss:Type=\"String\">" + escape(K.decode(b, false)));
                            }
                        } else {
                            writer.write("<ss:Cell><ss:Data ss:Type=\"String\">");
                        }

                        writer.write("</ss:Data></ss:Cell>");
                    }

                    if (pm.isCanceled()) {
                        break;
                    } else {
                        final int progress = (100 * i) / maxRow;
                        if (progress > lastProgress) {
                            lastProgress = progress;
                            final String note1 = progress + "% complete";
                            SwingUtilities.invokeLater(() -> {
                                pm.setProgress(progress);
                                pm.setNote(note1);
                            });

                            Thread.yield();
                        }
                    }
                    writer.write("</ss:Row>\n");
                }

                writer.write("</ss:Table>\n</ss:Worksheet>\n</ss:Workbook>");
                writer.close();

                if ((!pm.isCanceled()) && openIt) {
                    openTable(file);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "\nThere was an error encoding the K types into Excel types.\n\n" + e.getMessage() + "\n\n", "Studio for kdb+", JOptionPane.ERROR_MESSAGE, Util.ERROR_ICON);
            } finally {
                pm.close();
            }
        };

        Thread t = new Thread(runner);
        t.setName("Excel Exporter");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void openTable(File file) {
        try {
            String lcOSName = System.getProperty("os.name").toLowerCase();
            boolean MAC_OS_X = lcOSName.startsWith("mac os x");
            if (MAC_OS_X) {
                new ProcessBuilder("open", file.getName()).start();
            } else {
                new ProcessBuilder("cmd.exe", "/c", "start", file.getAbsolutePath()).start();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "\nThere was an error opening excel.\n\n" + e.getMessage() + "\n\nPerhaps you do not have Excel installed,\nor .xls files are not associated with Excel", "Studio for kdb+", JOptionPane.ERROR_MESSAGE, Util.ERROR_ICON);

        }
    }
}
