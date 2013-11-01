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
package org.apache.felix.webconsole.internal.servlet;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


/**
 * The <code>Password</code> class encapsulates encoding and decoding
 * operations on plain text and hashed passwords.
 * <p>
 * Encoded hashed passwords are strings of the form
 * <code>{hashAlgorithm}base64-encoded-password-hash</code> where
 * <i>hashAlgorithm</i> is the name of the hash algorithm used to hash
 * the password and <i>base64-encoded-password-hash</i> is the password
 * hashed with the indicated hash algorithm and subsequently encoded in
 * Base64.
 */
class Password
{

    // the default hash algorithm (part of the Java Platform since 1.4)
    private static final String DEFAULT_HASH_ALGO = "SHA-256";

    // the hash algorithm used to hash the password or null
    // if the password is not hashed at all
    private final String hashAlgo;

    // the hashed or plain password
    private final byte[] password;


    /**
     * Returns {@code true} if the given {@code textPassword} is hashed
     * and encoded as described in the class comment.
     *
     * @param textPassword
     * @return
     * @throws NullPointerException if {@code textPassword} is {@code null}.
     */
    static boolean isPasswordHashed( final String textPassword )
    {
        return getEndOfHashAlgorithm( textPassword ) >= 0;
    }


    /**
     * Returns the given plain {@code textPassword} as an encoded hashed
     * password string as described in the class comment.
     *
     * @param textPassword
     * @return
     * @throws NullPointerException if {@code textPassword} is {@code null}.
     */
    static String hashPassword( final String textPassword )
    {
        final byte[] bytePassword = Base64.getBytesUtf8( textPassword );
        return hashPassword( DEFAULT_HASH_ALGO, bytePassword );
    }


    Password( String textPassword )
    {
        this.hashAlgo = getPasswordHashAlgorithm( textPassword );
        this.password = getPasswordBytes( textPassword );
    }


    /**
     * Returns {@code true} if this password matches the password
     * {@code toCompare}. If this password is hashed, the {@code toCompare}
     * password is hashed, too, with the same hash algorithm before
     * comparison.
     *
     * @param toCompare
     * @return
     * @throws NullPointerException if {@code toCompare} is {@code null}.
     */
    boolean matches( final byte[] toCompare )
    {
        return Arrays.equals( this.password, hashPassword( toCompare, this.hashAlgo ) );
    }


    /**
     * Returns this password as a string hashed and encoded as described
     * by the class comment. If this password has not been hashed originally,
     * the default hash algorithm <i>SHA-256</i> is applied.
     */
    public String toString()
    {
        return hashPassword( this.hashAlgo, this.password );
    }


    private static String hashPassword( final String hashAlgorithm, final byte[] password )
    {
        final String actualHashAlgo = ( hashAlgorithm == null ) ? DEFAULT_HASH_ALGO : hashAlgorithm;
        final byte[] hashedPassword = hashPassword( password, actualHashAlgo );
        final StringBuffer buf = new StringBuffer( 2 + actualHashAlgo.length() + hashedPassword.length * 3 );
        buf.append( '{' ).append( actualHashAlgo.toLowerCase() ).append( '}' );
        buf.append( Base64.newStringUtf8( Base64.encodeBase64( hashedPassword ) ) );
        return buf.toString();
    }


    private static String getPasswordHashAlgorithm( final String textPassword )
    {
        final int endHash = getEndOfHashAlgorithm( textPassword );
        if ( endHash >= 0 )
        {
            return textPassword.substring( 1, endHash );
        }

        // password is plain text, hence no algorithm
        return null;
    }


    private static byte[] getPasswordBytes( final String textPassword )
    {
        final int endHash = getEndOfHashAlgorithm( textPassword );
        if ( endHash >= 0 )
        {
            final String encodedPassword = textPassword.substring( endHash + 1 );
            return Base64.decodeBase64( encodedPassword );
        }

        return Base64.getBytesUtf8( textPassword );
    }


    private static int getEndOfHashAlgorithm( final String textPassword )
    {
        if ( textPassword.startsWith( "{" ) )
        {
            final int endHash = textPassword.indexOf( "}" );
            if ( endHash > 0 )
            {
                return endHash;
            }
        }

        return -1;
    }


    private static byte[] hashPassword( final byte[] pwd, final String hashAlg )
    {
        // no hashing if no hash algorithm
        if ( hashAlg == null || hashAlg.length() == 0 )
        {
            return pwd;
        }

        try
        {
            final MessageDigest md = MessageDigest.getInstance( hashAlg );
            return md.digest( pwd );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new IllegalStateException( "Cannot hash the password: " + e );
        }
    }
}
