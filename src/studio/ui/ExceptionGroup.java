package studio.ui;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.swing.*;
import java.awt.*;

public class ExceptionGroup extends ThreadGroup {
    public ExceptionGroup() {
        super("ExceptionGroup");
    }

    public void uncaughtException(Thread t, Throwable e) {
        CharArrayWriter caw = new CharArrayWriter();

        e.printStackTrace(new PrintWriter(caw));

        JOptionPane.showMessageDialog(findActiveFrame(),
                "An uncaught exception occurred\n\nDetails - \n\n" + caw,
                "Studio for kdb+",
                JOptionPane.ERROR_MESSAGE);
    }

    private Frame findActiveFrame() {
        Frame[] frames = JFrame.getFrames();
        for (Frame frame : frames) {
            if (frame.isVisible())
                return frame;
        }
        return null;
    }
}
