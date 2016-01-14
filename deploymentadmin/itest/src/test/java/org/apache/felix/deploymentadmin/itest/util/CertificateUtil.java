/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.deploymentadmin.itest.util;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CertificateUtil {

    public enum KeyType {
        RSA, DSA, EC;
    }

    public static class SignerInfo {
        private final KeyPair m_keyPair;
        private final X509Certificate m_cert;

        public SignerInfo(KeyPair keyPair, X509Certificate cert) {
            m_keyPair = keyPair;
            m_cert = cert;
        }

        public X509Certificate getCert() {
            return m_cert;
        }

        public PrivateKey getPrivate() {
            return m_keyPair.getPrivate();
        }

        public PublicKey getPublic() {
            return m_keyPair.getPublic();
        }
    }

    public static SignerInfo createSelfSignedCert(String commonName, KeyType type) throws Exception {
        KeyPairGenerator kpGen;
        switch (type) {
            case RSA:
                kpGen = KeyPairGenerator.getInstance("RSA");
                kpGen.initialize(1024);
                break;
            case DSA:
                kpGen = KeyPairGenerator.getInstance("DSA");
                break;
            case EC:
                kpGen = KeyPairGenerator.getInstance("EC");
                break;
            default:
                throw new IllegalArgumentException("Invalid key type!");
        }

        KeyPair keyPair = kpGen.generateKeyPair();
        X509Certificate cert = createSelfSignedCert(commonName, keyPair);

        return new SignerInfo(keyPair, cert);
    }

    private static X509Certificate createSelfSignedCert(String commonName, KeyPair keypair) throws Exception {
        PublicKey publicKey = keypair.getPublic();
        String keyAlg = DPSigner.getSignatureAlgorithm(publicKey);

        X500Name issuer = new X500Name(commonName);
        BigInteger serial = BigInteger.probablePrime(16, new Random());
        Date notBefore = new Date(System.currentTimeMillis() - 1000);
        Date notAfter = new Date(notBefore.getTime() + 6000);

        SubjectPublicKeyInfo pubKeyInfo;
        try (ASN1InputStream is = new ASN1InputStream(publicKey.getEncoded())) {
            pubKeyInfo = SubjectPublicKeyInfo.getInstance(is.readObject());
        }

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, issuer, pubKeyInfo);
        builder.addExtension(new Extension(Extension.basicConstraints, true, new DEROctetString(new BasicConstraints(false))));

        X509CertificateHolder certHolder = builder.build(new JcaContentSignerBuilder(keyAlg).build(keypair.getPrivate()));
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    /**
     * Creates a new CertificateUtil instance.
     */
    private CertificateUtil() {
        // Nop
    }

}
