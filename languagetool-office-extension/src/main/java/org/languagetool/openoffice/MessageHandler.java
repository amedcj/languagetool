/* LanguageTool, a natural language style checker
 * Copyright (C) 2011 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.openoffice;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Date;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.languagetool.JLanguageTool;
import org.languagetool.openoffice.OfficeTools.OfficeProductInfo;
import org.languagetool.tools.Tools;

import com.sun.star.uno.XComponentContext;

/**
 * Writes Messages to screen or log-file
 * @since 4.3
 * @author Fred Kruse, Marcin Miłkowski
 */
class MessageHandler {
  
  private static final String logLineBreak = System.lineSeparator();  //  LineBreak in Log-File (MS-Windows compatible)
  
  private static boolean isOpen = false;
  
  private static boolean testMode;
  
  MessageHandler(XComponentContext xContext) {
    initLogFile(xContext);
  }

  /**
   * Initialize log-file
   */
  private static void initLogFile(XComponentContext xContext) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(OfficeTools.getLogFilePath()))) {
      Date date = new Date();
      OfficeProductInfo officeInfo = OfficeTools.getOfficeProductInfo(xContext);
      bw.write("LT office integration log from " + date + logLineBreak + logLineBreak);
      bw.write("LanguageTool " + JLanguageTool.VERSION + " (" + JLanguageTool.BUILD_DATE + ", " 
          + JLanguageTool.GIT_SHORT_ID + ")" + logLineBreak);
      bw.write("OS: " + System.getProperty("os.name") + " " 
          + System.getProperty("os.version") + " on " + System.getProperty("os.arch") + logLineBreak);
      if (officeInfo != null) { 
        bw.write(officeInfo.ooName + officeInfo.ooVersion + officeInfo.ooExtension
            + "(" + officeInfo.ooVendor +"), " + officeInfo.ooLocale + logLineBreak);
      }
      bw.write(OfficeTools.getJavaInformation() + logLineBreak + logLineBreak);
    } catch (Throwable t) {
      showError(t);
    }
  }
  
  /**
   * Initialize MessageHandler
   */
  static void init(XComponentContext xContext) {
    initLogFile(xContext);
  }

  /**
   * Show an error in a dialog
   */
  static void showError(Throwable e) {
    printException(e);
    if (testMode) {
      throw new RuntimeException(e);
    }
    String msg = "An error has occurred in LanguageTool "
        + JLanguageTool.VERSION + " (" + JLanguageTool.BUILD_DATE + "):\n" + e + "\nStacktrace:\n";
    msg += Tools.getFullStackTrace(e);
    String metaInfo = "OS: " + System.getProperty("os.name") + " on "
        + System.getProperty("os.arch") + ", Java version "
        + System.getProperty("java.version") + " from "
        + System.getProperty("java.vm.vendor");
    msg += metaInfo;
    DialogThread dt = new DialogThread(msg, true);
    e.printStackTrace();
    dt.start();
  }

  /**
   * Write to log-file
   */
  static void printToLogFile(String str) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(OfficeTools.getLogFilePath(), true))) {
      bw.write(str + logLineBreak);
    } catch (Throwable t) {
      showError(t);
    }
  }

  /** 
   * Prints Exception to log-file  
   */
  static void printException(Throwable t) {
   printToLogFile(Tools.getFullStackTrace(t));
  }

  /**
   * Will throw exception instead of showing errors as dialogs - use only for test cases.
   */
  static void setTestMode(boolean mode) {
    testMode = mode;
  }

  /**
   * Shows a message in a dialog box
   * @param txt message to be shown
   */
  static void showMessage(String txt) {
    showMessage(txt, true);
  }

  static void showMessage(String txt, boolean toLogFile) {
    if (toLogFile) {
      printToLogFile(txt);
    }
    DialogThread dt = new DialogThread(txt, false);
    dt.run();
  }

  /**
   * run an information message in a separate thread
   * closing if lost focus
   */
  static void showClosingInformationDialog(String text) {
    ClosingInformationThread informationDialog = new ClosingInformationThread(text);
    informationDialog.start();
  }
  
  /**
   * class to run a dialog in a separate thread
   */
  private static class DialogThread extends Thread {
    private final String text;
    private boolean isException;

    DialogThread(String text, boolean isException) {
      this.text = text;
      this.isException = isException;
    }

    @Override
    public void run() {
      if (isException) {
        if (!isOpen) {
          isOpen = true;
          JOptionPane.showMessageDialog(null, text);
          isOpen = false;
        }
      } else {
        JOptionPane.showMessageDialog(null, text);
      }
    }
  }
  
  /**
   * class to run a dialog in a separate thread
   * closing if lost focus
   */
  private static class ClosingInformationThread extends Thread {
    private final String text;
    JDialog dialog;

    ClosingInformationThread(String text) {
      this.text = text;
    }

    @Override
    public void run() {
      JOptionPane pane = new JOptionPane(text, JOptionPane.INFORMATION_MESSAGE);
      dialog = pane.createDialog(null, UIManager.getString("OptionPane.messageDialogTitle", null));
      dialog.setModal(false);
      dialog.setAlwaysOnTop(true);
      dialog.addWindowFocusListener(new WindowFocusListener() {
        @Override
        public void windowGainedFocus(WindowEvent e) {
        }
        @Override
        public void windowLostFocus(WindowEvent e) {
          dialog.setVisible(false);
        }
      });
      dialog.setVisible(true);
    }
  }
  
}
