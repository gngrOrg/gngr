package org.lobobrowser.primary.ext;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;

import org.lobobrowser.ua.RequestStats;

/**
 * Toolbar button displaying live blocked/allowed request counts and a
 * proportional ratio bar along the bottom edge.
 */
public class RequestManagerButton extends JButton {
  private static final long serialVersionUID = 1L;

  private static final int BUTTON_HEIGHT = 26;
  private static final int MIN_BUTTON_WIDTH = 76;
  private static final int SIDE_HORIZONTAL_PADDING = 10;
  private static final int BAR_HEIGHT = 4;
  private static final int HORIZONTAL_DIVIDER_HEIGHT = 1;
  private static final int MIN_NONZERO_BAR_PX = 2;

  private static final Color ENABLED_BLOCKED_BG = new Color(255, 204, 204);
  private static final Color ENABLED_ALLOWED_BG = new Color(204, 255, 204);
  private static final Color DISABLED_LEFT_BG = new Color(230, 230, 230);
  private static final Color DISABLED_RIGHT_BG = new Color(222, 222, 222);

  private static final Color ENABLED_TEXT_COLOR = new Color(30, 30, 30);
  private static final Color DISABLED_TEXT_COLOR = new Color(135, 135, 135);

  private static final Color VERTICAL_DIVIDER_COLOR = new Color(0, 0, 0, 42);
  private static final Color HORIZONTAL_DIVIDER_COLOR = new Color(0, 0, 0, 35);

  private static final Color BAR_BLOCKED_COLOR = new Color(217, 63, 63);
  private static final Color BAR_ALLOWED_COLOR = new Color(46, 160, 67);
  private static final Color BAR_EMPTY_COLOR = new Color(208, 208, 208);
  private static final Color BAR_DISABLED_COLOR = new Color(185, 185, 185);

  private static final Color BORDER_DEFAULT_COLOR = new Color(135, 140, 148);
  private static final Color BORDER_ROLLOVER_COLOR = new Color(95, 102, 112);
  private static final Color BORDER_PRESSED_COLOR = new Color(80, 86, 94);
  private static final Color BORDER_DISABLED_COLOR = new Color(175, 175, 175);

  private static final Color ROLLOVER_OVERLAY = new Color(0, 0, 0, 18);
  private static final Color PRESSED_OVERLAY = new Color(0, 0, 0, 38);
  private static final Color PRESSED_SHADOW = new Color(0, 0, 0, 40);
  private static final Color FOCUS_RING_COLOR = new Color(65, 115, 200, 140);

  private final Font labelFont;
  private final Dimension fixedSize;

  private final AtomicReference<RequestStats> latestStats = new AtomicReference<>(RequestStats.EMPTY);
  private final AtomicBoolean updateScheduled = new AtomicBoolean(false);
  private RequestStats stats = RequestStats.EMPTY;

  public RequestManagerButton(final Action action) {
    super();
    setUI(new BasicButtonUI());
    setOpaque(false);
    setContentAreaFilled(false);
    setBorderPainted(false);
    setFocusPainted(false);
    setRolloverEnabled(true);

    final Font baseFont = getFont() != null ? getFont() : UIManager.getFont("Button.font");
    this.labelFont = (baseFont != null ? baseFont : new Font(Font.SANS_SERIF, Font.PLAIN, 11)).deriveFont(Font.BOLD, 11f);
    setFont(this.labelFont);

    final FontMetrics fm = getFontMetrics(this.labelFont);
    final int sideWidth = fm.stringWidth("999") + SIDE_HORIZONTAL_PADDING;
    final int width = Math.max(MIN_BUTTON_WIDTH, (sideWidth * 2) + 1 + 2);
    this.fixedSize = new Dimension(width, BUTTON_HEIGHT);

    if (action != null) {
      setAction(action);
    }
    getAccessibleContext().setAccessibleName("Request Manager");
    updateToolTip();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(fixedSize);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(fixedSize);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(fixedSize);
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    updateToolTip();
    repaint();
  }

  /**
   * Updates the displayed counts, safely coalescing calls from background
   * threads onto the Swing event dispatch thread.
   */
  public void setRequestStats(final RequestStats newStats) {
    final RequestStats nonNullStats = newStats == null ? RequestStats.EMPTY : newStats;
    latestStats.set(nonNullStats);
    if (SwingUtilities.isEventDispatchThread()) {
      updateScheduled.set(false);
      applyStats(nonNullStats);
    } else if (updateScheduled.compareAndSet(false, true)) {
      SwingUtilities.invokeLater(() -> {
        updateScheduled.set(false);
        applyStats(latestStats.get());
      });
    }
  }

  private void applyStats(final RequestStats newStats) {
    if (!this.stats.equals(newStats)) {
      this.stats = newStats;
      updateToolTip();
      repaint();
    }
  }

  private void updateToolTip() {
    if (!isEnabled()) {
      setToolTipText("Manage requests");
    } else {
      setToolTipText("Request Manager \u2014 " + stats.blocked + " blocked, " + stats.allowed + " allowed");
    }
  }

  public static String formatCount(final int count) {
    if (count < 0) {
      return "0";
    }
    if (count < 1000) {
      return Integer.toString(count);
    }
    if (count < 10_000) {
      final int tenths = count / 100;
      final int whole = tenths / 10;
      final int frac = tenths % 10;
      return frac == 0 ? whole + "k" : whole + "." + frac + "k";
    }
    if (count < 1_000_000) {
      return (count / 1000) + "k";
    }
    return (count / 1_000_000) + "M";
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final int w = getWidth();
    final int h = getHeight();
    if ((w <= 4) || (h <= 4)) {
      return;
    }

    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      final RoundRectangle2D.Float outerShape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 6f, 6f);
      final RoundRectangle2D.Float innerShape = new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 5f, 5f);

      final int innerW = w - 2;
      final int innerH = h - 2;
      final int topHeight = Math.max(1, innerH - BAR_HEIGHT - HORIZONTAL_DIVIDER_HEIGHT);
      final int dividerY = 1 + topHeight;
      final int barY = dividerY + HORIZONTAL_DIVIDER_HEIGHT;

      final int leftWidth = (innerW - 1) / 2;
      final int dividerX = 1 + leftWidth;
      final int rightX = dividerX + 1;
      final int rightWidth = innerW - leftWidth - 1;

      final boolean enabled = isEnabled();
      final ButtonModel model = getModel();
      final boolean pressed = enabled && model.isPressed() && model.isArmed();
      final boolean rollover = enabled && model.isRollover();

      final Shape oldClip = g2.getClip();
      g2.clip(innerShape);

      // Split top halves (Blocked on left, Allowed on right)
      g2.setColor(enabled ? ENABLED_BLOCKED_BG : DISABLED_LEFT_BG);
      g2.fillRect(1, 1, leftWidth, topHeight);

      g2.setColor(enabled ? ENABLED_ALLOWED_BG : DISABLED_RIGHT_BG);
      g2.fillRect(rightX, 1, rightWidth, topHeight);

      // Center vertical divider and horizontal divider above bar
      g2.setColor(VERTICAL_DIVIDER_COLOR);
      g2.fillRect(dividerX, 1, 1, topHeight);

      g2.setColor(HORIZONTAL_DIVIDER_COLOR);
      g2.fillRect(1, dividerY, innerW, HORIZONTAL_DIVIDER_HEIGHT);

      // Bottom proportional ratio bar
      paintRatioBar(g2, 1, barY, innerW, BAR_HEIGHT, enabled);

      // Hover / pressed state feedback
      if (pressed) {
        g2.setColor(PRESSED_OVERLAY);
        g2.fill(innerShape);
        g2.setColor(PRESSED_SHADOW);
        g2.fillRect(1, 1, innerW, 1);
      } else if (rollover) {
        g2.setColor(ROLLOVER_OVERLAY);
        g2.fill(innerShape);
      }

      // Counts
      g2.setFont(labelFont);
      final FontMetrics fm = g2.getFontMetrics();
      final int textY = 1 + ((topHeight - fm.getHeight()) / 2) + fm.getAscent();
      final String leftText = enabled ? formatCount(stats.blocked) : "\u2013";
      final String rightText = enabled ? formatCount(stats.allowed) : "\u2013";

      g2.setColor(enabled ? ENABLED_TEXT_COLOR : DISABLED_TEXT_COLOR);
      g2.drawString(leftText, 1 + ((leftWidth - fm.stringWidth(leftText)) / 2), textY);
      g2.drawString(rightText, rightX + ((rightWidth - fm.stringWidth(rightText)) / 2), textY);

      g2.setClip(oldClip);

      // Outer border
      if (!enabled) {
        g2.setColor(BORDER_DISABLED_COLOR);
      } else if (pressed) {
        g2.setColor(BORDER_PRESSED_COLOR);
      } else if (rollover) {
        g2.setColor(BORDER_ROLLOVER_COLOR);
      } else {
        g2.setColor(BORDER_DEFAULT_COLOR);
      }
      g2.draw(outerShape);

      if (isFocusOwner()) {
        g2.setColor(FOCUS_RING_COLOR);
        g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, 4f, 4f));
      }
    } finally {
      g2.dispose();
    }
  }

  private void paintRatioBar(final Graphics2D g2, final int x, final int y, final int width, final int height, final boolean enabled) {
    if (!enabled) {
      g2.setColor(BAR_DISABLED_COLOR);
      g2.fillRect(x, y, width, height);
      return;
    }

    final int total = stats.getTotal();
    if (total <= 0) {
      g2.setColor(BAR_EMPTY_COLOR);
      g2.fillRect(x, y, width, height);
      return;
    }

    int redWidth = (int) Math.round(((double) stats.blocked * width) / total);
    if ((stats.blocked > 0) && (redWidth < MIN_NONZERO_BAR_PX)) {
      redWidth = Math.min(width, MIN_NONZERO_BAR_PX);
    }
    if ((stats.allowed > 0) && ((width - redWidth) < MIN_NONZERO_BAR_PX)) {
      redWidth = Math.max(0, width - MIN_NONZERO_BAR_PX);
    }

    if (redWidth > 0) {
      g2.setColor(BAR_BLOCKED_COLOR);
      g2.fillRect(x, y, redWidth, height);
    }
    final int greenWidth = width - redWidth;
    if (greenWidth > 0) {
      g2.setColor(BAR_ALLOWED_COLOR);
      g2.fillRect(x + redWidth, y, greenWidth, height);
    }
  }
}
