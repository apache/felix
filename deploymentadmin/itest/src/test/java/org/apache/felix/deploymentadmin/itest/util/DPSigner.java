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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/**
 * Signs a deployment package using a given keypair.
 */
public class DPSigner {
    private static final String META_INF = "META-INF/";

    public static String getSignatureAlgorithm(Key key) {
        if (key instanceof RSAKey) {
            return "SHA256withRSA";
        }
        else if (key instanceof DSAKey) {
            return "SHA1withDSA";
        }
        else if (key instanceof ECKey) {
            return "SHA256withECDSA";
        }
        else {
            throw new IllegalArgumentException("Invalid/unsupported key: " + key.getClass().getName());
        }
    }
    private static String getBlockFileExtension(Key key) {
        if (key instanceof RSAKey) {
            return ".RSA";
        }
        else if (key instanceof DSAKey) {
            return ".DSA";
        }
        else if (key instanceof ECKey) {
            return ".EC";
        }
        else {
            throw new IllegalArgumentException("Invalid/unsupported key: " + key.getClass().getName());
        }
    }
    private final MessageDigest m_digest;

    private final String m_digestAlg;

    private final String m_baseName;

    public DPSigner() {
        this("DP");
    }

    public DPSigner(String baseName) {
        try {
            m_baseName = META_INF.concat(baseName);
            m_digest = MessageDigest.getInstance("SHA-256");
            m_digestAlg = m_digest.getAlgorithm();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported by default?!");
        }
    }
    
    public void addDigestAttribute(Attributes attrs, ArtifactData file) throws IOException {
        attrs.putValue(m_digestAlg.concat("-Digest"), calculateDigest(file));
    }

    public void sign(DeploymentPackageBuilder builder, PrivateKey privKey, X509Certificate cert, OutputStream os) throws Exception {
        Manifest manifest = builder.createManifest();
        List<ArtifactData> artifacts = builder.getArtifactList();
        sign(manifest, artifacts, privKey, cert, os);
    }

    public void sign(Manifest manifest, List<ArtifactData> files, PrivateKey privKey, X509Certificate cert, OutputStream os) throws Exception {
        // For each file, add its signature to the manifest
        for (ArtifactData file : files) {
            String filename = file.getFilename();
            Attributes attrs = manifest.getAttributes(filename);
            addDigestAttribute(attrs, file);
        }

        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            writeSignedManifest(manifest, zos, privKey, cert);

            for (ArtifactData file : files) {
                ZipEntry entry = new ZipEntry(file.getFilename());
                zos.putNextEntry(entry);

                try (InputStream is = file.createInputStream()) {
                    byte[] buf = new byte[1024];
                    int read;
                    while ((read = is.read(buf)) > 0) {
                        zos.write(buf, 0, read);
                    }
                }
            }
        }
    }

    public void writeSignedManifest(Manifest manifest, ZipOutputStream zos, PrivateKey privKey, X509Certificate cert) throws Exception {
        zos.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
        manifest.write(zos);
        zos.closeEntry();

        long now = System.currentTimeMillis();

        // Determine the signature-file manifest...
        Manifest sf = createSignatureFile(manifest);

        byte[] sfRawBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            sf.write(baos);
            sfRawBytes = baos.toByteArray();
        }

        ZipEntry sigFileEntry = new ZipEntry(m_baseName.concat(".SF"));
        sigFileEntry.setTime(now);
        zos.putNextEntry(sigFileEntry);
        // Write the actual entry data...
        zos.write(sfRawBytes, 0, sfRawBytes.length);
        zos.closeEntry();

        // Create a PKCS#7 signature...
        byte[] encoded = calculateSignatureBlock(privKey, cert, sfRawBytes);

        ZipEntry blockFileEntry = new ZipEntry(m_baseName.concat(getBlockFileExtension(privKey)));
        blockFileEntry.setTime(now);
        zos.putNextEntry(blockFileEntry);
        zos.write(encoded);
        zos.closeEntry();
    }

    private String calculateDigest(ArtifactData file) throws IOException {
        m_digest.reset();
        try (InputStream is = file.createInputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) > 0) {
                m_digest.update(buffer, 0, read);
            }
        }
        return Base64.encodeBase64String(m_digest.digest());
    }

    private String calculateDigest(byte[] rawData) throws IOException {
        m_digest.reset();
        m_digest.update(rawData, 0, rawData.length);
        return Base64.encodeBase64String(m_digest.digest());
    }

    private byte[] calculateSignatureBlock(PrivateKey privKey, X509Certificate cert, byte[] sfRawBytes) throws Exception {
        String signatureAlgorithm = getSignatureAlgorithm(privKey);
        
        DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().build();
        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(privKey);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(signer, cert));
        gen.addCertificates(new JcaCertStore(Arrays.asList(cert)));

        CMSSignedData sigData = gen.generate(new CMSProcessableByteArray(sfRawBytes));

        return sigData.getEncoded();
    }

    private Manifest createSignatureFile(Manifest manifest) throws IOException {
        byte[] mfRawBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            manifest.write(baos);
            mfRawBytes = baos.toByteArray();
        }

        Manifest sf = new Manifest();
        Attributes sfMain = sf.getMainAttributes();
        Map<String, Attributes> sfEntries = sf.getEntries();

        sfMain.put(Attributes.Name.SIGNATURE_VERSION, "1.0");
        sfMain.putValue("Created-By", "Apache Felix DeploymentPackageBuilder");
        sfMain.putValue(m_digestAlg + "-Digest-Manifest", calculateDigest(mfRawBytes));
        sfMain.putValue(m_digestAlg + "-Digest-Manifest-Main-Attribute", calculateDigest(getRawBytesMainAttributes(manifest)));

        for (Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            String name = entry.getKey();
            byte[] entryData = getRawBytesAttributes(entry.getValue());

            sfEntries.put(name, getDigestAttributes(entryData));
        }
        return sf;
    }

    private Attributes getDigestAttributes(byte[] rawData) throws IOException {
        Attributes attrs = new Attributes();
        attrs.putValue(m_digestAlg + "-Digest", calculateDigest(rawData));
        return attrs;
    }

    private byte[] getRawBytesAttributes(Attributes attrs) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {

            Method m = Attributes.class.getDeclaredMethod("write", DataOutputStream.class);
            m.setAccessible(true);
            m.invoke(attrs, dos);

            return baos.toByteArray();
        }
        catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get raw bytes of main attributes!", e);
        }
    }

    private byte[] getRawBytesMainAttributes(Manifest manifest) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
            Attributes attrs = manifest.getMainAttributes();

            Method m = Attributes.class.getDeclaredMethod("writeMain", DataOutputStream.class);
            m.setAccessible(true);
            m.invoke(attrs, dos);

            return baos.toByteArray();
        }
        catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get raw bytes of main attributes!", e);
        }
    }
}
