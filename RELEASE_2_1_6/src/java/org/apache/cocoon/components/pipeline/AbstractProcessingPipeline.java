/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.pipeline;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentSelector;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ConnectionResetException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.CocoonComponentManager;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.generation.Generator;
import org.apache.cocoon.reading.Reader;
import org.apache.cocoon.serialization.Serializer;
import org.apache.cocoon.sitemap.SitemapModelComponent;
import org.apache.cocoon.sitemap.SitemapParameters;
import org.apache.cocoon.transformation.Transformer;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.cocoon.xml.XMLProducer;
import org.apache.excalibur.source.SourceValidity;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This is the base for all implementations of a <code>ProcessingPipeline</code>.
 *
 * @since 2.1
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @version CVS $Id$
 */
public abstract class AbstractProcessingPipeline
  extends AbstractLogEnabled
  implements ProcessingPipeline, Parameterizable, Recyclable {

    // Generator stuff
    protected Generator generator;
    protected Parameters generatorParam;
    protected String generatorSource;
    protected ComponentSelector generatorSelector;

    // Transformer stuff
    protected ArrayList transformers = new ArrayList();
    protected ArrayList transformerParams = new ArrayList();
    protected ArrayList transformerSources = new ArrayList();
    protected ArrayList transformerSelectors = new ArrayList();

    // Serializer stuff
    protected Serializer serializer;
    protected Parameters serializerParam;
    protected String serializerSource;
    protected String serializerMimeType;
    protected String sitemapSerializerMimeType;
    protected OutputComponentSelector serializerSelector;

    // Reader stuff
    protected Reader reader;
    protected Parameters readerParam;
    protected String readerSource;
    protected String readerMimeType;
    protected String sitemapReaderMimeType;
    protected OutputComponentSelector readerSelector;

    /**
     * True when pipeline has been prepared.
     */
    private boolean prepared;

    /**
     * This is the last component in the pipeline, either the serializer
     * or a custom xmlconsumer for the cocoon: protocol etc.
     */
    protected XMLConsumer lastConsumer;

    /** The component manager set with compose() */
    protected ComponentManager manager;

    /** The component manager set with compose() and recompose() */
    protected ComponentManager newManager;

    /** The configuration */
    protected Parameters configuration;

    /** The parameters */
    protected Parameters parameters;

    /** Expires value */
    protected long expires;

    /** Configured Expires value */
    protected long configuredExpires;

    /** Configured Output Buffer Size */
    protected int  configuredOutputBufferSize;

    /** Output Buffer Size */
    protected int  outputBufferSize;

    /**
     * Composable Interface
     */
    public void compose(ComponentManager manager)
    throws ComponentException {
        this.manager = manager;
        this.newManager = manager;
    }

    /**
     * Recomposable Interface
     */
    public void recompose(ComponentManager manager)
    throws ComponentException {
        this.newManager = manager;
    }

    /**
     * Parameterizable Interface - Configuration
     */
    public void parameterize(Parameters params)
    throws ParameterException {
        this.configuration = params;
        final String expiresValue = params.getParameter("expires", null);
        if (expiresValue != null) {
            this.configuredExpires = parseExpires(expiresValue);
        }
        this.configuredOutputBufferSize = params.getParameterAsInteger("outputBufferSize", -1);
    }

    /**
     * Setup this component
     */
    public void setup(Parameters params) {
        this.parameters = params;
        final String expiresValue = params.getParameter("expires", null);
        if (expiresValue != null) {
            this.expires = parseExpires(expiresValue);
        } else {
            this.expires = this.configuredExpires;
        }
        this.outputBufferSize = params.getParameterAsInteger("outputBufferSize",
                                                              this.configuredOutputBufferSize);
    }

    /**
     * Release this component
     * If you get an instance not by a component manager but for example
     * by a processor, you have to release this component by calling
     * this method and NOT by using a component manager!
     */
    public void release() {
        try {
            CocoonComponentManager.removeFromAutomaticRelease(this);
        } catch (ProcessingException pe) {
            // ignore this
            getLogger().error("Unabled to release processing component.", pe);
        }
    }

    /**
     * Get the generator - used for content aggregation
     */
    public Generator getGenerator() {
        return this.generator;
    }

    /**
     * Informs pipeline we have come across a branch point
     * Default Behaviour is do nothing
     */
    public void informBranchPoint() {
        // this can be overwritten in subclasses
    }

    /**
     * Set the generator that will be used as the initial step in the pipeline.
     * The generator role is given : the actual <code>Generator</code> is fetched
     * from the latest <code>ComponentManager</code> given by <code>compose()</code>
     * or <code>recompose()</code>.
     *
     * @param role the generator role in the component manager.
     * @param source the source where to produce XML from, or <code>null</code> if no
     *        source is given.
     * @param param the parameters for the generator.
     * @throws ProcessingException if the generator couldn't be obtained.
     */
    public void setGenerator (String role, String source, Parameters param, Parameters hintParam)
    throws ProcessingException {
        if (this.generator != null) {
            throw new ProcessingException ("Generator already set. Cannot set generator '" + role +
                "' at " + getLocation(param));
        }
        if (this.reader != null) {
            throw new ProcessingException ("Reader already set. Cannot set generator '" + role +
                "' at " + getLocation(param));
        }
        try {
            this.generatorSelector = (ComponentSelector) this.newManager.lookup(Generator.ROLE + "Selector");
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of generator selector failed at " +getLocation(param), ce);
        }
        try {
            this.generator = (Generator) this.generatorSelector.select(role);
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of generator '" + role + "' failed at " + getLocation(param), ce);
        }
        this.generatorSource = source;
        this.generatorParam = param;
    }

    /**
     * Add a transformer at the end of the pipeline.
     * The transformer role is given : the actual <code>Transformer</code> is fetched
     * from the latest <code>ComponentManager</code> given by <code>compose()</code>
     * or <code>recompose()</code>.
     *
     * @param role the transformer role in the component manager.
     * @param source the source used to setup the transformer (e.g. XSL file), or
     *        <code>null</code> if no source is given.
     * @param param the parameters for the transfomer.
     * @throws ProcessingException if the generator couldn't be obtained.
     */
    public void addTransformer (String role, String source, Parameters param, Parameters hintParam)
    throws ProcessingException {
        if (this.reader != null) {
            // Should normally never happen as setting a reader starts pipeline processing
            throw new ProcessingException ("Reader already set. Cannot add transformer '" + role +
                "' at " + getLocation(param));
        }
        if (this.generator == null) {
            throw new ProcessingException ("Must set a generator before adding transformer '" + role +
                "' at " + getLocation(param));
        }
        ComponentSelector selector = null;
        try {
            selector = (ComponentSelector) this.newManager.lookup(Transformer.ROLE + "Selector");
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of transformer selector failed at " + getLocation(param), ce);
        }
        try {
            this.transformers.add(selector.select(role));
            this.transformerSelectors.add(selector);
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of transformer '"+role+"' failed at " + getLocation(param), ce);
        }
        this.transformerSources.add(source);
        this.transformerParams.add(param);
    }

    /**
     * Set the serializer for this pipeline
     * @param mimeType Can be null
     */
    public void setSerializer (String role, String source, Parameters param, Parameters hintParam, String mimeType)
    throws ProcessingException {
        if (this.serializer != null) {
            // Should normally not happen as adding a serializer starts pipeline processing
            throw new ProcessingException ("Serializer already set. Cannot set serializer '" + role +
                "' at " + getLocation(param));
        }
        if (this.reader != null) {
            // Should normally never happen as setting a reader starts pipeline processing
            throw new ProcessingException ("Reader already set. Cannot set serializer '" + role +
                "' at " + getLocation(param));
        }
        if (this.generator == null) {
            throw new ProcessingException ("Must set a generator before setting serializer '" + role +
                "' at " + getLocation(param));
        }

        try {
            this.serializerSelector = (OutputComponentSelector) this.newManager.lookup(Serializer.ROLE + "Selector");
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of serializer selector failed at " + getLocation(param), ce);
        }
        try {
            this.serializer = (Serializer)serializerSelector.select(role);
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of serializer '" + role + "' failed at " + getLocation(param), ce);
        }
        this.serializerSource = source;
        this.serializerParam = param;
        this.serializerMimeType = mimeType;
        this.sitemapSerializerMimeType = serializerSelector.getMimeTypeForHint(role);
        this.lastConsumer = this.serializer;
    }

    /**
     * Set the reader for this pipeline
     * @param mimeType Can be null
     */
    public void setReader (String role, String source, Parameters param, String mimeType)
    throws ProcessingException {
        if (this.reader != null) {
            // Should normally never happen as setting a reader starts pipeline processing
            throw new ProcessingException ("Reader already set. Cannot set reader '" + role +
                "' at " + getLocation(param));
        }
        if (this.generator != null) {
            // Should normally never happen as setting a reader starts pipeline processing
            throw new ProcessingException ("Generator already set. Cannot use reader '" + role +
                "' at " + getLocation(param));
        }

        try {
            this.readerSelector = (OutputComponentSelector) this.newManager.lookup(Reader.ROLE + "Selector");
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of reader selector failed at " + getLocation(param), ce);
        }
        try {
            this.reader = (Reader)readerSelector.select(role);
        } catch (ComponentException ce) {
            throw new ProcessingException("Lookup of reader '"+role+"' failed at " + getLocation(param), ce);
        }
        this.readerSource = source;
        this.readerParam = param;
        this.readerMimeType = mimeType;
        this.sitemapReaderMimeType = readerSelector.getMimeTypeForHint(role);
    }

    /**
     * Sanity check
     * @return true if the pipeline is 'sane', false otherwise.
     */
    protected boolean checkPipeline() {
        if (this.generator == null && this.reader == null) {
            return false;
        }

        if (this.generator != null && this.serializer == null) {
            return false;
        }

        return true;
    }

    /**
     * Setup pipeline components.
     */
    protected void setupPipeline(Environment environment)
    throws ProcessingException {
        try {
            // setup the generator
            this.generator.setup(
                environment,
                environment.getObjectModel(),
                generatorSource,
                generatorParam
            );

            Iterator transformerItt = this.transformers.iterator();
            Iterator transformerSourceItt = this.transformerSources.iterator();
            Iterator transformerParamItt = this.transformerParams.iterator();

            while (transformerItt.hasNext()) {
                Transformer trans = (Transformer)transformerItt.next();
                trans.setup(environment,
                            environment.getObjectModel(),
                            (String)transformerSourceItt.next(),
                            (Parameters)transformerParamItt.next()
                );
            }

            if (this.serializer instanceof SitemapModelComponent) {
                ((SitemapModelComponent)this.serializer).setup(
                    environment,
                    environment.getObjectModel(),
                    this.serializerSource,
                    this.serializerParam
                );
            }
        } catch (SAXException e) {
            throw new ProcessingException("Could not setup pipeline.", e);
        } catch (IOException e) {
            throw new ProcessingException("Could not setup pipeline.", e);
        }
    }

    /**
     * Connect the next component
     */
    protected void connect(Environment environment,
                           XMLProducer producer,
                           XMLConsumer consumer)
    throws ProcessingException {
        // Connect next component.
        producer.setConsumer(consumer);
    }

    /**
     * Connect the XML pipeline.
     */
    protected void connectPipeline(Environment environment)
    throws ProcessingException {
        XMLProducer prev = this.generator;

        Iterator itt = this.transformers.iterator();
        while (itt.hasNext()) {
            Transformer next = (Transformer) itt.next();
            connect(environment, prev, next);
            prev = next;
        }

        // insert the serializer
        connect(environment, prev, this.lastConsumer);
    }

    /**
     * Process the given <code>Environment</code>, producing the output.
     */
    public boolean process(Environment environment)
    throws ProcessingException {
        if (!this.prepared) {
            preparePipeline(environment);
        }

        // See if we need to set an "Expires:" header
        if (this.expires != 0) {
            Response res = ObjectModelHelper.getResponse(environment.getObjectModel());
            res.setDateHeader("Expires", System.currentTimeMillis() + expires);
            res.setHeader("Cache-Control", "max-age=" + expires/1000 + ", public");
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Setting a new Expires object for this resource");
            }
            environment.getObjectModel().put(ObjectModelHelper.EXPIRES_OBJECT,
                                             new Long(expires + System.currentTimeMillis()));
        }

        if (this.reader != null) {
            if (checkIfModified(environment, this.reader.getLastModified())) {
                return true;
            }

            return processReader(environment);
        } else {
            // If this is an internal request, lastConsumer was reset!
            if (this.lastConsumer == null) {
                this.lastConsumer = this.serializer;
            }

            connectPipeline(environment);
            return processXMLPipeline(environment);
        }
    }

    /**
     * Prepare the pipeline
     */
    protected void preparePipeline(Environment environment)
    throws ProcessingException {
        if (!checkPipeline()) {
            throw new ProcessingException("Attempted to process incomplete pipeline.");
        }

        if (this.prepared) {
            throw new ProcessingException("Duplicate preparePipeline call caught.");
        }

        if (this.reader != null) {
            setupReader(environment);
        } else {
            setupPipeline(environment);
        }
        this.prepared = true;
    }

    /**
     * Prepare an internal processing
     * @param environment          The current environment.
     * @throws ProcessingException
     */
    public void prepareInternal(Environment environment)
    throws ProcessingException {
        this.lastConsumer = null;
        preparePipeline(environment);
    }

    /**
     * Process the SAX event pipeline
     */
    protected boolean processXMLPipeline(Environment environment)
    throws ProcessingException {

        setMimeTypeForSerializer(environment);

        try {
            if (this.lastConsumer == null) {
                // internal processing
                this.generator.generate();
            } else {
                if (this.serializer.shouldSetContentLength()) {
                    // set the output stream
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    this.serializer.setOutputStream(os);

                    // execute the pipeline:
                    this.generator.generate();
                    environment.setContentLength(os.size());
                    os.writeTo(environment.getOutputStream(0));
                } else {
                    // set the output stream
                    this.serializer.setOutputStream(environment.getOutputStream(this.outputBufferSize));
                    // execute the pipeline:
                    this.generator.generate();
                }
            }
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            // TODO: Unwrap SAXException ?
            throw new ProcessingException("Failed to execute pipeline.", e);
        }
        return true;
    }

    /**
     * Setup the reader
     */
    protected void setupReader(Environment environment)
    throws ProcessingException {
        try {
            this.reader.setup(environment,environment.getObjectModel(),readerSource,readerParam);
            // set the expires parameter on the pipeline if the reader is configured with one
            if (readerParam.isParameter("expires")) {
	            // should this checking be done somewhere else??
	            this.expires = readerParam.getParameterAsLong("expires");
            }
        } catch (SAXException e){
            throw new ProcessingException("Failed to execute reader pipeline.", e);
        } catch (ParameterException e) {
            throw new ProcessingException("Expires parameter needs to be of type long.",e);
        } catch (IOException e){
            throw new ProcessingException("Failed to execute reader pipeline.", e);
        }
    }

    /**
     * Set the mime-type for a reader
     * @param environment The current environment
     */
    protected void setMimeTypeForReader(Environment environment) {
        String mimeType = this.reader.getMimeType();
        if ( mimeType != null ) {
            environment.setContentType(mimeType);
        } else if ( readerMimeType != null ) {
            environment.setContentType(this.readerMimeType);
        } else {
            environment.setContentType(this.sitemapReaderMimeType);
        }
    }

    /**
     * Set the mime-type for a serializer
     * @param environment The current environment
     */
    protected void setMimeTypeForSerializer(Environment environment)
    throws ProcessingException {
        if (this.lastConsumer == null) {
            // internal processing: text/xml
            environment.setContentType("text/xml");
        } else {
            String mimeType = this.serializer.getMimeType();
            if (mimeType != null) {
                // we have a mimeType from the component itself
                environment.setContentType (mimeType);
            } else if (serializerMimeType != null) {
                // there was a mimeType specified in the sitemap pipeline
                environment.setContentType (serializerMimeType);
            } else if (this.sitemapSerializerMimeType != null) {
                // use the mimeType specified in the sitemap component declaration
                environment.setContentType (this.sitemapSerializerMimeType);
            } else {
                // No mimeType available
                String message = "Unable to determine MIME type for " +
                    environment.getURIPrefix() + "/" + environment.getURI();
                throw new ProcessingException(message);
            }
        }
    }

    protected boolean checkIfModified(Environment environment,
                                        long lastModified)
    throws ProcessingException {
        // has the read resource been modified?
        if(!environment.isResponseModified(lastModified)) {
            // environment supports this, so we are finished
            environment.setResponseIsNotModified();
            return true;
        }
        return false;
    }

    /**
     * Process the pipeline using a reader.
     * @throws ProcessingException if
     */
    protected boolean processReader(Environment environment)
    throws ProcessingException {
        try {
            this.setMimeTypeForReader(environment);
            if (this.reader.shouldSetContentLength()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                this.reader.setOutputStream(os);
                this.reader.generate();
                environment.setContentLength(os.size());
                os.writeTo(environment.getOutputStream(0));
            } else {
                this.reader.setOutputStream(environment.getOutputStream(this.outputBufferSize));
                this.reader.generate();
            }
        } catch (Exception e) {
            handleException(e);
        }

        return true;
    }

    public void recycle() {
        this.prepared = false;
        
        // Release reader.
        if (this.readerSelector != null) {
            this.readerSelector.release(this.reader);
            this.newManager.release(this.readerSelector);
            this.readerSelector = null;
            this.reader = null;
            this.readerParam = null;
        }

        // Release generator.
        if (this.generatorSelector != null) {
            this.generatorSelector.release(this.generator);
            this.newManager.release(this.generatorSelector);
            this.generatorSelector = null;
            this.generator = null;
            this.generatorParam = null;
        }

        // Release transformers
        int size = this.transformerSelectors.size();
        for (int i = 0; i < size; i++) {
            final ComponentSelector selector =
                    (ComponentSelector) this.transformerSelectors.get(i);
            selector.release((Component) this.transformers.get(i));
            this.newManager.release(selector);
        }
        this.transformerSelectors.clear();
        this.transformers.clear();
        this.transformerParams.clear();
        this.transformerSources.clear();

        // Release serializer
        if (this.serializerSelector != null) {
            this.serializerSelector.release(this.serializer);
            this.newManager.release(this.serializerSelector);
            this.serializerSelector = null;
            this.serializerParam = null;
        }
        this.serializer = null;
        this.parameters = null;
        this.lastConsumer = null;
    }

    /**
     * Process the given <code>Environment</code>, but do not use the
     * serializer. Instead the sax events are streamed to the XMLConsumer.
     */
    public boolean process(Environment environment, XMLConsumer consumer)
    throws ProcessingException {
        this.lastConsumer = consumer;
        if (this.reader != null) {
            throw new ProcessingException("Streaming of an internal pipeline is not possible with a reader.");
        }
        connectPipeline(environment);
        return processXMLPipeline(environment);
    }

    /**
     * Return valid validity objects for the event pipeline
     * If the "event pipeline" (= the complete pipeline without the
     * serializer) is cacheable and valid, return all validity objects.
     * Otherwise return <code>null</code>
     */
    public SourceValidity getValidityForEventPipeline() {
        return null;
    }

    /**
     * Parse the expires parameter
     */
    private long parseExpires(String expire) {
        StringTokenizer tokens = new StringTokenizer(expire);

        // get <base>
        String current = tokens.nextToken();
        if (current.equals("modification")) {
            getLogger().warn("the \"modification\" keyword is not yet" +
                             " implemented. Assuming \"now\" as the base attribute");
            current = "now";
        }

        if (!current.equals("now") && !current.equals("access")) {
            getLogger().error("bad <base> attribute, Expires header will not be set");
            return -1;
        }

        long number = 0;
        long modifier = 0;
        long expires = 0;

        while (tokens.hasMoreTokens()) {
            current = tokens.nextToken();

            // get rid of the optional <plus> keyword
            if (current.equals("plus")) {
                current = tokens.nextToken();
            }

            // We're expecting a sequence of <number> and <modification> here
            // get <number> first
            try {
                number = Long.parseLong(current);
            } catch (NumberFormatException nfe) {
                getLogger().error("state violation: a number was expected here");
                return -1;
            }

            // now get <modifier>
            try {
                current = tokens.nextToken();
            } catch (NoSuchElementException nsee) {
                getLogger().error("State violation: expecting a modifier" +
                                  " but no one found: Expires header will not be set");
            }
            if (current.equals("years")) {
                modifier = 365L * 24L * 60L * 60L * 1000L;
            } else if (current.equals("months")) {
                modifier = 30L * 24L * 60L * 60L * 1000L;
            } else if (current.equals("weeks")) {
                modifier = 7L * 24L * 60L * 60L * 1000L;
            } else if (current.equals("days")) {
                modifier = 24L * 60L * 60L * 1000L;
            } else if (current.equals("hours")) {
                modifier = 60L * 60L * 1000L;
            } else if (current.equals("minutes")) {
                modifier = 60L * 1000L;
            } else if (current.equals("seconds")) {
                modifier = 1000L;
            } else {
                getLogger().error("Bad modifier (" + current +
                                  "): ignoring expires configuration");
                return -1;
            }
            expires += number * modifier;
        }

        return expires;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.components.pipeline.ProcessingPipeline#getKeyForEventPipeline()
     */
    public String getKeyForEventPipeline() {
        return null;
    }

    protected String getLocation(Parameters param) {
        String value = null;
        if ( param instanceof SitemapParameters ) {
            value = ((SitemapParameters)param).getStatementLocation();
        }
        if ( value == null ) {
            value = "[unknown location]";
        }
        return value;
    }

    /**
     * Handles exception which can happen during pipeline processing.
     * @throws ConnectionResetException if connection reset detected
     * @throws ProcessingException in all other cases
     */
    protected void handleException(Exception e) throws ProcessingException {
        if (e instanceof SocketException) {
            if (e.getMessage().indexOf("reset") > 0
                    || e.getMessage().indexOf("aborted") > 0
                    || e.getMessage().indexOf("connection abort") > 0) {
                throw new ConnectionResetException("Connection reset by peer", e);
            }
        } else if (e instanceof IOException) {
            // Tomcat5 wraps SocketException into ClientAbortException which extends IOException.
            if (e.getClass().getName().endsWith("ClientAbortException")) {
                throw new ConnectionResetException("Connection reset by peer", e);
            }
        } else if (e instanceof ProcessingException) {
            throw (ProcessingException) e;
        }

        throw new ProcessingException("Error executing pipeline.", e);
    }
}