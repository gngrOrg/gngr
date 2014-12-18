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
import java.util.Enumeration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public final class TrustManager {

  public static SSLSocketFactory makeSSLSocketFactory(final InputStream extraCertsStream) {
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

      final TrustManagerFactory tmf =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keyStore);
      final SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, tmf.getTrustManagers(), null);
      return sc.getSocketFactory();
    } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException
        | UnrecoverableEntryException e) {
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
