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
package org.apache.cocoon.environment;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.CocoonComponentManager;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.util.BufferedOutputStream;
import org.apache.commons.collections.iterators.IteratorEnumeration;

/**
 * Base class for any environment
 *
 * @author <a href="mailto:bluetkemeier@s-und-n.de">Bj&ouml;rn L&uuml;tkemeier</a>
 * @author <a href="mailto:Giacomo.Pati@pwr.ch">Giacomo Pati</a>
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @version CVS $Id: AbstractEnvironment.java,v 1.20 2003/10/27 07:57:26 cziegeler Exp $
 */
public abstract class AbstractEnvironment 
    extends AbstractLogEnabled 
    implements Environment {

    /** The current uri in progress */
    protected String uris;

    /** The current prefix to strip off from the request uri  - TODO (CZ) Remove this*/
    protected StringBuffer prefix = new StringBuffer();

    /** The View requested */
    protected String view;

    /** The Action requested */
    protected String action;

     /** The Context path  - TODO (CZ) Remove this*/
    protected String context;

    /** The context path stored temporarily between constructor and initComponents 
     *    - TODO (CZ) Remove this*/
    private String tempInitContext;

    /** The root context path  - TODO (CZ) Remove this*/
    protected String rootContext;

    /** The servlet object model */
    protected HashMap objectModel;

    /** The real source resolver  - TODO (CZ) Remove this*/
    protected org.apache.excalibur.source.SourceResolver sourceResolver;

    /** The service manager - TODO (CZ) Remove this */
    protected ServiceManager manager;

    /** The attributes */
    private Map attributes = new HashMap();

    /** The secure Output Stream */
    protected BufferedOutputStream secureOutputStream;

    /** The real output stream */
    protected OutputStream outputStream;

    /** Do we have our components ?  - TODO (CZ) Remove this */
    protected boolean initializedComponents = false;
    
    /**
     * Constructs the abstract environment
     */
    public AbstractEnvironment(String uri, String view, File file)
    throws MalformedURLException {
        this(uri, view, file, null);
    }

    /**
     * Constructs the abstract environment
     */
    public AbstractEnvironment(String uri, String view, File file, String action)
    throws MalformedURLException {
        this(uri, view, file.toURL().toExternalForm(), action);
    }

    /**
     * Constructs the abstract environment
     */
    public AbstractEnvironment(String uri, String view, String context, String action)
    throws MalformedURLException {
        this.uris = uri;
        this.view = view;
        this.tempInitContext = context;
        this.action = action;
        this.objectModel = new HashMap();
    }

    // Sitemap methods

    /**
     * Returns the uri in progress. The prefix is stripped off
     */
    public String getURI() {
        return this.uris;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Environment#setURI(java.lang.String)
     */
    public void setURI(String value) {
        this.uris = value;
    }

    /**
     * Get the Root Context
     * TODO (CZ) Remove this method
     */
    public String getRootContext() {
        if ( !this.initializedComponents) {
            this.initComponents();
        }
        return this.rootContext;
    }

    /**
     * Get the current Context
     * TODO (CZ) Remove this method
     */
    public String getContext() {
        if ( !this.initializedComponents) {
            this.initComponents();
        }
        return this.context;
    }

    /**
     * Get the prefix of the URI in progress
     * TODO (CZ) Remove this method
     */
    public String getURIPrefix() {
        return this.prefix.toString();
    }

    /**
     * Set the prefix of the URI in progress
     * TODO (CZ) Remove this method
     */
    protected void setURIPrefix(String prefix) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Set the URI Prefix (OLD=" + getURIPrefix() + ", NEW=" +  prefix + ")");
        }
        this.prefix = new StringBuffer(prefix);
    }

    /**
     * Set the context.
     * TODO (CZ) Remove this method
     */
    protected void setContext(String context) {
        this.context = context;
    }

    /**
     * Set the context. This is similar to changeContext()
     * except that it is absolute.
     * TODO (CZ) Remove this method
     */
    public void setContext(String prefix, String uri, String context) {
        this.setContext(context);
        this.setURIPrefix(prefix == null ? "" : prefix);
        this.uris = uri;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Reset context to " + this.context);
        }
    }

    /**
     * Adds an prefix to the overall stripped off prefix from the request uri
     * TODO (CZ) Remove this method
     */
    public void changeContext(String prefix, String newContext)
    throws IOException {
        if ( !this.initializedComponents) {
            this.initComponents();
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Changing Cocoon context");
            getLogger().debug("  from context(" + this.context + ") and prefix(" + this.prefix + ")");
            getLogger().debug("  to context(" + newContext + ") and prefix(" + prefix + ")");
            getLogger().debug("  at URI " + this.uris);
        }
        int l = prefix.length();
        if (l >= 1) {
            if (!this.uris.startsWith(prefix)) {
                String message = "The current URI (" + this.uris +
                                 ") doesn't start with given prefix (" + prefix + ")";
                getLogger().error(message);
                throw new RuntimeException(message);
            }
            this.prefix.append(prefix);
            this.uris = this.uris.substring(l);

            // check for a slash at the beginning to avoid problems with subsitemaps
            if (this.uris.startsWith("/")) {
                this.uris = this.uris.substring(1);
                this.prefix.append('/');
            }
        }

        if (SourceUtil.getScheme(this.context).equals("zip")) {
            // if the resource is zipped into a war file (e.g. Weblogic temp deployment)
            // FIXME (VG): Is this still required? Better to unify both cases.
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Base context is zip: " + this.context);
            }
            
            org.apache.excalibur.source.Source source = null;
            try {
                source = this.sourceResolver.resolveURI(this.context + newContext);
                this.context = source.getURI();
            } finally {
                this.sourceResolver.release(source);
            }
        } else {
            String sContext;
            // if we got a absolute context or one with a protocol resolve it
            if (newContext.charAt(0) == '/') {
                // context starts with the '/' - absolute file URL
                sContext = "file:" + newContext;
            } else if (newContext.indexOf(':') > 1) {
                // context have ':' - absolute URL
                sContext = newContext;
            } else {
                // context is relative to old one
                sContext = this.context + '/' + newContext;
            }

            // Cut the file name part from context (if present)
            int i = sContext.lastIndexOf('/');
            if (i != -1 && i + 1 < sContext.length()) {
                sContext = sContext.substring(0, i + 1);
            }
            
            org.apache.excalibur.source.Source source = null;
            try {
                source = this.sourceResolver.resolveURI(sContext);
                this.context = source.getURI();
            } finally {
                this.sourceResolver.release(source);
            }
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("New context is " + this.context);
        }
    }

    /**
     * Redirect the client to a new URL
     */
    public abstract void redirect(boolean sessionmode, String newURL) throws IOException;

    public void globalRedirect(boolean sessionmode, String newURL) throws IOException {
        redirect(sessionmode, newURL);
    }

    // Request methods

    /**
     * Returns the request view
     */
    public String getView() {
        return this.view;
    }

    /**
     * Returns the request action
     */
    public String getAction() {
        return this.action;
    }

    // Response methods

    /**
     * Set a status code
     */
    public void setStatus(int statusCode) {
    }

    // Object model method

    /**
     * Returns a Map containing environment specific objects
     */
    public Map getObjectModel() {
        return this.objectModel;
    }

    /**
     * Check if the response has been modified since the same
     * "resource" was requested.
     * The caller has to test if it is really the same "resource"
     * which is requested.
     * @return true if the response is modified or if the
     *         environment is not able to test it
     */
    public boolean isResponseModified(long lastModified) {
        return true; // always modified
    }

    /**
     * Mark the response as not modified.
     */
    public void setResponseIsNotModified() {
        // does nothing
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public Enumeration getAttributeNames() {
        return new IteratorEnumeration(this.attributes.keySet().iterator());
    }

    /**
     * Get the output stream where to write the generated resource.
     * The returned stream is buffered by the environment. If the
     * buffer size is -1 then the complete output is buffered.
     * If the buffer size is 0, no buffering takes place.
     * This method replaces {@link #getOutputStream()}.
     */
    public OutputStream getOutputStream(int bufferSize)
    throws IOException {
        if (bufferSize == -1) {
            if (this.secureOutputStream == null) {
                this.secureOutputStream = new BufferedOutputStream(this.outputStream);
            }
            return this.secureOutputStream;
        } else if (bufferSize == 0) {
            return this.outputStream;
        } else {
            this.outputStream = new java.io.BufferedOutputStream(this.outputStream, bufferSize);
            return this.outputStream;
        }
    }

    /**
     * Reset the response if possible. This allows error handlers to have
     * a higher chance to produce clean output if the pipeline that raised
     * the error has already output some data.
     *
     * @return true if the response was successfully reset
    */
    public boolean tryResetResponse()
    throws IOException {
        if (this.secureOutputStream != null) {
            this.secureOutputStream.clearBuffer();
            return true;
        }
        return false;
    }

    /**
     * Commit the response
     */
    public void commitResponse()
    throws IOException {
        if (this.secureOutputStream != null) {
            this.secureOutputStream.realFlush();
        } else if ( this.outputStream != null ){
            this.outputStream.flush();
        }
    }

    /**
     * Initialize the components for the environment
     * This gets the source resolver and the xmlizer component
     * TODO (CZ) Remove this method
     */
    protected void initComponents() {
        this.initializedComponents = true;
        try {
            this.manager = CocoonComponentManager.getSitemapComponentManager();
            this.sourceResolver = (org.apache.excalibur.source.SourceResolver)this.manager.lookup(org.apache.excalibur.source.SourceResolver.ROLE);
            if (this.tempInitContext != null) {
                org.apache.excalibur.source.Source source = null;
                try {
                    source = this.sourceResolver.resolveURI(this.tempInitContext);
                    this.context = source.getURI();
                    
                    if (this.rootContext == null) // hack for EnvironmentWrapper
                        this.rootContext = this.context;
                } finally {
                    this.sourceResolver.release(source);
                }
                this.tempInitContext = null;
            }
        } catch (ServiceException ce) {
            // this should never happen!
            throw new CascadingRuntimeException("Unable to lookup component.", ce);
        } catch (IOException ie) {
            throw new CascadingRuntimeException("Unable to resolve URI: "+this.tempInitContext, ie);
        }
    }
    
    /**
     * Notify that the processing starts.
     */
    public void startingProcessing() {
        // do nothing here
    }

    /**
     * Notify that the processing is finished
     * This can be used to cleanup the environment object
     */
    public void finishingProcessing() {
        // TODO (CZ) Remove this
        if ( null != this.manager ) {
            this.manager.release( this.sourceResolver );
            this.manager = null;
            this.sourceResolver = null;
        }
        this.initializedComponents = false;
    }
}
