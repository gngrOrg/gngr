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
package org.lobobrowser.request;

import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.function.Consumer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;
import org.lobobrowser.clientlet.ClientletException;
import org.lobobrowser.clientlet.ClientletRequest;
import org.lobobrowser.clientlet.ClientletResponse;
import org.lobobrowser.ua.ProgressType;
import org.lobobrowser.ua.RequestType;
import org.lobobrowser.ua.UserAgentContext;

public abstract class AbstractRequestHandler implements RequestHandler {
  protected final ClientletRequest request;
  private final Component dialogComponent;
  private final UserAgentContext uaContext;
  private boolean cancelled = false;

  public AbstractRequestHandler(final ClientletRequest request, final Component dialogComponent, final UserAgentContext uaContext) {
    this.request = request;
    this.dialogComponent = dialogComponent;
    this.uaContext = uaContext;
  }

  @Override
  public UserAgentContext getContext() {
    return uaContext;
  }

  public void cancel() {
    this.cancelled = true;
  }

  public HostnameVerifier getHostnameVerifier() {
    return new LocalHostnameVerifier();
  }

  public String getLatestRequestMethod() {
    return this.request.getMethod();
  }

  public @NonNull URL getLatestRequestURL() {
    return this.request.getRequestURL();
  }

  public ClientletRequest getRequest() {
    return this.request;
  }

  public abstract boolean handleException(ClientletResponse response, Throwable exception, RequestType requestType)
      throws ClientletException;

  public abstract void handleProgress(ProgressType progressType, @NonNull URL url, String method, int value, int max);

  public boolean isCancelled() {
    return this.cancelled;
  }

  public boolean isNewNavigationEntry() {
    return false;
  }

  public abstract void processResponse(ClientletResponse response, Consumer<Boolean> consumer) throws ClientletException, IOException;

  private class LocalHostnameVerifier implements HostnameVerifier {
    private boolean verified;

    /*
     * (non-Javadoc)
     *
     * @see javax.net.ssl.HostnameVerifier#verify(java.lang.String,
     * javax.net.ssl.SSLSession)
     */
    public boolean verify(final String host, final SSLSession session) {
      if (OkHostnameVerifier.INSTANCE.verify(host, session)) {
        return true;
      } else {
        this.verified = false;
        final VerifiedHostsStore vhs = VerifiedHostsStore.getInstance();
        if (vhs.contains(host)) {
          return true;
        }
        // TODO: call with doPrivileged()
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              boolean verified = false;
              final Component dc = dialogComponent;
              if (dc != null) {
                final int result = JOptionPane.showConfirmDialog(dc, "Host " + host
                    + " does not match SSL certificate or CA not recognized. Proceed anyway?", "Security Warning",
                    JOptionPane.YES_NO_OPTION);
                verified = result == JOptionPane.YES_OPTION;
                if (verified) {
                  vhs.add(host);
                }
              }
              synchronized (LocalHostnameVerifier.this) {
                LocalHostnameVerifier.this.verified = verified;
              }
            }
          });
        } catch (final InterruptedException ie) {
          throw new IllegalStateException(ie);
        } catch (final InvocationTargetException ite) {
          throw new IllegalStateException(ite.getCause());
        }
        synchronized (this) {
          return this.verified;
        }
      }
    }
  }
}