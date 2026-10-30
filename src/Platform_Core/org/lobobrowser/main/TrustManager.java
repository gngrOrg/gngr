package org.lobobrowser.main;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public final class TrustManager {
  public static final class SSLInfo {
    public final X509TrustManager trustManager;
    public final SSLSocketFactory socketFactory;

    public SSLInfo(final X509TrustManager trustManager, final SSLSocketFactory socketFactory) {
      this.trustManager = trustManager;
      this.socketFactory = socketFactory;
    }
  }

  private static final class CompositeX509TrustManager implements X509TrustManager {
    private final X509TrustManager primaryTM;
    private final X509TrustManager fallbackTM;

    CompositeX509TrustManager(final X509TrustManager primaryTM, final X509TrustManager fallbackTM) {
      this.primaryTM = primaryTM;
      this.fallbackTM = fallbackTM;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      try {
        this.primaryTM.checkClientTrusted(chain, authType);
      } catch (final CertificateException primaryEx) {
        try {
          this.fallbackTM.checkClientTrusted(chain, authType);
        } catch (final CertificateException fallbackEx) {
          primaryEx.addSuppressed(fallbackEx);
          throw primaryEx;
        }
      }
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      try {
        this.primaryTM.checkServerTrusted(chain, authType);
      } catch (final CertificateException primaryEx) {
        try {
          this.fallbackTM.checkServerTrusted(chain, authType);
        } catch (final CertificateException fallbackEx) {
          primaryEx.addSuppressed(fallbackEx);
          throw primaryEx;
        }
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      final Set<X509Certificate> issuers = new LinkedHashSet<>(Arrays.asList(this.primaryTM.getAcceptedIssuers()));
      issuers.addAll(Arrays.asList(this.fallbackTM.getAcceptedIssuers()));
      return issuers.toArray(new X509Certificate[0]);
    }
  }

  private static KeyStore loadExtraCertsKeyStore(final InputStream extraCertsStream)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    if (extraCertsStream == null) {
      return null;
    }
    final KeyStore rawStore = KeyStore.getInstance(KeyStore.getDefaultType());
    rawStore.load(extraCertsStream, null);

    KeyStore extraKeyStore = null;
    final Enumeration<String> aliases = rawStore.aliases();
    while (aliases.hasMoreElements()) {
      final String alias = aliases.nextElement();
      if (rawStore.isCertificateEntry(alias)) {
        if (extraKeyStore == null) {
          extraKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
          extraKeyStore.load(null, null);
        }
        extraKeyStore.setCertificateEntry(alias, rawStore.getCertificate(alias));
      }
    }
    return extraKeyStore;
  }

  public static SSLInfo makeSSLSocketFactory(final InputStream extraCertsStream) {
    try {
      final X509TrustManager systemTM = getX509TM(null);
      final X509TrustManager effectiveTM;

      final KeyStore extraKeyStore = loadExtraCertsKeyStore(extraCertsStream);
      if (extraKeyStore != null) {
        final X509TrustManager extraTM = getX509TM(extraKeyStore);
        effectiveTM = new CompositeX509TrustManager(systemTM, extraTM);
      } else {
        effectiveTM = systemTM;
      }

      final SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, new javax.net.ssl.TrustManager[] {effectiveTM}, null);
      return new SSLInfo(effectiveTM, sc.getSocketFactory());
    } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | IOException
        | CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  public static X509TrustManager getX509TM(final KeyStore keyStore) {
    try {
      final String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
      trustManagerFactory.init(keyStore);
      final javax.net.ssl.TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
      for (final javax.net.ssl.TrustManager tm : trustManagers) {
        if (tm instanceof X509TrustManager) {
          return (X509TrustManager) tm;
        }
      }
      throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
    } catch (KeyStoreException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Works only with default HttpsURLConnection manager. Better to use OkHttp
   * API or the below API calls directly.
   *
   * @deprecated
   * */
  @Deprecated
  public static void installTrustStore(final SSLSocketFactory socketFactory) {
    HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
  }

}
