package org.wordpress.android.stores.network;

import android.content.Context;
import android.support.annotation.Nullable;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MemorizingTrustManager implements X509TrustManager {
    public static final String KEYSTORE_FILENAME = "wpstore_certs_truststore.bks";
    public static final String KEYSTORE_PASSWORD = "secret";

    private X509TrustManager mDefaultTrustManager;
    private KeyStore mLocalKeyStore;
    private X509Certificate mLastFailure;
    private Context mContext;

    public MemorizingTrustManager(Context appContext) {
        mContext = appContext;
        try {
            mLocalKeyStore = loadTrustStore();
        } catch (FileNotFoundException e) {
            // Init the key store for the first time
            try {
                initLocalKeyStoreFile();
                mLocalKeyStore = loadTrustStore();
            } catch (IOException | GeneralSecurityException e1) {
                throw new IllegalStateException(e1);
            }
        } catch (IOException | GeneralSecurityException e) {
            AppLog.e(T.API, e);
            throw new IllegalStateException(e);
        }
        mDefaultTrustManager = getTrustManager(null);
        if (mDefaultTrustManager == null) {
            throw new IllegalStateException("Couldn't find X509TrustManager");
        }
    }

    private X509TrustManager getTrustManager(@Nullable KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            for (TrustManager t : tmf.getTrustManagers()) {
                if (t instanceof X509TrustManager) {
                    return (X509TrustManager) t;
                }
            }
        } catch (Exception e) {
            // no op
        }
        return null;
    }

    private KeyStore loadTrustStore() throws IOException, GeneralSecurityException {
        File localKeyStoreFile = new File(mContext.getFilesDir(), KEYSTORE_FILENAME);
        KeyStore localKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = new FileInputStream(localKeyStoreFile);
        try {
            localKeyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
        } finally {
            in.close();
        }
        return localKeyStore;
    }

    private void saveTrustStore(KeyStore localKeyStore) throws IOException, GeneralSecurityException {
        FileOutputStream out = null;
        try {
            File localKeyStoreFile = new File(mContext.getFilesDir(), KEYSTORE_FILENAME);
            out = new FileOutputStream(localKeyStoreFile);
            localKeyStore.store(out, KEYSTORE_PASSWORD.toCharArray());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    AppLog.e(T.UTILS, e);
                }
            }
        }
    }

    private void initLocalKeyStoreFile() throws GeneralSecurityException, IOException {
        FileOutputStream out = null;
        try {
            File localKeyStoreFile = new File(mContext.getFilesDir(), KEYSTORE_FILENAME);
            out = new FileOutputStream(localKeyStoreFile);
            KeyStore localTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            localTrustStore.load(null, KEYSTORE_PASSWORD.toCharArray());
            localTrustStore.store(out, KEYSTORE_PASSWORD.toCharArray());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    AppLog.e(T.UTILS, e);
                }
            }
        }
    }

    public boolean isCertificateAccepted(X509Certificate cert) {
        try {
            return mLocalKeyStore.getCertificateAlias(cert) != null;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    public void storeLastFailure() {
        storeCert(mLastFailure);
    }

    public void storeCert(X509Certificate cert) {
        try {
            mLocalKeyStore.setCertificateEntry(cert.getSubjectDN().toString(), cert);
            saveTrustStore(mLocalKeyStore);
        } catch (IOException | GeneralSecurityException e) {
            AppLog.e(T.API, "Unable to store the certificate: " + cert);
        }
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        mDefaultTrustManager.checkClientTrusted(chain, authType);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            mDefaultTrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException ce) {
            mLastFailure = chain[0];
            if (isCertificateAccepted(chain[0])) {
                // Certificate has already been accepted by the user
                return;
            }
            throw ce;
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return mDefaultTrustManager.getAcceptedIssuers();
    }

    public X509Certificate getLastFailure() {
        return mLastFailure;
    }
}
