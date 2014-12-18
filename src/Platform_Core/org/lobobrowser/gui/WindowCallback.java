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
package org.lobobrowser.gui;

import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.clientlet.ComponentContent;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.NavigatorProgressEvent;
import org.lobobrowser.ua.RequestType;

/**
 * A interface that receives calls during requests.
 */
public interface WindowCallback {

  /**
   * Called when a document has been accessed, but has not yet rendered.
   * Processing could be cancelled.
   */
  public void handleDocumentAccess(NavigatorFrame frame, ClientletResponse response);

  /**
   * Called when the document has either rendered or is beginning to be
   * incrementally rendered.
   */
  public void handleDocumentRendering(NavigatorFrame frame, ClientletResponse response, ComponentContent content);

  /**
   * Updates request progress.
   */
  public void updateProgress(NavigatorProgressEvent event);

  /**
   * Invoked when there's a request error.
   *
   * @param requestType
   */
  public void handleError(NavigatorFrame frame, ClientletResponse response, Throwable exception, RequestType requestType);

  /**
   * Called to set a status message.
   */
  public void setStatus(NavigatorFrame frame, String status);

  /**
   * Called to set a default status message.
   */
  public void setDefaultStatus(NavigatorFrame frame, String defaultStatus);

  /**
   * Gets the current window status message.
   */
  public String getStatus();

  /**
   * Gets the current default status message or <code>null</code> if there's no
   * default.
   */
  public String getDefaultStatus();
}
