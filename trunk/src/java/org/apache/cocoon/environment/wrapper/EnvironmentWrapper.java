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
package org.apache.cocoon.environment.wrapper;

import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.logger.Logger;

import org.apache.cocoon.Processor;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.CocoonComponentManager;
import org.apache.cocoon.environment.AbstractEnvironment;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.BufferedOutputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This is a wrapper class for the <code>Environment</code> object.
 * It has the same properties except that the object model
 * contains a <code>RequestWrapper</code> object.
 *
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @version $Id: EnvironmentWrapper.java,v 1.1 2003/03/09 00:09:30 pier Exp $
 */
public class EnvironmentWrapper 
    extends AbstractEnvironment 
    implements Environment {

    /** The wrapped environment */
    protected Environment environment;

    /** The object model */
    protected Map objectModel;

    /** The redirect url */
    protected String redirectURL;

    /** The request object */
    protected Request request;

    /** The last context */
    protected URL lastContext;

    /** The last prefix */
    protected String lastPrefix;

    /** The last uri */
    protected String lastURI;

    /** The stream to output to */
    protected OutputStream outputStream;

    /**
     * Constructs an EnvironmentWrapper object from a Request
     * and Response objects
     */
    public EnvironmentWrapper(Environment env,
                              String      requestURI,
                              String      queryString,
                              Logger      logger)
    throws MalformedURLException {
        this(env, requestURI, queryString, logger, false);
    }

    /**
     * Constructs an EnvironmentWrapper object from a Request
     * and Response objects
     */
    public EnvironmentWrapper(Environment env,
                              String      requestURI,
                              String      queryString,
                              Logger      logger,
                              boolean     rawMode)
    throws MalformedURLException {
        this(env, requestURI, queryString, logger, null, rawMode);
    }

    /**
     * Constructs an EnvironmentWrapper object from a Request
     * and Response objects
     */
    public EnvironmentWrapper(Environment      env,
                              String           requestURI,
                              String           queryString,
                              Logger           logger,
                              ComponentManager manager,
                              boolean          rawMode)
    throws MalformedURLException {
        super(env.getURI(), env.getView(), env.getRootContext(), env.getAction());

        this.enableLogging(logger);
        this.environment = env;

        this.context = env.getContext();
        this.prefix = new StringBuffer(env.getURIPrefix());

        // create new object model and replace the request object
        Map oldObjectModel = env.getObjectModel();
        if (oldObjectModel instanceof HashMap) {
            this.objectModel = (Map)((HashMap)oldObjectModel).clone();
        } else {
            this.objectModel = new HashMap(oldObjectModel.size()*2);
            Iterator entries = oldObjectModel.entrySet().iterator();
            Map.Entry entry;
            while (entries.hasNext()) {
                entry = (Map.Entry)entries.next();
                this.objectModel.put(entry.getKey(), entry.getValue());
            }
        }
        this.request = new RequestWrapper(ObjectModelHelper.getRequest(oldObjectModel),
                                          requestURI,
                                          queryString,
                                          this,
                                          rawMode);
        this.objectModel.put(ObjectModelHelper.REQUEST_OBJECT, this.request);
    }

    /**
     * Redirect the client to a new URL is not allowed
     */
    public void redirect(boolean sessionmode, String newURL)
    throws IOException {
        this.redirectURL = newURL;

        // check if session mode shall be activated
        if (sessionmode) {
            // get session from request, or create new session
            request.getSession(true);
        }
    }

    /**
     * Redirect in the first non-wrapped environment
     */
    public void globalRedirect(boolean sessionmode, String newURL)
    throws IOException {
        if (environment instanceof EnvironmentWrapper) {
            ((EnvironmentWrapper)environment).globalRedirect(sessionmode, newURL);
        } else {
            environment.redirect(sessionmode,newURL);
        }
    }

    /**
     * Get the output stream
     */
    public OutputStream getOutputStream()
    throws IOException {
      return this.outputStream == null
        ? this.environment.getOutputStream()
        : this.outputStream;
    }

    public OutputStream getOutputStream(int bufferSize)
    throws IOException {
        return this.outputStream == null
                ? this.environment.getOutputStream()
                : this.outputStream;
    }

    /**
     * Set the output stream for this environment. It hides the one of the
     * wrapped environment.
     */
    public void setOutputStream(OutputStream stream) {
        this.outputStream = stream;
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
        if (getOutputStream() != null
            && getOutputStream() instanceof BufferedOutputStream) {
            ((BufferedOutputStream)getOutputStream()).clearBuffer();
            return true;
        }
        else
          return super.tryResetResponse();
    }

    /**
     * Commit the response
     */
    public void commitResponse() 
    throws IOException {
        if (getOutputStream() != null
            && getOutputStream() instanceof BufferedOutputStream) {
            ((BufferedOutputStream)getOutputStream()).realFlush();
        }
        else
          super.commitResponse();
    }

    /**
     * if a redirect should happen this returns the url,
     * otherwise <code>null</code> is returned
     */
    public String getRedirectURL() {
        return this.redirectURL;
    }

    public void reset() {
        this.redirectURL = null;
    }

    /**
     * Set the StatusCode
     */
    public void setStatus(int statusCode) {
        // ignore this
    }

    public void setContentLength(int length) {
        // ignore this
    }

    /**
     * Set the ContentType
     */
    public void setContentType(String contentType) {
        // ignore this
    }

    /**
     * Get the ContentType
     */
    public String getContentType() {
        // ignore this
        return null;
    }

    /**
     * Get the underlying object model
     */
    public Map getObjectModel() {
        return this.objectModel;
    }

    /**
     * Set a new URI for processing. If the prefix is null the
     * new URI is inside the current context.
     * If the prefix is not null the context is changed to the root
     * context and the prefix is set.
     */
    public void setURI(String prefix, String uris) {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug("Setting uri (prefix=" + prefix + ", uris=" + uris + ")");
        }
        if (prefix != null) {
            setContext(getRootContext());
            setURIPrefix(prefix);
        }
        this.uris = uris;
        this.lastURI = uris;
        this.lastContext = this.context;
        this.lastPrefix = this.prefix.toString();
    }

    public void changeContext(String prefix, String context)
    throws MalformedURLException {
        super.changeContext(prefix, context);
        this.lastContext = this.context;
        this.lastPrefix  = this.prefix.toString();
        this.lastURI     = this.uris;
    }

    /**
     * Change the current context to the last one set by changeContext()
     * and return last processor
     */
    public Processor changeToLastContext() {
        this.setContext(this.lastContext);
        this.setURIPrefix(this.lastPrefix);
        this.uris = this.lastURI;
		// HACK: As processing enters sitemap, capture current processor.
		// If pipeline is successfully assembled, this will contain proper processor.
		// Used by cocoon protocol.
		// FIXME (CZ) : Is this the right place? This was before
		//               in the setComponentManager method!
        return CocoonComponentManager.getCurrentProcessor();
    }

    /**
     * Generates SAX events from the given source
     * <b>NOTE</b> : if the implementation can produce lexical events, care should be taken
     * that <code>handler</code> can actually
     * directly implement the LexicalHandler interface!
     * @param  source    the data
     * @throws ProcessingException if no suitable converter is found
     */
    public void toSAX( org.apache.excalibur.source.Source source,
                       ContentHandler handler )
    throws SAXException, IOException, ProcessingException {
        this.environment.toSAX( source, handler );
    }

    /**
     * Lookup an attribute in this instance, and if not found search it
     * in the wrapped environment.
     *
     * @param name a <code>String</code>, the name of the attribute to
     * look for
     * @return an <code>Object</code>, the value of the attribute or
     * null if no such attribute was found.
     */
    public Object getAttribute(String name)
    {
        Object value = super.getAttribute(name);
        if (value == null)
            value = environment.getAttribute(name);

        return value;
    }

    /**
     * Remove attribute from the current instance, as well as from the
     * wrapped environment.
     *
     * @param name a <code>String</code> value
     */
    public void removeAttribute(String name) {
        super.removeAttribute(name);
        environment.removeAttribute(name);
    }

}
