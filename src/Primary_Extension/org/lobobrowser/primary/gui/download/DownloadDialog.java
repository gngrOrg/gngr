/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
package org.lobobrowser.primary.gui.download;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.gui.DefaultWindowFactory;
import org.lobobrowser.primary.gui.FieldType;
import org.lobobrowser.primary.gui.FormField;
import org.lobobrowser.primary.gui.FormPanel;
import org.lobobrowser.primary.settings.ToolsSettings;
import org.lobobrowser.request.AbstractRequestHandler;
import org.lobobrowser.request.ClientletRequestImpl;
import org.lobobrowser.request.RequestEngine;
import org.lobobrowser.request.RequestHandler;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.util.OS;
import org.lobobrowser.util.Timing;

public class DownloadDialog extends JFrame {
  private static final long serialVersionUID = -733135175100739218L;

  private static final Logger logger = Logger.getLogger(DownloadDialog.class.getName());

  private final JProgressBar progressBar = new JProgressBar();
  private final FormPanel bottomFormPanel = new FormPanel();
  private final FormPanel topFormPanel = new FormPanel();
  private final FormField documentField = new FormField(FieldType.TEXT, false);
  private final FormField sizeField = new FormField(FieldType.TEXT, false);
  private final FormField destinationField = new FormField(FieldType.TEXT, false);
  private final FormField timeLeftField = new FormField(FieldType.TEXT, false);
  private final FormField mimeTypeField = new FormField(FieldType.TEXT, false);
  private final FormField transferRateField = new FormField(FieldType.TEXT, false);
  private final FormField transferSizeField = new FormField(FieldType.TEXT, false);
  private final JButton saveButton = new JButton();
  private final JButton closeButton = new JButton();
  private final JButton openFolderButton = new JButton();
  private final JButton openButton = new JButton();

  private final @NonNull URL url;
  private final int knownContentLength;

  public DownloadDialog(final ClientletResponse response, final @NonNull URL url, final int transferSpeed, final UserAgentContext uaContext) {
    this.url = url;
    this.uaContext = uaContext;
    this.setIconImage(DefaultWindowFactory.getInstance().getDefaultImageIcon(uaContext).getImage());

    this.topFormPanel.setMinLabelWidth(100);
    this.bottomFormPanel.setMinLabelWidth(100);

    this.bottomFormPanel.setEnabled(false);

    this.documentField.setCaption("Document:");
    this.timeLeftField.setCaption("Estimated time:");
    this.mimeTypeField.setCaption("MIME type:");
    this.sizeField.setCaption("Size:");
    this.destinationField.setCaption("File:");
    this.transferSizeField.setCaption("Transfer size:");
    this.transferRateField.setCaption("Transfer rate:");
    this.openFolderButton.setVisible(false);
    this.openButton.setVisible(false);

    this.documentField.setValue(url.toExternalForm());
    this.mimeTypeField.setValue(response.getMimeType());
    final int cl = response.getContentLength();
    this.knownContentLength = cl;
    final String sizeText = cl == -1 ? "Not known" : getSizeText(cl);
    this.sizeField.setValue(sizeText);
    final String estTimeText = (transferSpeed <= 0) || (cl == -1) ? "Not known" : Timing.getElapsedText(cl / transferSpeed);
    this.timeLeftField.setValue(estTimeText);

    final Container contentPane = this.getContentPane();
    contentPane.setLayout(new FlowLayout());
    final Box rootPanel = new Box(BoxLayout.Y_AXIS);
    rootPanel.setBorder(new EmptyBorder(4, 8, 4, 8));
    rootPanel.add(this.progressBar);
    rootPanel.add(Box.createVerticalStrut(8));
    rootPanel.add(this.topFormPanel);
    rootPanel.add(Box.createVerticalStrut(8));
    rootPanel.add(this.bottomFormPanel);
    rootPanel.add(Box.createVerticalStrut(8));
    rootPanel.add(this.getButtonsPanel());
    contentPane.add(rootPanel);

    final FormPanel bfp = this.bottomFormPanel;
    bfp.addField(this.destinationField);
    bfp.addField(this.transferRateField);
    bfp.addField(this.transferSizeField);

    final FormPanel tfp = this.topFormPanel;
    tfp.addField(this.documentField);
    tfp.addField(this.mimeTypeField);
    tfp.addField(this.sizeField);
    tfp.addField(this.timeLeftField);

    final Dimension topPanelPs = this.topFormPanel.getPreferredSize();
    this.topFormPanel.setPreferredSize(new Dimension(400, topPanelPs.height));

    final Dimension bottomPanelPs = this.bottomFormPanel.getPreferredSize();
    this.bottomFormPanel.setPreferredSize(new Dimension(400, bottomPanelPs.height));

    this.progressBar.setEnabled(false);

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(final WindowEvent e) {
        final RequestHandler rh = requestHandler;
        if (rh != null) {
          rh.cancel();
          // So that there's no error dialog
          requestHandler = null;
        }
      }
    });
  }

  private Component getButtonsPanel() {
    final JButton saveButton = this.saveButton;
    saveButton.setAction(new SaveAction());
    saveButton.setText("Save As...");
    saveButton.setToolTipText("You must select a file before download begins.");

    final JButton closeButton = this.closeButton;
    closeButton.setAction(new CloseAction());
    closeButton.setText("Cancel");

    final JButton openButton = this.openButton;
    openButton.setAction(new OpenAction());
    openButton.setText("Open");

    final JButton openFolderButton = this.openFolderButton;
    openFolderButton.setAction(new OpenFolderAction());
    openFolderButton.setText("Open Folder");

    final Box box = new Box(BoxLayout.X_AXIS);
    // box.setBorder(new BevelBorder(BevelBorder.RAISED));
    box.add(Box.createGlue());
    box.add(openButton);
    box.add(Box.createHorizontalStrut(4));
    box.add(openFolderButton);
    box.add(Box.createHorizontalStrut(4));
    box.add(saveButton);
    box.add(Box.createHorizontalStrut(4));
    box.add(closeButton);
    return box;
  }

  @SuppressWarnings("unused")
  //Warning surpressed because there is a TODO with this method
  private void selectFile() {
    final String path = this.url.getPath();
    final int lastSlashIdx = path.lastIndexOf('/');
    final String tentativeName = lastSlashIdx == -1 ? path : path.substring(lastSlashIdx + 1);
    final JFileChooser chooser = new JFileChooser();
    final ToolsSettings settings = ToolsSettings.getInstance();
    final File directory = settings.getDownloadDirectory();
    if (directory != null) {
      final File selectedFile = new File(directory, tentativeName);
      chooser.setSelectedFile(selectedFile);
    }
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      final File file = chooser.getSelectedFile();
      if (file.exists()) {
        if (JOptionPane.showConfirmDialog(this, "The file exists. Are you sure you want to overwrite it?", "Confirm",
            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
          return;
        }
      }
      settings.setDownloadDirectory(file.getParentFile());
      settings.save();
      this.startDownload(chooser.getSelectedFile());
    }
  }

  private RequestHandler requestHandler;
  private File destinationFile;
  private long downloadBaseTimestamp;
  private long lastTimestamp;
  private long lastProgressValue;
  private double lastTransferRate = Double.NaN;

  final private UserAgentContext uaContext;

  private void startDownload(final java.io.File file) {
    this.saveButton.setEnabled(false);

    this.timeLeftField.setCaption("Time left:");

    this.destinationField.setValue(file.getName());
    this.destinationField.setToolTip(file.getAbsolutePath());

    this.bottomFormPanel.setEnabled(true);
    this.bottomFormPanel.revalidate();

    final ClientletRequest request = new ClientletRequestImpl(this.url, RequestType.DOWNLOAD);
    final RequestHandler handler = new DownloadRequestHandler(request, this, file, uaContext);

    this.destinationFile = file;
    this.requestHandler = handler;
    this.downloadBaseTimestamp = System.currentTimeMillis();

    final Thread t = new Thread(new DownloadRunnable(handler), "Download:" + this.url.toExternalForm());
    t.setDaemon(true);
    t.start();
  }

  private void doneWithDownload_Safe(final long totalSize) {
    SwingUtilities.invokeLater(() -> doneWithDownload(totalSize));
  }

  private void doneWithDownload(final long totalSize) {
    this.requestHandler = null;

    this.setTitle(this.destinationField.getValue());
    this.timeLeftField.setCaption("Download time:");
    final long elapsed = System.currentTimeMillis() - this.downloadBaseTimestamp;
    this.timeLeftField.setValue(Timing.getElapsedText(elapsed));

    final String sizeText = getSizeText(totalSize);
    this.transferSizeField.setValue(sizeText);
    this.sizeField.setValue(sizeText);

    if (elapsed > 0) {
      final double transferRate = (double) totalSize / elapsed;
      this.transferRateField.setValue(round1(transferRate) + " Kb/sec");
    } else {
      this.transferRateField.setValue("N/A");
    }

    this.progressBar.setIndeterminate(false);
    this.progressBar.setStringPainted(true);
    this.progressBar.setValue(100);
    this.progressBar.setMaximum(100);
    this.progressBar.setString("Done.");

    this.closeButton.setText("Close");

    if (OS.supportsLaunchPath()) {
      this.saveButton.setVisible(false);
      this.openFolderButton.setVisible(true);
      this.openButton.setVisible(true);
      this.openButton.revalidate();
    }
  }

  private void errorInDownload_Safe() {
    SwingUtilities.invokeLater(() -> errorInDownload());
  }

  private void errorInDownload() {
    if (this.requestHandler != null) {
      // If requestHandler is null, it means the download was explicitly
      // cancelled or the window closed.
      JOptionPane.showMessageDialog(this, "An error occurred while trying to download the file.");
      this.dispose();
    }
  }

  private static double round1(final double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private static String getSizeText(final long numBytes) {
    if (numBytes < 1024) {
      return numBytes + " bytes";
    } else {
      final double numK = numBytes / 1024.0;
      if (numK < 1024) {
        return round1(numK) + " Kb";
      } else {
        final double numM = numK / 1024.0;
        if (numM < 1024) {
          return round1(numM) + " Mb";
        } else {
          final double numG = numM / 1024.0;
          return round1(numG) + " Gb";
        }
      }
    }
  }

  private void updateProgress_Safe(final ProgressType progressType, final int value, final int max) {
    SwingUtilities.invokeLater(() -> updateProgress(progressType, value, max));
  }

  private void updateProgress(final ProgressType progressType, final int value, final int max) {
    final String sizeText = getSizeText(value);
    this.transferSizeField.setValue(sizeText);

    final long newTimestamp = System.currentTimeMillis();
    final double lastTransferRate = this.lastTransferRate;
    final long lastProgressValue = this.lastProgressValue;
    final long lastTimestamp = this.lastTimestamp;
    final long elapsed = newTimestamp - lastTimestamp;
    double newTransferRate = Double.NaN;
    if (elapsed > 0) {
      newTransferRate = (value - lastProgressValue) / elapsed;
      if (!Double.isNaN(lastTransferRate)) {
        // Weighed average
        newTransferRate = (newTransferRate + (lastTransferRate * 5.0)) / 6.0;
      }
    }
    if (!Double.isNaN(newTransferRate)) {
      this.transferRateField.setValue(round1(newTransferRate) + " Kb/sec");
      final int cl = this.knownContentLength;
      if ((cl > 0) && (newTransferRate > 0)) {
        this.timeLeftField.setValue(Timing.getElapsedText((long) ((cl - value) / newTransferRate)));
      }
    }
    this.lastTimestamp = newTimestamp;
    this.lastProgressValue = value;
    this.lastTransferRate = newTransferRate;

    final JProgressBar pb = this.progressBar;
    if (progressType == ProgressType.CONNECTING) {
      pb.setIndeterminate(true);
      pb.setStringPainted(true);
      pb.setString("Connecting...");
      this.setTitle(this.destinationField.getValue() + ": Connecting...");
    } else if (max <= 0) {
      pb.setIndeterminate(true);
      pb.setStringPainted(false);
      this.setTitle(sizeText + " " + this.destinationField.getValue());
    } else {
      final int percent = (value * 100) / max;
      pb.setIndeterminate(false);
      pb.setStringPainted(true);
      pb.setMaximum(max);
      pb.setValue(value);
      final String percentText = percent + "%";
      pb.setString(percentText);
      this.setTitle(percentText + " " + this.destinationField.getValue());
    }
  }

  private class SaveAction extends AbstractAction {
    private static final long serialVersionUID = -4635141657953704709L;

    public void actionPerformed(final ActionEvent e) {
      final String msg = "Downloads are disabled for security reasons.\nWe are working on a novel way to sandbox the browser and will enable downloads after the design is completed.";
      JOptionPane.showMessageDialog(DownloadDialog.this, msg);

      // TODO
      // selectFile();
    }
  }

  private class OpenFolderAction extends AbstractAction {
    private static final long serialVersionUID = -6860795246298542670L;

    public void actionPerformed(final ActionEvent e) {
      final File file = destinationFile;
      if (file != null) {
        try {
          OS.launchPath(file.getParentFile().getAbsolutePath());
        } catch (final Exception thrown) {
          logger.log(Level.WARNING, "Unable to open folder of file: " + file + ".", thrown);
          JOptionPane.showMessageDialog(DownloadDialog.this, "An error occurred trying to open the folder.");
        }
      }
    }
  }

  private class OpenAction extends AbstractAction {
    private static final long serialVersionUID = 8435296814556900437L;

    public void actionPerformed(final ActionEvent e) {
      final File file = destinationFile;
      if (file != null) {
        try {
          OS.launchPath(file.getAbsolutePath());
          DownloadDialog.this.dispose();
        } catch (final Exception thrown) {
          logger.log(Level.WARNING, "Unable to open file: " + file + ".", thrown);
          JOptionPane.showMessageDialog(DownloadDialog.this, "An error occurred trying to open the file.");
        }
      }
    }
  }

  private class CloseAction extends AbstractAction {
    private static final long serialVersionUID = 5036020829977878826L;

    public void actionPerformed(final ActionEvent e) {
      // windowClosedEvent takes care of cancelling download.
      DownloadDialog.this.dispose();
    }
  }

  private class DownloadRunnable implements Runnable {
    private final RequestHandler handler;

    public DownloadRunnable(final RequestHandler handler) {
      this.handler = handler;
    }

    public void run() {
      try {
        RequestEngine.getInstance().inlineRequest(this.handler);
      } catch (final Exception err) {
        logger.log(Level.SEVERE, "Unexpected error on download of [" + url.toExternalForm() + "].", err);
      }
    }
  }

  private class DownloadRequestHandler extends AbstractRequestHandler {
    private final File file;
    private boolean downloadDone = false;
    private long lastProgressUpdate = 0;

    public DownloadRequestHandler(final ClientletRequest request, final Component dialogComponent, final File file,
        final UserAgentContext uaContext) {
      super(request, dialogComponent, uaContext);
      this.file = file;
    }

    @Override
    public boolean handleException(final ClientletResponse response, final Throwable exception, final RequestType requestType)
        throws ClientletException {
      logger.log(Level.WARNING, "An error occurred trying to download " + response.getResponseURL() + " to " + this.file + ".", exception);
      errorInDownload_Safe();
      return true;
    }

    @Override
    public void handleProgress(final ProgressType progressType, final @NonNull URL url, final String method, final int value, final int max) {
      if (!this.downloadDone) {
        final long timestamp = System.currentTimeMillis();
        if ((timestamp - this.lastProgressUpdate) > 1000) {
          updateProgress_Safe(progressType, value, max);
          this.lastProgressUpdate = timestamp;
        }
      }
    }

    @Override
    public void processResponse(final ClientletResponse response, Consumer<Boolean> consumer) throws ClientletException, IOException {
      try (
        final OutputStream out = new FileOutputStream(this.file);
        final InputStream in = response.getInputStream()) {
        int totalRead = 0;
        final byte[] buffer = new byte[8192];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
          if (this.isCancelled()) {
            throw new IOException("cancelled");
          }
          totalRead += numRead;
          out.write(buffer, 0, numRead);
        }
        this.downloadDone = true;
        doneWithDownload_Safe(totalRead);
      }
      consumer.accept(true);
    }
  }
}
