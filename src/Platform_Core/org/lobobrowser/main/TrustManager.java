package org.lobobrowser.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;

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

  public static SSLInfo makeSSLSocketFactory(final InputStream extraCertsStream) {
    final String sep = File.separator;
    final String hardDefaultPath = System.getProperty("java.home") + sep + "lib" + sep + "security" + sep + "cacerts";
    final String defaultStorePath = System.getProperty("javax.net.ssl.trustStore", hardDefaultPath);
    try (
        final FileInputStream defaultIS = new FileInputStream(defaultStorePath)) {

      final KeyStore defKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      defKeyStore.load(defaultIS, "changeit".toCharArray());

      final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(extraCertsStream, null);

      // final KeyStore keyStore =  KeyStore.Builder.newInstance(defKeyStore, null).getKeyStore();
      final Enumeration<String> aliases = defKeyStore.aliases();
      while (aliases.hasMoreElements()) {
        final String alias = aliases.nextElement();
        if (defKeyStore.isCertificateEntry(alias)) {
          final Entry entry = defKeyStore.getEntry(alias, null);
          keyStore.setEntry(alias, entry, null);
        }
      }

      final SSLContext sc = SSLContext.getInstance("TLS");
      final X509TrustManager x509TM = getX509TM(keyStore);
      sc.init(null, new javax.net.ssl.TrustManager[] {x509TM}, null);
      return new SSLInfo(x509TM, sc.getSocketFactory());
    } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException
        | UnrecoverableEntryException e) {
      throw new RuntimeException(e);
    }

  }

  public static X509TrustManager getX509TM(final KeyStore keyStore) {
    try {
      final String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
      trustManagerFactory.init(keyStore);
      final javax.net.ssl.TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
      X509TrustManager x509tm = null;
      for (int i = 0; i < trustManagers.length; i++) {
        if (trustManagers[i] instanceof X509TrustManager) {
          x509tm = (X509TrustManager) trustManagers[0];
        }
      }
      if (x509tm == null) {
        throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
      }
      return x509tm;
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
