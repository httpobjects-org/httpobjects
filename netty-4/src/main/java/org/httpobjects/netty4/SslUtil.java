package org.httpobjects.netty4;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;

public class SslUtil {
    public static SslContext buildSslContext(File keystoreFile, String keystorePassword, String certPassword){
        try{
            return SslContextBuilder.forServer(SslUtil.buildKmf(keystoreFile, keystorePassword, certPassword)).build();
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }
    public static KeyManagerFactory buildKmf(File keystoreFile, String keystorePassword, String certPassword) {

        final String PROTOCOL = "TLS";
        final String ALGORITHM="ssl.KeyManagerFactory.algorithm";
        final String KEYSTORE_TYPE="JKS";

        try {

            InputStream inputStream = new FileInputStream(keystoreFile);

            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(inputStream, keystorePassword.toCharArray());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, certPassword.toCharArray());
            return kmf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}