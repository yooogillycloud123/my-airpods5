package com.aaairpods.bridge;

import android.content.Context;
import android.os.Build;
import android.sun.misc.BASE64Encoder;
import android.sun.security.provider.X509Factory;
import android.sun.security.x509.AlgorithmId;
import android.sun.security.x509.CertificateAlgorithmId;
import android.sun.security.x509.CertificateExtensions;
import android.sun.security.x509.CertificateIssuerName;
import android.sun.security.x509.CertificateSerialNumber;
import android.sun.security.x509.CertificateSubjectName;
import android.sun.security.x509.CertificateValidity;
import android.sun.security.x509.CertificateVersion;
import android.sun.security.x509.CertificateX509Key;
import android.sun.security.x509.KeyIdentifier;
import android.sun.security.x509.PrivateKeyUsageExtension;
import android.sun.security.x509.SubjectKeyIdentifierExtension;
import android.sun.security.x509.X500Name;
import android.sun.security.x509.X509CertImpl;
import android.sun.security.x509.X509CertInfo;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

public class LocalAdbConnectionManager extends AbsAdbConnectionManager {
    private static AbsAdbConnectionManager INSTANCE;

    public static synchronized AbsAdbConnectionManager getInstance(Context context) throws Exception {
        if (INSTANCE == null) INSTANCE = new LocalAdbConnectionManager(context.getApplicationContext());
        return INSTANCE;
    }

    private PrivateKey privateKey;
    private Certificate certificate;

    private LocalAdbConnectionManager(Context context) throws Exception {
        setApi(Build.VERSION.SDK_INT);
        setHostAddress("127.0.0.1");
        // A paired device on Android trusts this certificate, not this app's
        // display name. Never silently create a different identity if exactly
        // one credential is missing or the existing pair does not match.
        File keyFile = new File(context.getFilesDir(), "adb_private.key");
        File certFile = new File(context.getFilesDir(), "adb_cert.pem");
        boolean keyExists = keyFile.isFile();
        boolean certExists = certFile.isFile();
        if (!keyExists && !certExists) {
            // First install only: no previous app-side ADB identity existed.
            if (!ExistingAdbIdentityGate.mayCreateNewIdentity(
                    ExistingAdbIdentityGate.classify(false,false,false,false,false)))
                throw new IOException("FRESH_ADB_IDENTITY_NOT_ALLOWED");
            generateAndStore(context);
        } else {
            // Any invalid/partial legacy cache must remain intact for
            // inspection or manual recovery. No automatic overwrite.
            if (!keyExists || !certExists)
                throw new IOException("EXISTING_ADB_IDENTITY_PARTIAL_PRESERVED");
            privateKey = readPrivateKeyFromFile(context);
            certificate = readCertificateFromFile(context);
            boolean matched = sameKeyPair(privateKey,certificate);
            ExistingAdbIdentityGate.State state=ExistingAdbIdentityGate.classify(
                keyExists,certExists,privateKey!=null,certificate!=null,matched);
            if (state!=ExistingAdbIdentityGate.State.PRESERVE_EXISTING_IDENTITY)
                throw new IOException("EXISTING_ADB_IDENTITY_UNVERIFIED_PRESERVED");
        }
    }

    /** Verifies the loaded existing key/certificate without publishing secrets. */
    private static boolean sameKeyPair(PrivateKey key, Certificate cert) {
        if(key==null||cert==null)return false;
        try {
            byte[] challenge="aa-airpods-existing-adb-identity-v1".getBytes(StandardCharsets.US_ASCII);
            Signature signer=Signature.getInstance("SHA256withRSA");
            signer.initSign(key);
            signer.update(challenge);
            byte[] signature=signer.sign();
            Signature checker=Signature.getInstance("SHA256withRSA");
            checker.initVerify(cert.getPublicKey());
            checker.update(challenge);
            return checker.verify(signature);
        } catch(GeneralSecurityException error) {
            return false;
        }
    }

    private void generateAndStore(Context context) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"));
        KeyPair pair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        privateKey = pair.getPrivate();

        String subject = "CN=AA AirPods Bridge";
        String algorithmName = "SHA512withRSA";
        long now = System.currentTimeMillis();
        long expiryDate = now + (10L * 365L * 24L * 60L * 60L * 1000L);

        CertificateExtensions extensions = new CertificateExtensions();
        extensions.set("SubjectKeyIdentifier",
                new SubjectKeyIdentifierExtension(new KeyIdentifier(publicKey).getIdentifier()));

        X500Name x500Name = new X500Name(subject);
        Date notBefore = new Date(now - 60000L);
        Date notAfter = new Date(expiryDate);
        extensions.set("PrivateKeyUsage", new PrivateKeyUsageExtension(notBefore, notAfter));

        CertificateValidity validity = new CertificateValidity(notBefore, notAfter);
        X509CertInfo info = new X509CertInfo();
        info.set("version", new CertificateVersion(2));
        info.set("serialNumber", new CertificateSerialNumber(new Random().nextInt() & Integer.MAX_VALUE));
        info.set("algorithmID", new CertificateAlgorithmId(AlgorithmId.get(algorithmName)));
        info.set("subject", new CertificateSubjectName(x500Name));
        info.set("key", new CertificateX509Key(publicKey));
        info.set("validity", validity);
        info.set("issuer", new CertificateIssuerName(x500Name));
        info.set("extensions", extensions);

        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privateKey, algorithmName);
        certificate = cert;

        writePrivateKeyToFile(context, privateKey);
        writeCertificateToFile(context, certificate);
    }

    @Override
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    protected Certificate getCertificate() {
        return certificate;
    }

    @Override
    protected String getDeviceName() {
        return "AA-AirPods-Bridge";
    }

    private static Certificate readCertificateFromFile(Context context)
            throws IOException, CertificateException {
        File certFile = new File(context.getFilesDir(), "adb_cert.pem");
        if (!certFile.exists()) return null;
        try (InputStream cert = new FileInputStream(certFile)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(cert);
        }
    }

    private static void writeCertificateToFile(Context context, Certificate cert)
            throws CertificateEncodingException, IOException {
        File certFile = new File(context.getFilesDir(), "adb_cert.pem");
        BASE64Encoder encoder = new BASE64Encoder();
        try (OutputStream os = new FileOutputStream(certFile)) {
            os.write(X509Factory.BEGIN_CERT.getBytes(StandardCharsets.UTF_8));
            os.write('\n');
            encoder.encode(cert.getEncoded(), os);
            os.write('\n');
            os.write(X509Factory.END_CERT.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static PrivateKey readPrivateKeyFromFile(Context context)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File keyFile = new File(context.getFilesDir(), "adb_private.key");
        if (!keyFile.exists()) return null;

        byte[] bytes = new byte[(int) keyFile.length()];
        try (InputStream is = new FileInputStream(keyFile)) {
            int off = 0;
            while (off < bytes.length) {
                int n = is.read(bytes, off, bytes.length - off);
                if (n < 0) break;
                off += n;
            }
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return keyFactory.generatePrivate(spec);
    }

    private static void writePrivateKeyToFile(Context context, PrivateKey key)
            throws IOException {
        File keyFile = new File(context.getFilesDir(), "adb_private.key");
        try (OutputStream os = new FileOutputStream(keyFile)) {
            os.write(key.getEncoded());
        }
    }
}
