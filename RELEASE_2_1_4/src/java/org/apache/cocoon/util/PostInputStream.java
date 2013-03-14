/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.util;

import java.io.IOException;
import java.io.InputStream;
/**
 * The class PostInputStream is a wrapper for InputStream associated with POST message.
 * It allows to control read operation, restricting the number of bytes read to the value returned by getContentLen() method.
 *
 * @author <a href="mailto:Kinga_Dziembowski@hp.com">Kinga Dziembowski</a>
 * @version CVS $Id: PostInputStream.java,v 1.2 2003/11/15 04:21:28 joerg Exp $
 */

public class PostInputStream extends InputStream {

    /**
    * Class name
    */
    public static final String CLASS = PostInputStream.class.getName();

    /** The real InputStream object */
    private InputStream m_inputStream = null;

    /** The length of InputStream */
    private int m_contentLen = 0;

    /** The number of bytes read */
    protected int m_bytesRead = 0;


    /**
    * Creates a PostInputStream
    */
    public PostInputStream() {
        super();
    }
    /**
    * Creates a <code>PostInputStream</code> based on a real InputStream object with the specified
    * post message body length. Saves its  argument, the input stream
    * <code>m_inputStream</code>, for later use.
    *
    * @param   input     the underlying input stream.
    * @param   len   the post message body length.
    * @exception IllegalArgumentException  len <= 0.
    */

    public PostInputStream(final InputStream input, final int len) throws IllegalArgumentException {
        super();
        init(input, len );
    }
    /**
    * Sets the underlying input stream and contentLen value .
    *
    * @param input the input stream; can not be null.
    * @param len the post message body length.
    *
    * @throws IllegalArgumentException
    */
    protected void init(final InputStream input, final int len) throws IllegalArgumentException {
        if (len <= 0) {
            throw new IllegalArgumentException("contentLen <= 0 ");
        }
        this.m_inputStream = input;
        this.m_contentLen = len;
    }

    /**
    * Sets the underlying input stream and contentLen value .
    *
    * @param input the input stream; can not be null.
    * @param len the post message body length.
    *
    * @throws IOException
    */
    public synchronized void setInputStream(final InputStream input, final int len) throws IOException {
        if (m_inputStream != null) {
            close();
        }
        init(input, len);
    }
    /**
    * Returns the underlying input stream.
    *
    * @return inputStream the underlying InputStream.
    */
    public InputStream getInputStream() {
        return( m_inputStream );
    }

    /**
    * Returns the post message body length.
    *
    * @return m_contentLen;
    */

    public int getContentLen() {
        return( m_contentLen );
    }

    /**
    * Reads the next byte from the input stream.  If the end of the stream has been reached, this method returns -1.
    *
    * @return the next byte or -1 if at the end of the stream.
    *
    * @throws IOException
    */
    public synchronized int read() throws IOException {

        checkOpen();
        if (m_bytesRead == m_contentLen) {
            return -1;
        }
        int byt =  m_inputStream.read();
        if (byt != -1) {
           m_bytesRead++;
        }
        return byt;
    }

    /**
    * Reads bytes from this byte-input stream into the specified byte array,
    * starting at the given offset.
    *
    * <p> This method implements the general contract of the corresponding
    * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
    * the <code>{@link InputStream}</code> class.
    * This method delegetes the read operation to the underlying InputStream implementation class but it
    * controlls the number of bytes read from the stream. In the remote situation the underlying InputStream has no knowledge of
    * the length of the stream and the notion of the "end" is undefined. This wrapper class has a knowledge of the
    * length of data send by the requestor by the means of contentLength. This method returns the number of bytes read and
    * accumulates the total number of bytes read in m_bytesRead. When the m_bytesRead is equal to the specified contentLength
    * value the method returns returns -1 to signal the end of data.
    *
    * @param buffer the byte array to read into; can not be null.
    * @param offset the starting offset in the byte array.
    * @param len the maximum number of bytes to read.
    *
    * @return     the number of bytes read, or <code>-1</code> if the end of
    *             the stream has been reached.
    * @exception  IOException  if an I/O error occurs.
    */

    public synchronized int read(byte[] buffer, int offset, int len) throws IOException {
        checkOpen();
        if (m_bytesRead == m_contentLen) {
            return -1;
        }
        int available = Math.min( available(), len );
        int num = m_inputStream.read( buffer, offset, available );
        if (num > 0) {
            m_bytesRead += num;
        }
        return num;
    }

    public synchronized int read(byte[] buffer) throws IOException {

        return read( buffer, 0, buffer.length);
    }


    /**
    * Checks to see if this stream is closed; if it is, an IOException is thrown.
    *
    * @throws IOException
    */
    protected void checkOpen() throws IOException {
        if (m_inputStream == null) {
            throw new IOException("InputStream closed");
        }
    }

    /**
         * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     * Delegates execution to the underlying InputStream implementation class.
     * Checks to see if this stream is closed; if it is, an IOException is thrown.
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized long skip(long n) throws IOException {
        checkOpen();
        if ( m_bytesRead == m_contentLen )
        {
            return ( 0 );
        }
        else
        {
            return ( m_inputStream.skip( n ) );
        }

    }

    /**
    * Returns the number of bytes available from this input stream that can be read without the stream blocking.
    * Delegates execution to the underlying InputStream implementation class.
    * @return available the number of available bytes.
    *
    * @throws IOException
    */
    public synchronized int available() throws IOException {
        checkOpen();
        int avail = m_inputStream.available();
        return (avail == 0 ? (m_contentLen - m_bytesRead) : avail);
    }

    /**
    * Tests if this input stream supports the <code>mark</code>
    * and <code>reset</code> methods. The <code>markSupported</code>
    * method of <code>BufferedInputStream</code> returns
    * <code>false</code>.
    *
    * @return  a <code>boolean</code> indicating if this stream type supports
    *          the <code>mark</code> and <code>reset</code> methods.
    * @see     java.io.InputStream#mark(int)
    * @see     java.io.InputStream#reset()
    */
    public boolean markSupported() {
        return false;
    }

    /**
    * Closes this input stream by closing the underlying stream and marking this one as closed.
    *
    * @throws IOException
    */
    public synchronized void close() throws IOException {
        if (m_inputStream == null) {
            return;
        }
        m_inputStream.close();
        m_inputStream = null;
        m_contentLen = 0;
        m_bytesRead = 0;
    }

    /**
    * Returns a String representation of this.
    *
    * @return string the String representation of this.
    */
    public String toString() {
        return new StringBuffer(getClass().getName())
                .append("[inputStream=").append(m_inputStream)
                .append(",  contentLen=").append(m_contentLen)
                .append("bytesRead=").append(m_bytesRead)
                .append("]").toString();
    }
}