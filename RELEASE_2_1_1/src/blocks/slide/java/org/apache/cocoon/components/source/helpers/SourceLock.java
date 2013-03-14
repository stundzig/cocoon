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
package org.apache.cocoon.components.source.helpers;

import java.util.Date;

/**
 * This interface for lock of a source
 *
 * @author <a href="mailto:stephan@vern.chem.tu-berlin.de">Stephan Michels</a>
 * @version CVS $Id: SourceLock.java,v 1.3 2003/09/05 07:31:44 cziegeler Exp $
 */
public class SourceLock {

    private String  subject;
    private String  type;
    private Date    expiration;
    private boolean inheritable;
    private boolean exclusive;

    /**
     * Creates a new lock for a source
     *
     * @param subject Which user should be locked
     * @param type Type of lock
     * @param expiration When the lock expires
     * @param inheritable If the lock is inheritable
     * @param exclusive If the lock is exclusive
     */
    public SourceLock(String subject, String type, Date expiration,
                      boolean inheritable, boolean exclusive) {

        this.subject     = subject;
        this.type        = type;
        this.expiration  = expiration;
        this.inheritable = inheritable;
        this.exclusive   = exclusive;
    }

    /**
     *  Sets the subject for this lock
     *
     * @param subject Which user should be locked
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * return the subject of the lock
     * 
     * @return Which user should be locked
     */
    public String getSubject() {
        return this.subject;
    }

    /**
     * Sets the type of the lock
     *
     * @param type Type of lock
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Return ths type of the lock
     * 
     * @return Type of lock
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the expiration date
     *
     * @param expiration Expiration date
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     * Returns the expiration date
     * 
     * @return Expiration date
     */
    public Date getExpiration() {
        return this.expiration;
    }

    /**
     * Sets the inheritable flag
     *
     * @param inheritable If the lock is inheritable
     */
    public void setInheritable(boolean inheritable) {
        this.inheritable = inheritable;
    }

    /**
     * Returns the inheritable flag
     * 
     * @return If the lock is inheritable
     */
    public boolean isInheritable() {
        return this.inheritable;
    }

    /**
     * Sets the exclusive flag
     *
     * @param exclusive If the lock is exclusive
     */
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    /**
     * Returns the exclusive flag
     * 
     * @return If the lock is exclusive
     */
    public boolean isExclusive() {
        return this.exclusive;
    }
}