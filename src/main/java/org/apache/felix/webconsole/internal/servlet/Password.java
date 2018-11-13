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


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * The <code>Password</code> class encapsulates encoding and decoding
 * operations on plain text and hashed passwords.
 * <p>
 * Encoded hashed passwords are strings of the form
 * <code>{hashAlgorithm}base64-encoded-password-hash</code> where
 * <i>hashAlgorithm</i> is the name of the hash algorithm used to hash
 * the password and <i>password</i> is the password
 * hashed with the indicated hash algorithm.
 */
class Password
{

    // the default hash algorithm (part of the Java Platform since 1.4)
    private static final String DEFAULT_HASH_ALGO = "SHA-256";
    
    private static final char DELIMITER = '-';
    
    private static final int NO_ITERATIONS = 1;
    
    private static final int DEFAULT_ITERATIONS = 1000;
    
    public static final int DEFAULT_SALT_SIZE = 8;

    // the hash algorithm used to hash the password or null
    // if the password is not hashed at all
    private final String hashAlgo;

    // the hashed or plain password
    private final String password;


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
        String salt = generateSalt(DEFAULT_SALT_SIZE);
        return hashPassword( DEFAULT_HASH_ALGO, DEFAULT_ITERATIONS, salt, textPassword  );
    }

    Password( String textPassword )
    {
        this.hashAlgo = getPasswordHashAlgorithm( textPassword );
        this.password = getPassword(textPassword);
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
        if (this.hashAlgo != null) 
        {
            int startPos = 0;
            String salt = extractSalt(this.password, startPos);
            int iterations = NO_ITERATIONS;
            if (salt != null) 
            {
                startPos += salt.length()+1;
                iterations = extractIterations(this.password, startPos);
               
            }
            String hash = hashPassword(this.hashAlgo, iterations, salt, new String(toCompare));
            final StringBuilder buf = new StringBuilder();
            return compareSecure(buf.append("{").append(this.hashAlgo).append("}").append(password).toString(), hash);
        } else {
            return compareSecure(password, new String(toCompare));
        }
        
    }
    
    private static String hashPassword( final String hashAlgorithm, final int iterations, final  String salt, final String password )
    {
        
        final StringBuilder buf = new StringBuilder();
        buf.append( '{' ).append( hashAlgorithm.toLowerCase() ).append( '}' );
        if (salt != null && !salt.isEmpty()) {
            buf.append(salt).append(DELIMITER);
            if (iterations > NO_ITERATIONS) {
                buf.append(iterations).append(DELIMITER);
            }
            final byte[] hashedPassword = hashPassword( password, salt,iterations, hashAlgorithm );
            buf.append( Base64.newStringUtf8( Base64.encodeBase64( hashedPassword ) ) );
        } else {
            // backwards compatible to previous version: no salt, no iterations
            final byte[] hashedPassword = hashPassword( password, null, NO_ITERATIONS, hashAlgorithm );
            buf.append( Base64.newStringUtf8( Base64.encodeBase64( hashedPassword ) ) );
        }

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

    private static String getPassword( final String textPassword )
    {
        final int endHash = getEndOfHashAlgorithm( textPassword );
        if ( endHash >= 0 )
        {
            final String encodedPassword = textPassword.substring( endHash + 1 );
            return  encodedPassword;
        }

        return textPassword;
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

    private static byte[] hashPassword( final String pwd, final String salt, final int iterations, final String hashAlg )
    {
        try
        {
            StringBuilder data = new StringBuilder();
            if (salt != null) 
            {
                data.append(salt);
            }
            data.append(pwd);
            byte[] bytes =  Base64.getBytesUtf8( data.toString());
            final MessageDigest md = MessageDigest.getInstance( hashAlg );
            for (int i = 0; i < iterations; i++) 
            {
                md.reset();
                bytes = md.digest(bytes);
            }
            return bytes;
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new IllegalStateException( "Cannot hash the password: " + e );
        }
    }
    
    private static boolean compareSecure( final String a,final String b ) 
    {
        int len = a.length();
        if (len != b.length()) 
        {
            return false;
        }
        if (len == 0) 
        {
            return true;
        }
        // don't use conditional operations inside the loop
        int bits = 0;
        for (int i = 0; i < len; i++) 
        {
            // this will never reset any bits
            bits |= a.charAt(i) ^ b.charAt(i);
        }
        return bits == 0;
    }
    
    private static String generateSalt( final  int saltSize ) 
    {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[saltSize];
        random.nextBytes(salt);
        return toHex(salt);
    }
    
    private static String toHex( final byte[] array )
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0) 
        {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        }
        else 
        {
            return hex;
        }
    }
    
    private static String extractSalt( final String hashedPwd, final int start ) 
    {
        if (hashedPwd != null) 
        {
            int end = hashedPwd.indexOf(DELIMITER, start);
            if (end > -1) 
            {
                return hashedPwd.substring(start, end);
            }
        }
        // no salt
        return null;
    }
    
    private static int extractIterations( final String hashedPwd, int start ) 
    {
        if (hashedPwd != null) 
        {
            int end = hashedPwd.indexOf(DELIMITER, start);
            if (end > -1) 
            {
                String str = hashedPwd.substring(start, end);
                try 
                {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) 
                {
                    //nothing to do
                }
            }
        }
        // no extra iterations
        return NO_ITERATIONS;
    }

}
