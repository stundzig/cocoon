/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

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
package org.apache.cocoon.portal.layout.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cocoon.portal.layout.LayoutAspectDescription;
import org.apache.cocoon.portal.layout.LayoutDescription;


/**
 * A configured layout
 * 
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * 
 * @version CVS $Id: DefaultLayoutDescription.java,v 1.1 2003/05/19 12:50:58 cziegeler Exp $
 */
public class DefaultLayoutDescription
    implements LayoutDescription  {

    protected String name;
    
    protected String className;
    
    protected String rendererName;
    
    protected List aspects = new ArrayList();

    /**
     * toString
     * @return
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("LayoutDescription(").append(super.toString()).append(")\n");
        buffer.append("    Name=").append(this.name).append('\n');
        buffer.append("    ClassName=").append(this.className).append('\n');
        buffer.append("    RendererName=").append(this.rendererName).append('\n');
        buffer.append("    Aspects=(\n");
        Iterator i = this.aspects.iterator();
        while ( i.hasNext() ) {
            DefaultLayoutAspectDescription d = (DefaultLayoutAspectDescription)i.next();
            buffer.append("             Aspect(").append(d.toString()).append(")\n");
            buffer.append("                 Name=").append(d.getName()).append('\n');
            buffer.append("                 ClassName=").append(d.getClassName()).append('\n');
            buffer.append("                 Persistence=").append(d.getPersistence()).append('\n');
        }
        buffer.append("            )\n");
        return buffer.toString();
    }
    
    /**
     * @return
     */
    public List getAspects() {
        return aspects;
    }

    public void addAspect(LayoutAspectDescription aspect) {
        this.aspects.add(aspect);
    }
    
    /**
     * Return the description for an aspect
     */
    public LayoutAspectDescription getAspect(String name) {
        LayoutAspectDescription desc = null;
        Iterator i = this.aspects.iterator();
        while (desc == null && i.hasNext() ) {
            LayoutAspectDescription current = (LayoutAspectDescription)i.next();
            if ( name.equals(current.getName())) {
                desc = current;
            }
        }
        return desc;
    }
    
    /**
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param string
     */
    public void setClassName(String string) {
        className = string;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        name = string;
    }

    /**
     * @return
     */
    public String getRendererName() {
        return rendererName;
    }

    /**
     * @param string
     */
    public void setRendererName(String string) {
        rendererName = string;
    }

}