package studio.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.*;

import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import static studio.ui.EscapeDialog.DialogResult.ACCEPTED;
import static studio.ui.EscapeDialog.DialogResult.CANCELLED;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.TableModel;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import kx.c;
import org.netbeans.editor.*;
import org.netbeans.editor.Utilities;
import studio.core.Credentials;
import studio.kdb.ListModel;
import studio.qeditor.QKit;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.editor.ext.ExtSettingsInitializer;
import studio.qeditor.QSettingsInitializer;
import studio.kdb.*;
import studio.utils.BrowserLaunch;
import studio.utils.OSXAdapter;
import studio.utils.SwingWorker;

public class StudioPanel extends JPanel implements WindowListener {
    static {
        // Register us
        LocaleSupport.addLocalizer(new Impl("org.netbeans.editor.Bundle"));

        Settings.addInitializer(new BaseSettingsInitializer(), Settings.CORE_LEVEL);
        Settings.addInitializer(new ExtSettingsInitializer(), Settings.CORE_LEVEL);

        QKit editorKit = new QKit();
        JEditorPane.registerEditorKitForContentType(editorKit.getContentType(),
                editorKit.getClass().getName());
        Settings.addInitializer(new QSettingsInitializer());
        Settings.reset();
    }

    private JComboBox<String> comboServer;
    private JTextField txtServer;
    private JTable table;
    private String exportFilename;
    private String lastQuery = null;
    private JMenuBar menubar;
    private JToolBar toolbar;
    private JEditorPane textArea;
    private JSplitPane splitpane;
    private JTabbedPane tabbedPane;
    private ServerList serverList;
    private UserAction arrangeAllAction;
    private UserAction closeFileAction;
    private UserAction newFileAction;
    private UserAction openFileAction;
    private UserAction openInExcel;
    private UserAction codeKxComAction;
    private UserAction serverListAction;
    private UserAction openFileInNewWindowAction;
    private UserAction saveFileAction;
    private UserAction saveAsFileAction;
    private UserAction exportAction;
    private UserAction chartAction;
    private ActionFactory.UndoAction undoAction;
    private ActionFactory.RedoAction redoAction;
    private BaseKit.CutAction cutAction;
    private BaseKit.CopyAction copyAction;
    private BaseKit.PasteAction pasteAction;
    private BaseKit.SelectAllAction selectAllAction;
    private Action findAction;
    private Action replaceAction;
    private UserAction stopAction;
    private UserAction executeAction;
    private UserAction executeCurrentLineAction;
    private UserAction refreshAction;
    private UserAction aboutAction;
    private UserAction exitAction;
    private UserAction settingsAction;
    private UserAction toggleDividerOrientationAction;
    private UserAction minMaxDividerAction;
    private UserAction editServerAction;
    private UserAction addServerAction;
    private UserAction removeServerAction;
    private static int scriptNumber = 0;
    private static int myScriptNumber;
    private JFrame frame;
    public static java.util.List windowList = Collections.synchronizedList(new LinkedList());
    private final int menuShortcutKeyMask = InputEvent.CTRL_DOWN_MASK;

    private final static int MAX_SERVERS_TO_CLONE = 20;

    public void refreshFrameTitle() {
        String s = (String) textArea.getDocument().getProperty("filename");
        if (s == null)
            s = "Script" + myScriptNumber;
        String title = s.replace('\\', '/');
        frame.setTitle(title + (getModified() ? " (not saved) " : "") + (server != null ? " @" + server : "") + " Studio for kdb+ " + Lm.getVersionString());
    }

    public static class WindowListChangedEvent extends EventObject {
        public WindowListChangedEvent(Object source) {
            super(source);
        }
    }

    public interface WindowListChangedEventListener extends EventListener {
        void WindowListChangedEventOccurred(WindowListChangedEvent evt);
    }

    public static class WindowListMonitor {
        protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();

        public synchronized void addEventListener(WindowListChangedEventListener listener) {
            listenerList.add(WindowListChangedEventListener.class, listener);
        }

        public synchronized void removeEventListener(WindowListChangedEventListener listener) {
            listenerList.remove(WindowListChangedEventListener.class, listener);
        }

        synchronized void fireMyEvent(WindowListChangedEvent evt) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = 0; i < listeners.length; i += 2)
                if (listeners[i] == WindowListChangedEventListener.class)
                    ((WindowListChangedEventListener) listeners[i + 1]).WindowListChangedEventOccurred(evt);
        }
    }

    public static WindowListMonitor windowListMonitor = new WindowListMonitor();

    private void updateUndoRedoState(UndoManager um) {
        undoAction.setEnabled(um.canUndo());
        redoAction.setEnabled(um.canRedo());
    }

    private void initDocument() {
        initActions();
        refreshActionState();

        Document doc;
        if (textArea == null) {
            textArea = new JEditorPane("text/q", "");
            Action[] actions = textArea.getActions();

            for (Action action : actions)
                if (action instanceof BaseKit.CopyAction) {
                    copyAction = (BaseKit.CopyAction) action;
                    copyAction.putValue(Action.SHORT_DESCRIPTION, "Copy the selected text to the clipboard");
                    copyAction.putValue(Action.SMALL_ICON, Util.COPY_ICON);
                    copyAction.putValue(Action.NAME, I18n.getString("Copy"));
                    copyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
                    copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask));
                } else if (action instanceof BaseKit.CutAction) {
                    cutAction = (BaseKit.CutAction) action;
                    cutAction.putValue(Action.SHORT_DESCRIPTION, "Cut the selected text");
                    cutAction.putValue(Action.SMALL_ICON, Util.CUT_ICON);
                    cutAction.putValue(Action.NAME, I18n.getString("Cut"));
                    cutAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
                    cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask));
                } else if (action instanceof BaseKit.PasteAction) {
                    pasteAction = (BaseKit.PasteAction) action;
                    pasteAction.putValue(Action.SHORT_DESCRIPTION, "Paste text from the clipboard");
                    pasteAction.putValue(Action.SMALL_ICON, Util.PASTE_ICON);
                    pasteAction.putValue(Action.NAME, I18n.getString("Paste"));
                    pasteAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
                    pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask));
                } else if (action instanceof ExtKit.FindAction) {
                    findAction = action;
                    findAction.putValue(Action.SHORT_DESCRIPTION, "Find text in the document");
                    findAction.putValue(Action.SMALL_ICON, Util.FIND_ICON);
                    findAction.putValue(Action.NAME, I18n.getString("Find"));
                    findAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
                    findAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutKeyMask));
                } else if (action instanceof ExtKit.ReplaceAction) {
                    replaceAction = action;
                    replaceAction.putValue(Action.SHORT_DESCRIPTION, "Replace text in the document");
                    replaceAction.putValue(Action.SMALL_ICON, Util.REPLACE_ICON);
                    replaceAction.putValue(Action.NAME, I18n.getString("Replace"));
                    replaceAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
                    replaceAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, menuShortcutKeyMask));
                } else if (action instanceof BaseKit.SelectAllAction) {
                    selectAllAction = (BaseKit.SelectAllAction) action;
                    selectAllAction.putValue(Action.SHORT_DESCRIPTION, "Select all text in the document");
                    selectAllAction.putValue(Action.SMALL_ICON, null);
                    selectAllAction.putValue(Action.NAME, I18n.getString("SelectAll"));
                    selectAllAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
                    selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask));
                } else if (action instanceof ActionFactory.UndoAction) {
                    undoAction = (ActionFactory.UndoAction) action;
                    undoAction.putValue(Action.SHORT_DESCRIPTION, "Undo the last change to the document");
                    undoAction.putValue(Action.SMALL_ICON, Util.UNDO_ICON);
                    undoAction.putValue(Action.NAME, I18n.getString("Undo"));
                    undoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
                    undoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcutKeyMask));
                } else if (action instanceof ActionFactory.RedoAction) {
                    redoAction = (ActionFactory.RedoAction) action;
                    redoAction.putValue(Action.SHORT_DESCRIPTION, "Redo the last change to the document");
                    redoAction.putValue(Action.SMALL_ICON, Util.REDO_ICON);
                    redoAction.putValue(Action.NAME, I18n.getString("Redo"));
                    redoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
                    redoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutKeyMask));
                }

            doc = textArea.getDocument();
            doc.putProperty("filename", null);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
            //  doc.putProperty("created", Boolean.TRUE);
        } else
            doc = textArea.getDocument();

        JComponent c = (textArea.getUI() instanceof BaseTextUI) ? Objects.requireNonNull(Utilities.getEditorUI(textArea)).getExtComponent() : new JScrollPane(textArea);

        doc.putProperty("server", server);

        MarkingDocumentListener mdl = (MarkingDocumentListener) doc.getProperty("MarkingDocumentListener");
        if (mdl == null) {
            mdl = new MarkingDocumentListener(c);
            doc.putProperty("MarkingDocumentListener", mdl);
            doc.addDocumentListener(mdl);
        }
        mdl.setModified(false);

        UndoManager um = (UndoManager) doc.getProperty(BaseDocument.UNDO_MANAGER_PROP);
        if (um == null) {
            um = new UndoManager() {
                public void undoableEditHappened(UndoableEditEvent e) {
                    super.undoableEditHappened(e);
                    updateUndoRedoState(this);
                }

                public synchronized void redo() throws CannotRedoException {
                    super.redo();
                    updateUndoRedoState(this);
                }

                public synchronized void undo() throws CannotUndoException {
                    super.undo();
                    updateUndoRedoState(this);
                }
            };
            doc.putProperty(BaseDocument.UNDO_MANAGER_PROP, um);
            doc.addUndoableEditListener(um);
        }
        um.discardAllEdits();
        updateUndoRedoState(um);

        if (splitpane.getTopComponent() != c) {
            splitpane.setTopComponent(c);
            splitpane.setDividerLocation(0.5);
        }

        rebuildToolbar();
        rebuildMenuBar();

        textArea.requestFocus();
    }

    private void refreshActionState() {
        newFileAction.setEnabled(true);
        arrangeAllAction.setEnabled(true);
        openFileAction.setEnabled(true);
        serverListAction.setEnabled(true);
        openFileInNewWindowAction.setEnabled(true);
        saveFileAction.setEnabled(true);
        saveAsFileAction.setEnabled(true);
        exportAction.setEnabled(false);
        chartAction.setEnabled(false);
        openInExcel.setEnabled(false);
        stopAction.setEnabled(false);
        executeAction.setEnabled(true);
        executeCurrentLineAction.setEnabled(true);
        refreshAction.setEnabled(false);

//        helpAction.setEnabled(true);
        aboutAction.setEnabled(true);
        exitAction.setEnabled(true);
        settingsAction.setEnabled(true);
    }

    private String getFilename() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        FileFilter ff =
                new FileFilter() {
                    public String getDescription() {
                        return "q script";
                    }

                    public boolean accept(File file) {
                        return file.isDirectory() || file.getName().endsWith(".q");
                    }
                };

        chooser.addChoosableFileFilter(ff);

        chooser.setFileFilter(ff);

        String filename = (String) textArea.getDocument().getProperty("filename");
        if (filename != null) {
            File file = new File(filename);
            File dir = new File(file.getPath());
            chooser.setCurrentDirectory(dir);
        }

        int option = chooser.showOpenDialog(textArea);

        if (option == JFileChooser.APPROVE_OPTION) {
            File sf = chooser.getSelectedFile();
            File f = chooser.getCurrentDirectory();
            String dir = f.getAbsolutePath();

            try {
                filename = dir + "/" + sf.getName();
                return filename;
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private void exportAsExcel(final String filename) {
        new ExcelExporter().exportTableX(frame, table, new File(filename), false);
    }

    private void exportAsDelimited(final TableModel model, final String filename, final char delimiter) {
        final String message = "Exporting data to " + filename;

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
            if (filename != null) {
                String lineSeparator = System.getProperty("line.separator");
                BufferedWriter fw;

                try {
                    fw = new BufferedWriter(new FileWriter(filename));

                    for (int col = 0; col < model.getColumnCount(); col++) {
                        if (col > 0)
                            fw.write(delimiter);

                        fw.write(model.getColumnName(col));
                    }
                    fw.write(lineSeparator);

                    int maxRow = model.getRowCount();
                    int lastProgress = 0;

                    for (int r = 1; r <= maxRow; r++) {
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            if (col > 0)
                                fw.write(delimiter);

                            K.KBase o = (K.KBase) model.getValueAt(r - 1, col);
                            if (!o.isNull())
                                fw.write(o.toString(false));
                        }
                        fw.write(lineSeparator);

                        boolean cancelled = pm.isCanceled();

                        if (cancelled)
                            break;
                        else {
                            final int progress = (100 * r) / maxRow;
                            if (progress > lastProgress) {
                                final String note1 = progress + "% complete";
                                SwingUtilities.invokeLater(() -> {
                                    pm.setProgress(progress);
                                    pm.setNote(note1);
                                });

                                Thread.yield();
                            }
                        }
                    }

                    fw.close();
                } catch (Exception ex) {
                    ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                } finally {
                    pm.close();
                }
            }
        };

        Thread t = new Thread(runner);
        t.setName("export");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void exportAsXml(final TableModel model, final String filename) {
        final String message = "Exporting data to " + filename;

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
            if (filename != null) {
                String lineSeparator = System.getProperty("line.separator");

                BufferedWriter fw;

                try {
                    fw = new BufferedWriter(new FileWriter(filename));

                    fw.write("<R>");

                    int maxRow = model.getRowCount();
                    int lastProgress = 0;

                    fw.write(lineSeparator);

                    String[] columns = new String[model.getColumnCount()];
                    for (int col = 0; col < model.getColumnCount(); col++)
                        columns[col] = model.getColumnName(col);

                    for (int r = 1; r <= maxRow; r++) {
                        fw.write("<r>");
                        for (int col = 0; col < columns.length; col++) {
                            fw.write("<" + columns[col] + ">");

                            K.KBase o = (K.KBase) model.getValueAt(r - 1, col);
                            if (!o.isNull())
                                fw.write(o.toString(false));

                            fw.write("</" + columns[col] + ">");
                        }
                        fw.write("</r>");
                        fw.write(lineSeparator);

                        boolean cancelled = pm.isCanceled();

                        if (cancelled)
                            break;
                        else {
                            final int progress = (100 * r) / maxRow;
                            if (progress > lastProgress) {
                                final String note1 = progress + "% complete";
                                SwingUtilities.invokeLater(() -> {
                                    pm.setProgress(progress);
                                    pm.setNote(note1);
                                });

                                Thread.yield();
                            }
                        }
                    }
                    fw.write("</R>");

                    fw.close();
                } catch (Exception ex) {
                    ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                } finally {
                    pm.close();
                }
            }
        };

        Thread t = new Thread(runner);
        t.setName("export");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void exportAsTxt(String filename) {
        exportAsDelimited(table.getModel(), filename, '\t');
    }

    private void exportAsCSV(String filename) {
        exportAsDelimited(table.getModel(), filename, ',');
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Export result set as");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        FileFilter csvFilter = null;
        FileFilter txtFilter = null;
        FileFilter xmlFilter = null;
        FileFilter xlsFilter = null;

        if (table != null) {
            csvFilter =
                    new FileFilter() {
                        public String getDescription() {
                            return "csv (Comma delimited)";
                        }

                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().endsWith(".csv");
                        }
                    };

            txtFilter =
                    new FileFilter() {
                        public String getDescription() {
                            return "txt (Tab delimited)";
                        }

                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().endsWith(".txt");
                        }
                    };

            xmlFilter =
                    new FileFilter() {
                        public String getDescription() {
                            return "xml";
                        }

                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().endsWith(".xml");
                        }
                    };


            xlsFilter =
                    new FileFilter() {
                        public String getDescription() {
                            return "xls (Microsoft Excel)";
                        }

                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().endsWith(".xls");
                        }
                    };

            chooser.addChoosableFileFilter(csvFilter);
            chooser.addChoosableFileFilter(txtFilter);
            chooser.addChoosableFileFilter(xmlFilter);
            chooser.addChoosableFileFilter(xlsFilter);
        }

        if (exportFilename != null) {
            File file = new File(exportFilename);
            File dir = new File(file.getPath());
            chooser.setCurrentDirectory(dir);
            chooser.ensureFileIsVisible(file);
            if (table != null)
                if (exportFilename.endsWith(".xls"))
                    chooser.setFileFilter(xlsFilter);
                else if (exportFilename.endsWith(".csv"))
                    chooser.setFileFilter(csvFilter);
                else if (exportFilename.endsWith(".xml"))
                    chooser.setFileFilter(xmlFilter);
                else if (exportFilename.endsWith(".txt"))
                    chooser.setFileFilter(txtFilter);
        }

        int option = chooser.showSaveDialog(textArea);

        if (option == JFileChooser.APPROVE_OPTION) {
            File sf = chooser.getSelectedFile();
            File f = chooser.getCurrentDirectory();
            String dir = f.getAbsolutePath();

//            Cursor cursor= frame.getCursor();

            try {
                //              frame.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                FileFilter ff = chooser.getFileFilter();

                exportFilename = dir + "/" + sf.getName();

                if (table != null)
                    if (exportFilename.endsWith(".xls"))
                        exportAsExcel(exportFilename);
                    else if (exportFilename.endsWith(".csv"))
                        exportAsCSV(exportFilename);
                    else if (exportFilename.endsWith(".txt"))
                        exportAsTxt(exportFilename);
                    else if (exportFilename.endsWith(".xml"))
                        exportAsXml(table.getModel(), exportFilename);
                    /*                    else if (exportFilename.endsWith(".res")) {
                    exportAsBin(exportFilename);
                    }
                     */
                    else if (ff == csvFilter)
                        exportAsCSV(exportFilename);
                    else if (ff == xlsFilter)
                        exportAsExcel(exportFilename);
                    else if (ff == txtFilter)
                        exportAsTxt(exportFilename);
                    else if (ff == xmlFilter)
                        exportAsXml(table.getModel(), exportFilename);
                        /*else if( ff == binFilter){
                        exportAsBin(exportFilename);
                        }
                         */
                    else
                        JOptionPane.showMessageDialog(frame,
                                "Warning",
                                "You did not specify what format to export the file as.\n Cancelling data export",
                                JOptionPane.WARNING_MESSAGE,
                                Util.WARNING_ICON);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                        "Error",
                        "An error occurred whilst writing the export file.\n Details are: " + e.getMessage(),
                        JOptionPane.ERROR_MESSAGE,
                        Util.ERROR_ICON);
            }
        }
    }

    public void newFile() {
        try {
            String filename = (String) textArea.getDocument().getProperty("filename");
            if (!saveIfModified(filename))
                return;

            textArea.getDocument().remove(0, textArea.getDocument().getLength());
            textArea.getDocument().putProperty("filename", null);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
            initDocument();
            refreshFrameTitle();
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void openFile() {
        String filename = (String) textArea.getDocument().getProperty("filename");
        if (!saveIfModified(filename))
            return;

        filename = getFilename();

        if (filename != null) {
            loadFile(filename);
            addToMruFiles(filename);
        }
    }

    // returns true to continue
    public boolean saveIfModified(String filename) {
        if (getModified()) {
            int choice = JOptionPane.showOptionDialog(frame,
                    "Changes not saved.\nSave now?",
                    "Save changes?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Util.QUESTION_ICON,
                    null, // use standard button titles
                    null);      // no default selection

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    if (saveFile(filename, false))
                        // was cancelled so return
                        return false;
                } catch (Exception e) {
                    return false;
                }
                return true;
            } else return (choice != JOptionPane.CANCEL_OPTION) && (choice != JOptionPane.CLOSED_OPTION);
        }
        return true;
    }

    public void loadMRUFile(String filename, String oldFilename) {
        if (!saveIfModified(oldFilename))
            return;

        loadFile(filename);
        addToMruFiles(filename);
        setServer(server);
    }

    private void addToMruFiles(String filename) {
        if (filename == null)
            return;

        Vector v = new Vector();
        v.add(filename);
        String[] mru = Config.getInstance().getMRUFiles();
        for (String s : mru)
            if (!v.contains(s))
                v.add(s);
        Config.getInstance().saveMRUFiles((String[]) v.toArray(new String[0]));
        rebuildMenuBar();
    }

    static public String getContents(File aFile) {
        StringBuilder contents = new StringBuilder();

        try {
            InputStreamReader isr = new InputStreamReader(Files.newInputStream(aFile.toPath()),
                    StandardCharsets.UTF_8);
            try (BufferedReader input = new BufferedReader(isr)) {
                String line;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return contents.toString();
    }

    public void loadFile(String filename) {
        try {
            String s = getContents(new File(filename));

            textArea.getDocument().remove(0, textArea.getDocument().getLength());
            textArea.getDocument().insertString(0, s, null);
            textArea.getDocument().putProperty("filename", filename);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
            initDocument();
            textArea.setCaretPosition(0);
            refreshFrameTitle();
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public boolean saveAsFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Save script as");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        FileFilter ff =
                new FileFilter() {
                    public String getDescription() {
                        return "q script";
                    }

                    public boolean accept(File file) {
                        return file.isDirectory() || file.getName().endsWith(".q");
                    }
                };

        chooser.addChoosableFileFilter(ff);

        chooser.setFileFilter(ff);

        String filename = (String) textArea.getDocument().getProperty("filename");
        if (filename != null) {
            File file = new File(filename);
            File dir = new File(file.getPath());
            chooser.setCurrentDirectory(dir);
        }

//        chooser.setMultiSelectionEnabled(true);
        int option = chooser.showSaveDialog(textArea);

        if (option == JFileChooser.APPROVE_OPTION) {
            File sf = chooser.getSelectedFile();
            File f = chooser.getCurrentDirectory();
            String dir = f.getAbsolutePath();

            try {
                filename = dir + "/" + sf.getName();
                sf = new File(filename);

                if (sf.exists()) {
                    int choice = JOptionPane.showOptionDialog(frame,
                            filename + " already exists.\nOverwrite?",
                            "Overwrite?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            Util.QUESTION_ICON,
                            null, // use standard button titles
                            null);      // no default selection

                    if (choice != JOptionPane.YES_OPTION)
                        return false;
                }

                return saveFile(filename, true);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    //   private boolean wasLoaded=false;
    // returns true if saved, false if error or cancelled
    public boolean saveFile(String filename, boolean force) {
        if (filename == null)
            return saveAsFile();

        try {
            if (!force)
                if (null == textArea.getDocument().getProperty("filename"))
                    return saveAsFile();

            textArea.write(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(filename)), StandardCharsets.UTF_8)));
            textArea.getDocument().putProperty("filename", filename);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
            setModified(false);
            addToMruFiles(filename);
            refreshFrameTitle();
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    private void arrangeAll() {
        int noWins = windowList.size();

        Iterator i = windowList.iterator();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int noRows = Math.min(noWins, 3);
        int height = screenSize.height / noRows;

        for (int row = 0; row < noRows; row++) {
            int noCols = (noWins / 3);

            if ((row == 0) && ((noWins % 3) > 0))
                noCols++;
            else if ((row == 1) && ((noWins % 3) > 1))
                noCols++;

            int width = screenSize.width / noCols;

            for (int col = 0; col < noCols; col++) {
                Object o = i.next();
                JFrame f;

                if (o instanceof StudioPanel)
                    f = ((StudioPanel) o).frame;
                else
                    f = (JFrame) o;

                f.setSize(width, height);
                f.setLocation(col * width, ((noRows - 1) - row) * height);
                ensureDeiconified(f);
            }
        }
    }

    private void setModified(boolean value) {
        if (textArea != null) {
            Document doc = textArea.getDocument();

            if (doc != null) {
                MarkingDocumentListener mdl = (MarkingDocumentListener) doc.getProperty("MarkingDocumentListener");
                if (mdl != null)
                    mdl.setModified(value);
            }
        }
    }

    private boolean getModified() {
        if (textArea != null) {
            Document doc = textArea.getDocument();

            if (doc != null) {
                MarkingDocumentListener mdl = (MarkingDocumentListener) doc.getProperty("MarkingDocumentListener");
                if (mdl != null)
                    return mdl.getModified();
            }
        }

        return true;
    }

    private void setServer(Server server) {
        if (server == null)
            return;

        this.server = server;

        if (textArea != null) {
            Document doc = textArea.getDocument();

            if (doc != null)
                doc.putProperty("server", server);
            Objects.requireNonNull(Utilities.getEditorUI(textArea)).getComponent().setBackground(server.getBackgroundColor());
        }

        new ReloadQKeywords(server);
        Config.getInstance().setLRUServer(server);

        refreshFrameTitle();
        windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
    }

    private void initActions() {
        newFileAction = new UserAction(I18n.getString("New"),
                Util.NEW_DOCUMENT_ICON,
                "Create a blank script",
                KeyEvent.VK_N,
                null) {
            public void actionPerformed(ActionEvent e) {
                //   PrintUtilities.printComponent(textArea);
                newFile();
            }
        };

        arrangeAllAction = new UserAction(I18n.getString("ArrangeAll"),
                Util.BLANK_ICON,
                "Arrange all windows on screen",
                KeyEvent.VK_A,
                null) {
            public void actionPerformed(ActionEvent e) {
                arrangeAll();
            }
        };

        minMaxDividerAction = new UserAction(I18n.getString("MaximizeEditorPane"),
                Util.BLANK_ICON,
                "Maximize editor pane",
                KeyEvent.VK_M,
                KeyStroke.getKeyStroke(KeyEvent.VK_M, menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                minMaxDivider();
            }
        };

        toggleDividerOrientationAction = new UserAction(I18n.getString("ToggleDividerOrientation"),
                Util.BLANK_ICON,
                "Toggle the window divider's orientation",
                KeyEvent.VK_C,
                null) {
            public void actionPerformed(ActionEvent e) {
                toggleDividerOrientation();
            }
        };

        closeFileAction = new UserAction(I18n.getString("Close"),
                Util.BLANK_ICON,
                "Close current document",
                KeyEvent.VK_C,
                null) {
            public void actionPerformed(ActionEvent e) {
                quitWindow();
                if (windowList.isEmpty())
                    System.exit(0);
            }
        };

        openFileAction = new UserAction(I18n.getString("Open"),
                Util.FOLDER_ICON,
                "Open a script",
                KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        };

        openFileInNewWindowAction = new UserAction(I18n.getString("NewWindow"),
                Util.BLANK_ICON,
                "Open a new window",
                KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                new StudioPanel(server, null);
            }
        };

        serverListAction = new UserAction(I18n.getString("ServerList"),
                Util.TEXT_TREE_ICON,
                "Show sever list",
                KeyEvent.VK_L,
                KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcutKeyMask | InputEvent.SHIFT_DOWN_MASK)) {
            public void actionPerformed(ActionEvent e) {
                if (serverList == null) {

                    Point location = frame.getLocation();
                    GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
                    Rectangle screenBounds = Stream.of(devices)
                            .map(d -> d.getDefaultConfiguration().getBounds())
                            .filter(b -> b.contains(location))
                            .findFirst().orElse(null);

                    Rectangle bounds = Config.getInstance().getServerListBounds();
                    bounds.translate(frame.getX(), frame.getY());

                    serverList = new ServerList(frame);
                    if (screenBounds != null && screenBounds.contains(bounds)) {
                        serverList.setBounds(bounds);
                    } else {
                        serverList.align();
                    }
                }
                serverList.updateServerTree(Config.getInstance().getServerTree(), server);
                serverList.setVisible(true);

                Rectangle bounds = serverList.getBounds();
                bounds.translate(-frame.getX(), -frame.getY());
                Config.getInstance().setServerListBounds(bounds);

                Server selectedServer = serverList.getSelectedServer();
                if (selectedServer == null || selectedServer.equals(server)) return;

                setServer(selectedServer);
                rebuildToolbar();
            }
        };

        editServerAction = new UserAction(I18n.getString("Edit"),
                Util.SERVER_INFORMATION_ICON,
                "Edit the server details",
                KeyEvent.VK_E,
                null) {
            public void actionPerformed(ActionEvent e) {
                Server s = new Server(server);

                EditServerForm f = new EditServerForm(frame, s);
                f.alignAndShow();
                if (f.getResult() == ACCEPTED) {
                    if (stopAction.isEnabled())
                        stopAction.actionPerformed(e);

                    ConnectionPool.getInstance().purge(server);
                    Config.getInstance().removeServer(server);

                    s = f.getServer();
                    Config.getInstance().addServer(s);
                    setServer(s);
                    rebuildToolbar();
                    rebuildMenuBar();

                    windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
                }
            }
        };


        addServerAction = new UserAction(I18n.getString("Add"),
                Util.ADD_SERVER_ICON,
                "Configure a new server",
                KeyEvent.VK_A,
                null) {
            public void actionPerformed(ActionEvent e) {
                AddServerForm f = new AddServerForm(frame);
                f.alignAndShow();
                if (f.getResult() == ACCEPTED) {
                    Server s = f.getServer();
                    Config.getInstance().addServer(s);
                    ConnectionPool.getInstance().purge(s);   //?
                    setServer(s);
                    rebuildToolbar();
                    rebuildMenuBar();
                    windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
                }
            }
        };

        removeServerAction = new UserAction(I18n.getString("Remove"),
                Util.DELETE_SERVER_ICON,
                "Remove this server",
                KeyEvent.VK_R,
                null) {
            public void actionPerformed(ActionEvent e) {
                int choice = JOptionPane.showOptionDialog(frame,
                        "Remove server " + server.getFullName() + " from list?",
                        "Remove server?",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        Util.QUESTION_ICON,
                        null, // use standard button titles
                        null);      // no default selection

                if (choice == 0) {
                    Config.getInstance().removeServer(server);

                    Server[] servers = Config.getInstance().getServers();

                    if (servers.length > 0)
                        setServer(servers[0]);

                    rebuildToolbar();
                    rebuildMenuBar();
                    windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
                }
            }
        };


        saveFileAction = new UserAction(I18n.getString("Save"),
                Util.DISKS_ICON,
                "Save the script",
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                String filename = (String) textArea.getDocument().getProperty("filename");
                saveFile(filename, false);
            }
        };

        saveAsFileAction = new UserAction(I18n.getString("SaveAs"),
                Util.SAVE_AS_ICON,
                "Save script as",
                KeyEvent.VK_A,
                null) {
            public void actionPerformed(ActionEvent e) {
                saveAsFile();
            }
        };

        exportAction = new UserAction(I18n.getString("Export"),
                Util.EXPORT_ICON,
                "Export result set",
                KeyEvent.VK_E,
                null) {
            public void actionPerformed(ActionEvent e) {
                export();
            }
        };

        chartAction = new UserAction(I18n.getString("Chart"),
                Util.CHART_ICON,
                "Chart current data set",
                KeyEvent.VK_E,
                null) {
            public void actionPerformed(ActionEvent e) {
                new LineChart((KTableModel) table.getModel());
                //new PriceVolumeChart(table);
            }
        };


        stopAction = new UserAction(I18n.getString("Stop"),
                Util.STOP_ICON,
                "Stop the query",
                KeyEvent.VK_S,
                null) {
            public void actionPerformed(ActionEvent e) {
                if (worker != null) {
                    worker.interrupt();
                    stopAction.setEnabled(false);
                    textArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        };


        openInExcel = new UserAction(I18n.getString("OpenInExcel"),
                Util.EXCEL_ICON,
                "Open in Excel",
                KeyEvent.VK_O,
                null) {

            public void actionPerformed(ActionEvent e) {
                try {
                    File file = File.createTempFile("studioExport", ".xls");
                    new ExcelExporter().exportTableX(frame, table, file, true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };


        executeAction = new UserAction(I18n.getString("Execute"),
                Util.TABLE_SQL_RUN_ICON,
                "Execute the full or highlighted text as a query",
                KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, menuShortcutKeyMask)) {

            public void actionPerformed(ActionEvent e) {
                executeQuery();
            }
        };


        executeCurrentLineAction = new UserAction(I18n.getString("ExecuteCurrentLine"),
                Util.RUN_ICON,
                "Execute the current line as a query",
                KeyEvent.VK_ENTER,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuShortcutKeyMask)) {

            public void actionPerformed(ActionEvent e) {
                executeQueryCurrentLine();
            }
        };


        refreshAction = new UserAction(I18n.getString("Refresh"),
                Util.REFRESH_ICON,
                "Refresh the result set",
                KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutKeyMask | InputEvent.SHIFT_DOWN_MASK)) {

            public void actionPerformed(ActionEvent e) {
                refreshQuery();
            }
        };

        aboutAction = new UserAction(I18n.getString("About"),
                Util.ABOUT_ICON,
                "About Studio for kdb+",
                KeyEvent.VK_E,
                null) {

            public void actionPerformed(ActionEvent e) {
                about();
            }
        };

        exitAction = new UserAction(I18n.getString("Exit"),
                Util.BLANK_ICON,
                "Close this window",
                KeyEvent.VK_X,
                null) {

            public void actionPerformed(ActionEvent e) {
                if (quit())
                    System.exit(0);
            }
        };

        settingsAction = new UserAction("Settings",
                Util.BLANK_ICON,
                "Settings",
                KeyEvent.VK_S,
                null) {

            public void actionPerformed(ActionEvent e) {
                settings();
            }
        };

        codeKxComAction = new UserAction("code.kx.com",
                Util.TEXT_ICON,
                "Open code.kx.com",
                KeyEvent.VK_C,
                null) {

            public void actionPerformed(ActionEvent e) {
                try {
                    BrowserLaunch.openURL("http://code.kx.com/trac/wiki/Reference");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error attempting to launch web browser:\n" + ex.getLocalizedMessage());
                }
            }
        };
    }

    public void settings() {
        SettingsDialog dialog = new SettingsDialog(frame);
        dialog.alignAndShow();
        if (dialog.getResult() == CANCELLED) return;

        String auth = dialog.getDefaultAuthenticationMechanism();
        Config.getInstance().setDefaultAuthMechanism(auth);
        Config.getInstance().setDefaultCredentials(auth, new Credentials(dialog.getUser(), dialog.getPassword()));
        Config.getInstance().setShowServerComboBox(dialog.isShowServerComboBox());
        Config.getInstance().setResultTabsCount(dialog.getResultTabsCount());
        Config.getInstance().setMaxCharsInResult(dialog.getMaxCharsInResult());
        Config.getInstance().setMaxCharsInTableCell(dialog.getMaxCharsInTableCell());

        String lfClass = dialog.getLookAndFeelClassName();
        if (!lfClass.equals(UIManager.getLookAndFeel().getClass().getName())) {
            Config.getInstance().setLookAndFeel(lfClass);
            JOptionPane.showMessageDialog(frame, "Look and Feel was changed. New L&F will take effect on the next start up.", "Look and Feel Setting Changed", JOptionPane.INFORMATION_MESSAGE);
        }

        rebuildToolbar();
    }

    public void about() {
        HelpDialog help = new HelpDialog(frame);
        Util.centerChildOnParent(help, frame);
        // help.setTitle("About Studio for kdb+");
        help.pack();
        help.setVisible(true);
    }

    public boolean quit() {
        boolean okToExit = true;

        Object[] objs = windowList.toArray();

        for (Object o : objs) {
            if (o instanceof StudioPanel) {
                if (!((StudioPanel) o).quitWindow())
                    okToExit = false;
            } else if (o instanceof JFrame) {
                JFrame f = (JFrame) o;
                f.setVisible(false);

                f.dispose();
            }
        }

        return okToExit;
    }

    public boolean quitWindow() {
        if (getModified()) {
            int choice = JOptionPane.showOptionDialog(frame,
                    "Changes not saved.\nSave now?",
                    "Save changes?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Util.QUESTION_ICON,
                    null, // use standard button titles
                    null);      // no default selection

            if (choice == 0)
                try {
                    String filename = (String) textArea.getDocument().getProperty("filename");
                    if (!saveFile(filename, false))
                        // was cancelled so return
                        return false;
                } catch (Exception e) {
                    return false;
                }
            else if ((choice == 2) || (choice == JOptionPane.CLOSED_OPTION))
                return false;
        }

        windowList.remove(this);
        windowListMonitor.removeEventListener(windowListChangedEventListener);
        windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
        frame.dispose();

        return true;
    }

    private void rebuildMenuBar() {
        menubar = createMenuBar();
        SwingUtilities.invokeLater(
                () -> {
                    if (frame != null) {
                        frame.setJMenuBar(menubar);
                        menubar.validate();
                        menubar.repaint();
                        frame.validate();
                        frame.repaint();
                    }
                });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(I18n.getString("File"));
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(new JMenuItem(newFileAction));
        menu.add(new JMenuItem(openFileAction));
        menu.add(new JMenuItem(saveFileAction));
        menu.add(new JMenuItem(saveAsFileAction));

        menu.add(new JMenuItem(closeFileAction));

        if (!Util.MAC_OS_X) {
            menu.add(new JMenuItem(settingsAction));
        }
        menu.addSeparator();
//        menu.add(new JMenuItem(importAction));
        menu.add(new JMenuItem(openInExcel));
        menu.addSeparator();
        menu.add(new JMenuItem(exportAction));
        menu.addSeparator();
        menu.add(new JMenuItem(chartAction));

        String[] mru = Config.getInstance().getMRUFiles();

        if (mru.length > 0) {
            menu.addSeparator();
            char[] mnems = "123456789".toCharArray();

            for (int i = 0; i < (Math.min(mru.length, mnems.length)); i++) {
                final String filename = mru[i];

                JMenuItem item = new JMenuItem((i + 1) + " " + filename);
                item.setMnemonic(mnems[i]);
                item.setIcon(Util.BLANK_ICON);
                item.addActionListener(e -> loadMRUFile(filename, (String) textArea.getDocument().getProperty("filename")));
                menu.add(item);
            }
        }

        if (!Util.MAC_OS_X) {
            menu.addSeparator();
            menu.add(new JMenuItem(exitAction));
        }
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Edit"));
        menu.setMnemonic(KeyEvent.VK_E);
        menu.add(new JMenuItem(undoAction));
        menu.add(new JMenuItem(redoAction));
        menu.addSeparator();
        menu.add(new JMenuItem(cutAction));
        menu.add(new JMenuItem(copyAction));
        menu.add(new JMenuItem(pasteAction));
        menu.addSeparator();
        menu.add(new JMenuItem(selectAllAction));
        menu.addSeparator();
        menu.add(new JMenuItem(findAction));
        menu.add(new JMenuItem(replaceAction));
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Server"));
        menu.setMnemonic(KeyEvent.VK_S);
        menu.add(new JMenuItem(addServerAction));
        menu.add(new JMenuItem(editServerAction));
        menu.add(new JMenuItem(removeServerAction));

        Server[] servers = Config.getInstance().getServers();
        if (servers.length > 0) {
            JMenu subMenu = new JMenu(I18n.getString("Clone"));
            subMenu.setIcon(Util.DATA_COPY_ICON);

            int count = MAX_SERVERS_TO_CLONE;
            for (final Server s : servers) {
                if (!s.equals(server) && count <= 0) continue;
                count--;
                JMenuItem item = new JMenuItem(s.getFullName());
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Server clone = new Server(s);
                        clone.setName("Clone of " + clone.getName());

                        EditServerForm f = new EditServerForm(frame, clone);
                        f.alignAndShow();

                        if (f.getResult() == ACCEPTED) {
                            clone = f.getServer();
                            Config.getInstance().addServer(clone);
                            //ebuildToolbar();
                            setServer(clone);
                            ConnectionPool.getInstance().purge(clone); //?
                            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
                        }
                    }
                });

                subMenu.add(item);
            }

            menu.add(subMenu);
        }

        menubar.add(menu);

        menu = new JMenu(I18n.getString("Query"));
        menu.setMnemonic(KeyEvent.VK_Q);
        menu.add(new JMenuItem(executeCurrentLineAction));
        menu.add(new JMenuItem(executeAction));
        menu.add(new JMenuItem(stopAction));
        menu.add(new JMenuItem(refreshAction));
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Window"));
        menu.setMnemonic(KeyEvent.VK_W);

        menu.add(new JMenuItem(minMaxDividerAction));
        menu.add(new JMenuItem(toggleDividerOrientationAction));
        menu.add(new JMenuItem(openFileInNewWindowAction));
        menu.add(new JMenuItem(arrangeAllAction));
        menu.add(new JMenuItem(serverListAction));

        if (!windowList.isEmpty()) {
            menu.addSeparator();

            int i = 0;

            for (Object object : windowList) {
                String t = "unknown";

                final Object o = object;

                if (o instanceof StudioPanel) {
                    StudioPanel r = (StudioPanel) o;
                    String filename = (String) r.textArea.getDocument().getProperty("filename");

                    if (filename != null)
                        t = filename.replace('\\', '/');

                    if (r.server != null)
                        t = t + "[" + r.server.getFullName() + "]";
                    else
                        t = t + "[no server]";
                } else if (o instanceof JFrame)
                    t = ((JFrame) o).getTitle();

                JMenuItem item = new JMenuItem((i + 1) + " " + t);
                item.addActionListener(e -> {
                    if (o instanceof StudioPanel) {
                        JFrame f = ((StudioPanel) o).frame;
                        ensureDeiconified(f);
                    } else if (o instanceof JFrame) {
                        ensureDeiconified((JFrame) o);
                    }
                });

                if (o == this)
                    item.setIcon(Util.CHECK_ICON);
                else
                    item.setIcon(Util.BLANK_ICON);

                menu.add(item);
                i++;
            }
        }
        menubar.add(menu);
        menu = new JMenu(I18n.getString("Help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(new JMenuItem(codeKxComAction));
        if (!Util.MAC_OS_X)
            menu.add(new JMenuItem(aboutAction));
        menubar.add(menu);

        return menubar;
    }

    private void ensureDeiconified(JFrame f) {
        int state = f.getExtendedState();
        state = state & ~Frame.ICONIFIED;
        f.setExtendedState(state);
        f.setVisible(true);
    }

    private void selectConnectionString() {
        String connection = txtServer.getText().trim();
        if (connection.isEmpty()) return;
        if (server != null && server.getConnectionString(false).equals(connection)) return;

        try {
            setServer(Config.getInstance().getServerByConnectionString(connection));

            rebuildToolbar();
            toolbar.validate();
            toolbar.repaint();
        } catch (IllegalArgumentException e) {
            refreshConnection();
        }
    }

    private void selectServerName() {
        String selection = Objects.requireNonNull(comboServer.getSelectedItem()).toString();
        if (!Config.getInstance().getServerNames().contains(selection)) return;

        setServer(Config.getInstance().getServer(selection));
        rebuildToolbar();
        toolbar.validate();
        toolbar.repaint();
    }

    private void refreshConnection() {
        if (server == null) {
            txtServer.setText("");
            txtServer.setToolTipText("Select connection details");
        } else {
            txtServer.setText(server.getConnectionString(false));
            txtServer.setToolTipText(server.getConnectionString(true));
        }
    }

    private void toolbarAddServerSelection() {
        Collection<String> names = Config.getInstance().getServerNames();
        String name = server == null ? "" : server.getFullName();
        if (!names.contains(name)) {
            List<String> newNames = new ArrayList<>();
            newNames.add(name);
            newNames.addAll(names);
            names = newNames;
        }
        comboServer = new JComboBox<>(names.toArray(new String[0]));
        comboServer.setToolTipText("Select the server context");
        comboServer.setSelectedItem(name);
        comboServer.addActionListener(e -> selectServerName());
        // Cut the width if it is too wide.
        comboServer.setMinimumSize(new Dimension(0, 0));
        comboServer.setVisible(Config.getInstance().isShowServerComboBox());

        txtServer = new JTextField(32);
        txtServer.addActionListener(e -> selectConnectionString());
        txtServer.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                selectConnectionString();
            }
        });
        refreshConnection();

        toolbar.add(new JLabel(I18n.getString("Server")));
        toolbar.add(comboServer);
        toolbar.add(txtServer);
        toolbar.add(serverListAction);
        toolbar.addSeparator();
    }

    private void rebuildToolbar() {
        if (toolbar != null) {
            toolbar.removeAll();
            toolbarAddServerSelection();
            if (server == null) {
                addServerAction.setEnabled(true);
                editServerAction.setEnabled(false);
                removeServerAction.setEnabled(false);
                stopAction.setEnabled(false);
                executeAction.setEnabled(false);
                executeCurrentLineAction.setEnabled(false);
                refreshAction.setEnabled(false);
            } else {
                executeAction.setEnabled(true);
                executeCurrentLineAction.setEnabled(true);
                editServerAction.setEnabled(true);
                removeServerAction.setEnabled(true);
            }

            toolbar.add(stopAction);
            toolbar.add(executeAction);
            toolbar.add(refreshAction);
            toolbar.addSeparator();

            toolbar.add(openFileAction);
            toolbar.add(saveFileAction);
            toolbar.add(saveAsFileAction);
            toolbar.addSeparator();
//            toolbar.add(importAction);
            toolbar.add(openInExcel);
            toolbar.addSeparator();
            toolbar.add(exportAction);
            toolbar.addSeparator();

            toolbar.add(chartAction);
            toolbar.addSeparator();

            toolbar.add(undoAction);
            toolbar.add(redoAction);
            toolbar.addSeparator();

            toolbar.add(cutAction);
            toolbar.add(copyAction);
            toolbar.add(pasteAction);

            toolbar.addSeparator();
            toolbar.add(findAction);

            toolbar.add(replaceAction);

            toolbar.addSeparator();
            toolbar.add(codeKxComAction);

            for (int j = 0; j < toolbar.getComponentCount(); j++) {
                Component c = toolbar.getComponentAtIndex(j);

                if (c instanceof JButton)
                    ((JButton) c).setRequestFocusEnabled(false);
            }
        }
    }

    private JToolBar createToolbar() {
        toolbar = new JToolBar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        rebuildToolbar();
        return toolbar;
    }

    private static class Impl extends FileView implements
            LocaleSupport.Localizer {
        // FileView implementation

        public String getName(File f) {
            return null;
        }


        public String getDescription(File f) {
            return null;
        }


        public String getTypeDescription(File f) {
            return null;
        }


        public Boolean isTraversable(File f) {
            return null;
        }


        public Icon getIcon(File f) {
            return null;
        }

        private ResourceBundle bundle;

        public Impl(String bundleName) {
            bundle = ResourceBundle.getBundle(bundleName);
        }
        // Localizer

        public String getString(String key) {
            return bundle.getString(key);
        }
    }

    private WindowListChangedEventListener windowListChangedEventListener;

    private int dividerLastPosition; // updated from property change listener

    private void minMaxDivider() {
        if (splitpane.getDividerLocation() >= splitpane.getMaximumDividerLocation()) {
            // Minimize editor pane
            splitpane.getTopComponent().setMinimumSize(new Dimension());
            splitpane.getBottomComponent().setMinimumSize(null);
            splitpane.setDividerLocation(0.);
            splitpane.setResizeWeight(0.);
        } else if (splitpane.getDividerLocation() <= splitpane.getMinimumDividerLocation()) {
            // Restore editor pane
            splitpane.getTopComponent().setMinimumSize(null);
            splitpane.getBottomComponent().setMinimumSize(null);
            splitpane.setResizeWeight(0.);
            // Could probably catch resize edge-cases etc in pce too
            if (dividerLastPosition >= splitpane.getMaximumDividerLocation() || dividerLastPosition <= splitpane.getMinimumDividerLocation())
                dividerLastPosition = splitpane.getMaximumDividerLocation() / 2;
            splitpane.setDividerLocation(dividerLastPosition);
        } else {
            // Maximize editor pane
            splitpane.getBottomComponent().setMinimumSize(new Dimension());
            splitpane.getTopComponent().setMinimumSize(null);
            splitpane.setDividerLocation(splitpane.getOrientation() == VERTICAL_SPLIT ? splitpane.getHeight() - splitpane.getDividerSize() : splitpane.getWidth() - splitpane.getDividerSize());
            splitpane.setResizeWeight(1.);
        }
    }

    private void toggleDividerOrientation() {
        if (splitpane.getOrientation() == JSplitPane.VERTICAL_SPLIT)
            splitpane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        else
            splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        splitpane.setDividerLocation(0.5);
    }

    public StudioPanel(Server server, String filename) {

        registerForMacOSXEvents();

        windowListChangedEventListener = evt -> {
            rebuildMenuBar();
            rebuildToolbar();
        };

        windowListMonitor.addEventListener(windowListChangedEventListener);

        splitpane = new JSplitPane();
        frame = new JFrame();
        windowList.add(this);

        initDocument();
        setServer(server);

        menubar = createMenuBar();
        toolbar = createToolbar();

        tabbedPane = new JTabbedPane();
        splitpane.setBottomComponent(tabbedPane);
        splitpane.setOneTouchExpandable(true);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        try {
            Component divider = ((BasicSplitPaneUI) splitpane.getUI()).getDivider();

            divider.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2)
                        toggleDividerOrientation();
                }
            });
        } catch (ClassCastException ignored) {
        }
        splitpane.setContinuousLayout(true);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setJMenuBar(menubar);

        if (filename != null)
            loadFile(filename);
        else
            myScriptNumber = scriptNumber++;

        refreshFrameTitle();

        frame.getContentPane().add(toolbar, BorderLayout.NORTH);
        frame.getContentPane().add(splitpane, BorderLayout.CENTER);
        // frame.setSize(frame.getContentPane().getPreferredSize());

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.setSize((int) (0.8 * screenSize.width),
                (int) (0.8 * screenSize.height));

        frame.setLocation(((int) Math.max(0, (screenSize.width - frame.getWidth()) / 2.0)),
                (int) (Math.max(0, (screenSize.height - frame.getHeight()) / 2.0)));

        frame.setIconImage(Objects.requireNonNull(Util.LOGO_ICON).getImage());

        //     frame.pack();
        frame.setVisible(true);
        splitpane.setDividerLocation(0.5);

        textArea.requestFocus();
        splitpane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, pce -> {
            String s = splitpane.getDividerLocation() >= splitpane.getMaximumDividerLocation() ? I18n.getString("MinimizeEditorPane") : splitpane.getDividerLocation() <= splitpane.getMinimumDividerLocation() ? I18n.getString("RestoreEditorPane") : I18n.getString("MaximizeEditorPane");
            minMaxDividerAction.putValue(Action.SHORT_DESCRIPTION, s);
            minMaxDividerAction.putValue(Action.NAME, s);
            if (splitpane.getDividerLocation() < splitpane.getMaximumDividerLocation() && splitpane.getDividerLocation() > splitpane.getMinimumDividerLocation())
                dividerLastPosition = splitpane.getDividerLocation();
        });
        dividerLastPosition = splitpane.getDividerLocation();
    }

    private static boolean registeredForMaxOSXEvents = false;

    public void registerForMacOSXEvents() {
        if (registeredForMaxOSXEvents)
            return;

        if (Util.MAC_OS_X)
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, StudioPanel.class.getDeclaredMethod("quit", (Class[]) null));
                OSXAdapter.setAboutHandler(this, StudioPanel.class.getDeclaredMethod("about", (Class[]) null));
                OSXAdapter.setPreferencesHandler(this, StudioPanel.class.getDeclaredMethod("settings", (Class[]) null));
                registeredForMaxOSXEvents = true;
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
    }

    public static void init(String[] args) {
        try {
            String filename = null;

            String[] mruFiles = Config.getInstance().getMRUFiles();
            if (args.length > 0) {
                File f = new File(args[0]);
                if (f.exists())
                    filename = args[0];
            } else if (mruFiles.length > 0) {
                File f = new File(mruFiles[0]);
                if (f.exists())
                    filename = mruFiles[0];
            }

            Locale.setDefault(Locale.US);

            Server s = null;
            String lruServer = Config.getInstance().getLRUServer();
            if (Config.getInstance().getServerNames().contains(lruServer)) {
                s = Config.getInstance().getServer(lruServer);
            }
            new StudioPanel(s, filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshQuery() {
        table = null;
        executeK4Query(lastQuery);
    }

    public void executeQueryCurrentLine() {
        executeQuery(getCurrentLineEditorText(textArea));
    }

    public void executeQuery() {
        executeQuery(getEditorText(textArea));
    }

    private void executeQuery(String text) {
        table = null;

        if (text == null) {
            JOptionPane.showMessageDialog(frame,
                    "\nNo text available to submit to server.\n\n",
                    "Studio for kdb+",
                    JOptionPane.ERROR_MESSAGE,
                    Util.INFORMATION_ICON);

            return;
        }

        refreshAction.setEnabled(false);
        stopAction.setEnabled(true);
        executeAction.setEnabled(false);
        executeCurrentLineAction.setEnabled(false);
        exportAction.setEnabled(false);
        chartAction.setEnabled(false);
        openInExcel.setEnabled(false);

        executeK4Query(text);

        lastQuery = text;
    }

    private String getEditorText(JEditorPane editor) {
        String text = editor.getSelectedText();

        if (text != null) {
            if (!text.isEmpty())
                if (text.trim().isEmpty())
                    return null; // selected text is whitespace
        } else
            text = editor.getText(); // get the full text then

        if (text != null)
            text = text.trim();

        if (text != null && text.trim().isEmpty()) text = null;

        return text;
    }

    private String getCurrentLineEditorText(JEditorPane editor) {
        String newLine = "\n";
        String text = null;

        try {
            int pos = editor.getCaretPosition();
            int max = editor.getDocument().getLength();


            if ((max > pos) && (!editor.getText(pos, 1).equals("\n"))) {
                String toeol = editor.getText(pos, max - pos);
                int eol = toeol.indexOf('\n');

                if (eol > 0)
                    pos = pos + eol;
                else
                    pos = max;
            }

            text = editor.getText(0, pos);

            int lrPos = text.lastIndexOf(newLine);

            if (lrPos >= 0) {
                lrPos += newLine.length(); // found it so skip it
                text = text.substring(lrPos, pos).trim();
            }
        } catch (BadLocationException ignored) {
        }

        if (text != null) {
            text = text.trim();

            if (text.isEmpty())
                text = null;
        }

        return text;
    }

    private void processK4Results(K.KBase r) {
        if (r != null) {
            exportAction.setEnabled(true);
            KTableModel model = KTableModel.getModel(r);
            if (model != null) {
                boolean dictModel = model instanceof DictModel;
                boolean listModel = model instanceof ListModel;
                boolean tableModel = !(dictModel || listModel);
                QGrid grid = new QGrid(model);
                table = grid.getTable();
                openInExcel.setEnabled(true);
                chartAction.setEnabled(tableModel);
                String title = tableModel ? "Table" : (dictModel ? "Dict" : "List");
                TabPanel frame = new TabPanel(title + " [" + grid.getRowCount() + " rows] ",
                        Util.TABLE_ICON,
                        grid);
//                frame.setTitle(I18n.getString("Table")+" [" + grid.getRowCount() + " "+I18n.getString("rows")+"] ");
                tabbedPane.addTab(frame.getTitle(), frame.getIcon(), frame.getComponent());
            } else {
                chartAction.setEnabled(false);
                openInExcel.setEnabled(false);
                LimitedWriter lm = new LimitedWriter(Config.getInstance().getMaxCharsInResult());
                try {
                    if (!(r instanceof K.UnaryPrimitive && 0 == ((K.UnaryPrimitive) r).getPrimitiveAsInt()))
                        r.toString(lm, true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (LimitedWriter.LimitException ignored) {
                }

                JEditorPane textArea = new JEditorPane("text/q", lm.toString());
                textArea.setEditable(false);

                TabPanel frame = new TabPanel("Console View ",
                        Util.CONSOLE_ICON,
                        Objects.requireNonNull(Utilities.getEditorUI(textArea)).getExtComponent());


                frame.setTitle(I18n.getString("ConsoleView"));

                tabbedPane.addTab(frame.getTitle(), frame.getIcon(), frame.getComponent());
            }
        }
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    Server server = null;

    public void executeK4Query(final String text) {
        final Cursor cursor = textArea.getCursor();

        textArea.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (tabbedPane.getTabCount() >= Config.getInstance().getResultTabsCount()) {
            tabbedPane.remove(0);
        }

        worker = new SwingWorker() {
            Server s = null;
            c c = null;
            K.KBase r = null;
            Throwable exception;
            boolean cancelled = false;
            long execTime = 0;

            public void interrupt() {
                super.interrupt();

                cancelled = true;

                if (c != null)
                    c.close();
                cleanup();
            }

            public Object construct() {
                try {
                    this.s = server;
                    c = ConnectionPool.getInstance().leaseConnection(s);
                    ConnectionPool.getInstance().checkConnected(c);
                    c.setFrame(frame);
                    long startTime = System.currentTimeMillis();
                    c.k(new K.KCharacterVector(text));
                    r = c.getResponse();
                    execTime = System.currentTimeMillis() - startTime;
                } catch (Throwable e) {
                    System.err.println("Error occurred during query execution: " + e);
                    e.printStackTrace(System.err);
                    exception = e;
                }

                return null;
            }

            public void finished() {
                if (!cancelled) {
                    if (exception != null)
                        try {
                            throw exception;
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "\nA communications error occurred whilst sending the query.\n\nPlease check that the server is running on " + server.getHost() + ":" + server.getPort() + "\n\nError detail is\n\n" + ex.getMessage() + "\n\n",
                                    "Studio for kdb+",
                                    JOptionPane.ERROR_MESSAGE,
                                    Util.ERROR_ICON);
                        } catch (c.K4Exception ex) {
                            JTextPane pane = new JTextPane();
                            String hint = QErrors.lookup(ex.getMessage());
                            if (hint != null)
                                hint = "\nStudio Hint: Possibly this error refers to " + hint;
                            else
                                hint = "";
                            pane.setText("An error occurred during execution of the query.\nThe server sent the response:\n" + ex.getMessage() + hint);
                            pane.setForeground(Color.RED);

                            JScrollPane scrollpane = new JScrollPane(pane);

                            TabPanel frame = new TabPanel("Error Details ",
                                    Util.ERROR_SMALL_ICON,
                                    scrollpane);
                            frame.setTitle("Error Details ");

                            tabbedPane.addTab(frame.getTitle(), frame.getIcon(), frame.getComponent());
                            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                        } catch (java.lang.OutOfMemoryError ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "\nOut of memory whilst communicating with " + server.getHost() + ":" + server.getPort() + "\n\nThe result set is probably too large.\n\nTry increasing the memory available to studio through the command line option -J -Xmx512m\n\n",
                                    "Studio for kdb+",
                                    JOptionPane.ERROR_MESSAGE,
                                    Util.ERROR_ICON);
                        } catch (Throwable ex) {
                            String message = ex.getMessage();

                            if ((message == null) || (message.isEmpty()))
                                message = "No message with exception. Exception is " + ex;

                            JOptionPane.showMessageDialog(frame,
                                    "\nAn unexpected error occurred whilst communicating with " + server.getHost() + ":" + server.getPort() + "\n\nError detail is\n\n" + message + "\n\n",
                                    "Studio for kdb+",
                                    JOptionPane.ERROR_MESSAGE,
                                    Util.ERROR_ICON);
                        }
                    else
                        try {
                            Utilities.setStatusText(textArea, "Last execution time:" + (execTime > 0 ? "" + execTime : "<1") + " mS");
                            processK4Results(r);
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                            JOptionPane.showMessageDialog(frame,
                                    "\nAn unexpected error occurred whilst communicating with " + server.getHost() + ":" + server.getPort() + "\n\nError detail is\n\n" + e.getMessage() + "\n\n",
                                    "Studio for kdb+",
                                    JOptionPane.ERROR_MESSAGE,
                                    Util.ERROR_ICON);
                        }

                    cleanup();
                }
            }

            private void cleanup() {
                if (c != null)
                    ConnectionPool.getInstance().freeConnection(s, c);
                c = null;

                textArea.setCursor(cursor);

                stopAction.setEnabled(false);
                executeAction.setEnabled(true);
                executeCurrentLineAction.setEnabled(true);
                refreshAction.setEnabled(true);

                System.gc();

                worker = null;
            }
        };

        worker.start();
    }

    private SwingWorker worker;

    public void windowClosing(WindowEvent e) {
        if (quitWindow())
            if (windowList.isEmpty())
                System.exit(0);
    }


    public void windowClosed(WindowEvent e) {
    }


    public void windowOpened(WindowEvent e) {
    }
    // ctrl-alt spacebar to minimize window

    public void windowIconified(WindowEvent e) {
    }


    public void windowDeiconified(WindowEvent e) {
    }


    public void windowActivated(WindowEvent e) {
        this.invalidate();
        SwingUtilities.updateComponentTreeUI(this);
    }


    public void windowDeactivated(WindowEvent e) {
    }

    private class MarkingDocumentListener implements DocumentListener {
        private boolean modified = false;

        private void setModified(boolean b) {
            modified = b;
        }

        private boolean getModified() {
            return modified;
        }

        private Component comp;

        public MarkingDocumentListener(Component comp) {
            this.comp = comp;
        }

        private void markChanged(DocumentEvent evt) {
            setModified(true);
            refreshFrameTitle();
        }


        public void changedUpdate(DocumentEvent e) {
        }


        public void insertUpdate(DocumentEvent evt) {
            markChanged(evt);
        }


        public void removeUpdate(DocumentEvent evt) {
            markChanged(evt);
        }

        /**
         * Document property holding String name of associated file
         */
        private static final String FILE = "file";
        /**
         * Document property holding Boolean if document was created or opened
         */
        private static final String CREATED = "created";
        /**
         * Document property holding Boolean modified information
         */
        private static final String MODIFIED = "modified";
    }
}
