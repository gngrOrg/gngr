package org.lobobrowser.security;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import org.lobobrowser.security.PermissionSystem.Permission;
import org.lobobrowser.security.PermissionSystem.PermissionBoard.PermissionRow.PermissionCell;
import org.lobobrowser.security.PermissionSystem.PermissionResult;

public class PermissionCellButton extends JButton implements MouseMotionListener {
  @FunctionalInterface
  public static interface ChangeListener {
    public void onChange();
  }

  private final PermissionCell cell;
  private PermissionResult permissionResult;
  private final ChangeListener cellChangeListener;
  private final boolean canBeUndecidable;

  private static final int BORDER_PADDING = 2;

  private static final Color GREY_TRANSPARENT = new Color(.3f, .3f, .3f, .5f);
  private static final Color WHITE = new Color(1f, 1f, 1f, 1f);
  private static final Color RED_TRANSPARENT = new Color(1f, .2f, .2f, .51f);
  private static final Color GREEN_TRANSPARENT = new Color(.2f, 1f, .2f, .51f);
  private static final Color RED_OPAQUE = new Color(1f, .4f, .4f, 1f);
  private static final Color GREEN_OPAQUE = new Color(.2f, 1f, .2f, 1f);
  private static final Color LIGHT_RED_OPAQUE = new Color(1f, .8f, .8f, 1f);
  private static final Color LIGHT_GREEN_OPAQUE = new Color(.8f, 1f, .8f, 1f);

  private static final long serialVersionUID = 1L;

  // private final CompoundBorder redCompoundBorder, greenCompoundBorder;
  // private final Border defaultBorder;

  public static Insets grow(final Insets in, final int amount) {
    final Insets out = new Insets(in.top + amount, in.left + amount, in.bottom + amount, in.right + amount);
    return out;
  }

  public PermissionCellButton(final PermissionCell cell, final String text, final boolean alignRight, final ChangeListener changeListener) {
    super(text);
    this.cell = cell;
    this.canBeUndecidable = cell.canBeUndecidable;
    this.cellChangeListener = changeListener;

    // final Border defaultBorder = getBorder();
    final Insets marginInsets = getMargin();
    final Insets borderInsets = grow(marginInsets, BORDER_PADDING);
    final Border emptyBorder =
        BorderFactory
            .createEmptyBorder(borderInsets.top, borderInsets.left, borderInsets.bottom, borderInsets.right);
    final Border defaultColorBorder = BorderFactory.createDashedBorder(GREY_TRANSPARENT);
    final CompoundBorder plainBorder = BorderFactory.createCompoundBorder(defaultColorBorder, emptyBorder);
    setBorder(plainBorder);

    permissionResult = cell.getEffectivePermission();
    assert (permissionResult.permission.isDecided());

    setHorizontalAlignment(alignRight ? SwingConstants.RIGHT : SwingConstants.CENTER);
    setUI(new BasicButtonUI());
    addMouseMotionListener(this);
    setFocusable(false);
    setForeground(Color.BLACK);
    updateBackground();
  }

  private void updateBackground() {
    final boolean isDefault = permissionResult.isDefault;
    switch (permissionResult.permission) {
    case Allow:
      setBackground(isDefault ? LIGHT_GREEN_OPAQUE : GREEN_OPAQUE);
      break;
    case Deny:
      setBackground(isDefault ? LIGHT_RED_OPAQUE : RED_OPAQUE);
      break;
    case Undecided:
      throw new RuntimeException("Unexpected undecided state");
    }
    /*
     * if (state.isKnownState()) { switch (defaultState) { case Disabled:
     * setBorder(redCompoundBorder); break; case Enabled:
     * setBorder(greenCompoundBorder); break; case Default: throw new
     * RuntimeException("Unexpected default state"); } } else {
     * setBorder(defaultBorder); }
     */
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;
    super.paintComponent(g);

    if (mouseIn) {
      if (permissionResult.isDefault || !canBeUndecidable) {
        g.setColor(mouseOnLeft ? RED_TRANSPARENT : GREEN_TRANSPARENT);
        final int leftEdge = mouseOnLeft ? 0 : getWidth() / 2;
        final int rightEdge = mouseOnLeft ? getWidth() / 2 : getWidth();
        g.fillRect(leftEdge, 0, rightEdge, getHeight());
      } else {
        g.setColor(WHITE);
        final int leftX = 6;
        final int topY = 6;
        final int rightX = 12;
        final int bottomY = 12;
        g2.setStroke(new BasicStroke(2));
        g.drawLine(leftX, topY, rightX, bottomY);
        g.drawLine(leftX, bottomY, rightX, topY);
      }
    }
  }

  private boolean mouseIn = false;
  private boolean mouseOnLeft = false;

  @Override
  public void mouseDragged(final MouseEvent e) {
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    // System.out.println(String.format("%5d, %5d   : %s", e.getX(),
    // e.getY(), e));
    final boolean prevMouseOnLeft = mouseOnLeft;
    mouseOnLeft = e.getX() < (getWidth() / 2);
    if (mouseOnLeft != prevMouseOnLeft) {
      repaint();
    }
  }

  @Override
  protected void processMouseEvent(final MouseEvent e) {
    super.processMouseEvent(e);
    if (e.getID() == MouseEvent.MOUSE_ENTERED) {
      mouseIn = true;
      repaint();
    } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
      mouseIn = false;
      repaint();
    } else if (e.getID() == MouseEvent.MOUSE_CLICKED) {
      assert (mouseIn);
      if (!permissionResult.isDefault && canBeUndecidable) {
        cell.setPermission(Permission.Undecided);
      } else {
        if (mouseOnLeft) {
          cell.setPermission(Permission.Deny);
        } else {
          cell.setPermission(Permission.Allow);
        }
      }
      cellChangeListener.onChange();
    } else {
      // System.out.println("Mouse event:" + e);
    }
  }

  public void update() {
    permissionResult = cell.getEffectivePermission();
    updateBackground();
  }
}
