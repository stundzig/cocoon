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
package org.apache.cocoon.webapps.portal.context;

import org.apache.avalon.framework.component.ComponentManager;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.webapps.portal.PortalConstants;
import org.apache.cocoon.webapps.portal.components.PortalManager;
import org.apache.cocoon.webapps.session.context.SessionContext;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.cocoon.xml.dom.DOMBuilder;
import org.apache.cocoon.xml.dom.DOMUtil;
import org.apache.excalibur.source.SourceParameters;
import org.apache.excalibur.source.SourceResolver;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *  The portal context
 *
 *  This context allows access to various parts of a portal profile.
 *  The context provides reading of the following xml, if the current
 *  resource is running inside a portal module:
 * &lt;layout&gt;
 *     &lt;portal&gt;
 *         ...
 *     &lt;/portal&gt;
 *     &lt;coplets&gt;
 *         ...
 *     &lt;/coplets&gt;
 * &lt;/layout&gt;
 * &lt;configuration&gt;
 *     ...
 * &lt;/configuration&gt;
 *
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * @version CVS $Id: SessionContextImpl.java,v 1.2 2003/05/04 20:19:41 cziegeler Exp $
*/
public final class SessionContextImpl
implements SessionContext {

    /* This contains all information about the currently processed coplet */
    public static ThreadLocal copletInfo = new ThreadLocal();

    /** The context name */
    private String   name;

    /** The attributes */
    private Map      attributes = new HashMap();

    /** The cached layoutDOM */
    private Document layoutDOM;

    /** The cached configurationDOM */
    private Document configurationDOM;

    /** The current profile */
    private Map      profile;

    /** All coplet parameters */

    private SourceParameters copletPars;

    /** The status profile */
    private Element  statusProfile;

    /** the coplet id */
    private String copletID;

    /** The number of the coplet */
    private String copletNumber;

    /** The profile ID */
    private String profileID;

    /** The portal URI */
    private String portalURI;

    /** The media type */
    private String mediaType;

    /** The current request */
    private Request request;
    
    public SessionContextImpl(String  name,
                              Map     objectModel,
                              PortalManager portal)
    throws IOException, SAXException, ProcessingException {
        this.setup(name, null, null);

        // try to get the resource connector info
        Map info = (Map)SessionContextImpl.copletInfo.get();
        if (info != null) {
            SessionContextImpl.copletInfo.set(null);
            this.copletPars = (SourceParameters)info.get(PortalConstants.COPLETINFO_PARAMETERS);
            this.portalURI = (String)info.get(PortalConstants.COPLETINFO_PORTALURI);
            if (this.copletPars != null) {
                this.copletID = this.copletPars.getParameter(PortalConstants.PARAMETER_ID);
                this.copletNumber = this.copletPars.getParameter(PortalConstants.PARAMETER_NUMBER);
                if (this.copletID != null && this.copletNumber != null) {
                    this.portalURI = this.portalURI + (this.portalURI.indexOf('?') == -1 ? '?' : '&')
                          + "portalcmd=update_" + this.copletID + "_" + this.copletNumber;
                }
            }
            this.statusProfile = (Element)info.get(PortalConstants.COPLETINFO_STATUSPROFILE);
        }
        this.mediaType = (this.copletPars != null ? (String)copletPars.getParameter(PortalConstants.PARAMETER_MEDIA)
                                                  : portal.getMediaType());
        // get the profile

        SessionContext context = portal.getContext(false);
        if (context != null) {
            if (context.getAttribute(PortalManager.ATTRIBUTE_PORTAL_ROLE) != null) {
                this.profileID = portal.getProfileID(PortalManager.BUILDTYPE_VALUE_ID,
                  (String)context.getAttribute(PortalManager.ATTRIBUTE_PORTAL_ROLE),
                  (String)context.getAttribute(PortalManager.ATTRIBUTE_PORTAL_ID), false);
                this.profile = portal.retrieveProfile(this.profileID);
            }
        }
        this.getConfigurationDOM(portal);
        this.request = ObjectModelHelper.getRequest( objectModel );
    }

    /**
     * Get the name of the context
     */
    public String getName() {
        return this.name;
    }

    public Request getRequest() {
        return this.request;
    }
    
    /**
     * Get the layout DOM
     */
    private void getLayoutDOM()
    throws ProcessingException {
        if (this.layoutDOM == null && this.profile != null) {
            try {
                Map portalLayouts = (Map)this.profile.get(PortalConstants.PROFILE_PORTAL_LAYOUTS);
                Map copletLayouts = (Map)this.profile.get(PortalConstants.PROFILE_COPLET_LAYOUTS);
                DOMBuilder builder = new DOMBuilder();
                builder.startDocument();
                PortalManager.streamLayoutProfile(builder, portalLayouts, copletLayouts, this.mediaType);
                builder.endDocument();
                this.layoutDOM = builder.getDocument();
            } catch (SAXException local) {
                throw new ProcessingException("Unable to get portal." + local, local);
            }
        }
    }

    /**
     * Get the configuration DOM
     */
    private void getConfigurationDOM(PortalManager portal)
    throws ProcessingException, IOException {
        if (this.configurationDOM == null && portal != null) {
            try {
                String contextID = null;
                if (this.copletID != null && this.copletNumber != null) {
                    contextID = "coplet_"+copletID+"_"+copletNumber;
                }
                DOMBuilder builder = new DOMBuilder();
                builder.startDocument();
                portal.streamConfiguration(builder,
                                            this.portalURI,
                                            this.profileID,
                                            this.mediaType,
                                            contextID);
                builder.endDocument();
                this.configurationDOM = builder.getDocument();
            } catch (SAXException local) {
                throw new ProcessingException("Unable to get portal." + local, local);
            }
        }
    }

    /* Set the context name */
    public void setup(String value, String load, String save) {
        name = value;
    }

    /**
     * Get the xml fragment
     */
    public synchronized DocumentFragment getXML(String path)
    throws ProcessingException {
        DocumentFragment result = null;

        if (path.startsWith("/") == true) path = path.substring(1);
        NodeList list = null;

        if (path == null || path.equals("") == true) {
            Document doc = DOMUtil.createDocument();
            result = doc.createDocumentFragment();
            this.getLayoutDOM();
            if (this.layoutDOM != null) {
                result.appendChild(doc.importNode(this.layoutDOM.getDocumentElement(), true));
            }
            if (this.configurationDOM != null) {
                result.appendChild(doc.importNode(this.configurationDOM.getDocumentElement(), true));
            }

            if (this.statusProfile != null) {
                if (this.copletID != null && this.copletNumber != null) {
                    String statusPath = "customization/coplet[@id='"+copletID+"' and @number='"+copletNumber+"']";
                    try {
                        Node node = DOMUtil.getSingleNode(this.statusProfile, statusPath);
                        if (node != null) {
                            Element copletData = doc.createElementNS(null, "coplet-data");
                            NodeList childs = node.getChildNodes();
                            if (childs != null) {
                                for(int l=0; l<childs.getLength(); l++) {
                                    copletData.appendChild(doc.importNode(childs.item(l), true));
                                }
                            }
                            result.appendChild(copletData);
                        }
                    } catch (javax.xml.transform.TransformerException localException) {
                        throw new ProcessingException("TransformerException: " + localException, localException);
                    }
                }
            }
        }

        if (path.equals("layout") == true
            || path.startsWith("layout/") == true) {
            try {
                this.getLayoutDOM();
                if (this.layoutDOM != null) list = DOMUtil.selectNodeList(this.layoutDOM, path);
            } catch (javax.xml.transform.TransformerException localException) {
                throw new ProcessingException("TransformerException: " + localException, localException);
            }
        }

        if (path.equals("configuration") == true
            || path.startsWith("configuration/") == true) {
            try {
                if (this.configurationDOM != null) list = DOMUtil.selectNodeList(this.configurationDOM, path);
            } catch (javax.xml.transform.TransformerException localException) {
                throw new ProcessingException("TransformerException: " + localException, localException);
            }
        }

        if (path.startsWith("coplet-data/") == true
            || path.equals("coplet-data") == true) {

            if (this.statusProfile != null) {
                if (this.copletID != null && this.copletNumber != null) {
                    String statusPath = "customization/coplet[@id='"+copletID+"' and @number='"+copletNumber+"']";
                    if (path.startsWith("coplet-data/") == true) {
                        statusPath = statusPath + path.substring(11);
                    }
                    try {
                        list = DOMUtil.selectNodeList(this.statusProfile, statusPath);
                    } catch (javax.xml.transform.TransformerException localException) {
                        throw new ProcessingException("TransformerException: " + localException, localException);
                    }
                }
            }
        }

        if (list != null && list.getLength() > 0) {
            Document doc = DOMUtil.createDocument();
            result = doc.createDocumentFragment();

            for(int i = 0; i < list.getLength(); i++) {

                // the found node is either an attribute or an element
                if (list.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
                    // if it is an attribute simple create a new text node with the value of the attribute
                    result.appendChild(doc.createTextNode(list.item(i).getNodeValue()));
                } else {
                    // now we have an element
                    // copy all children of this element in the resulting tree
                    NodeList childs = list.item(i).getChildNodes();
                    if (childs != null) {
                        for(int m = 0; m < childs.getLength(); m++) {
                            result.appendChild(doc.importNode(childs.item(m), true));
                        }
                    }
                }
            }
        }

        return result;
    }


    /**
     * Set the xml
     */
    public synchronized void setXML(String path, DocumentFragment fragment)
    throws ProcessingException {
        if (path != null) {
            if (path.startsWith("/") == true) path = path.substring(1);
            if (path.startsWith("coplet-data/") == true
                || path.equals("coplet-data") == true) {

                if (this.statusProfile != null) {
                    if (this.copletID != null && this.copletNumber != null) {
                        String statusPath = "customization/coplet[@id='"+copletID+"' and @number='"+copletNumber+"']";
                        if (path.startsWith("coplet-data/") == true) {
                            statusPath = statusPath + path.substring(11);
                        }

                        Node node = DOMUtil.selectSingleNode(this.statusProfile, statusPath);
                        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                            // now we have to serialize the fragment to a string and insert this
                            Attr attr = (Attr)node;
                            attr.setNodeValue(DOMUtil.getValueOfNode(fragment));
                        } else {

                            // remove old childs
                            while (node.hasChildNodes() == true) {
                                node.removeChild(node.getFirstChild());
                            }

                            // Insert new childs
                            NodeList childs = fragment.getChildNodes();
                            if (childs != null && childs.getLength() > 0) {
                                for(int i = 0; i < childs.getLength(); i++) {
                                    Node n = this.statusProfile.getOwnerDocument().importNode(childs.item(i), true);
                                    node.appendChild(n);
                                }
                            }
                        }

                        if (this.copletPars.getParameter(PortalConstants.PARAMETER_PERSISTENT, "false").equals("true") == true) {
                            this.profile.put(PortalConstants.PROFILE_SAVE_STATUS_FLAG, "true");
                        }
                    }
                }
            }
        }

    }

    /**
     * Append a document fragment at the given path. The implementation of this
     * method is context specific.
     * Usually the children of the fragment are appended as new children of the
     * node specified by the path.
     * If the path is not existent it is created.
     */
    public synchronized void appendXML(String path, DocumentFragment fragment)
    throws ProcessingException {
        throw new ProcessingException("appendXML() not implemented.");
    }

    /**
     * Remove nodes
     */
    public synchronized void removeXML(String path)
    throws ProcessingException {
        throw new ProcessingException("removeXML() not implemented.");
    }

    /**
     * Get a copy the first node specified by the path.
     */
    public synchronized Node getSingleNode(String path)
    throws ProcessingException {
        // Node result = null;
        throw new ProcessingException("getSingleNode() not implemented.");
        // return result;
    }

    /**
     * Get a copy all the nodes specified by the path.
     */
    public synchronized NodeList getNodeList(String path)
    throws ProcessingException {
        // NodeList result = null;
        throw new ProcessingException("getNodeList() not implemented.");
        // return result;
    }

    /**
     * Set the value of a node. The node is copied before insertion.
     */
    public synchronized void setNode(String path, Node node)
    throws ProcessingException {
        throw new ProcessingException("setNode() not implemented.");
    }


    /**
     * Set a context attribute. If value is null the attribute is removed.
     */
    public synchronized void setAttribute(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    /**
     * Get a context attribute. If the attribute is not available return null
     */
    public synchronized Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Get a context attribute. If the attribute is not available the defaultObject is returned
     */
    public synchronized Object getAttribute(String key, Object defaultObject) {
        Object value = attributes.get(key);
        if (value == null) value = defaultObject;
        return value;
    }

    /**
     * Get the value of this node. This is similiar to the xsl:value-of
     * function. If the node does not exist, <code>null</code> is returned.
     */
    public synchronized String getValueOfNode(String path)
    throws ProcessingException {
        // String value = null;
        throw new ProcessingException("getValueOfNode() not implemented.");
        // return value;
    }

    /**
     * Set the value of a node.
     */
    public synchronized void setValueOfNode(String path, String value)
    throws ProcessingException {
        throw new ProcessingException("setValueOfNode() not implemented.");
    }

    /**
     * Stream the XML directly to the handler. This streams the contents of getXML()
     * to the given handler without creating a DocumentFragment containing a copy
     * of the data
     */
    public synchronized boolean streamXML(String path,
                           ContentHandler contentHandler,
                           LexicalHandler lexicalHandler)
    throws SAXException, ProcessingException {
        boolean  streamed = false;
        DocumentFragment fragment = this.getXML(path);
        if (fragment != null) {
            streamed = true;
            IncludeXMLConsumer.includeNode(fragment, contentHandler, lexicalHandler);
        }
        return streamed;
    }

    /**
     * Try to load XML into the context.
     * If the context does not provide the ability of loading,
     * an exception is thrown.
     */
    public void loadXML(String path,
                        SourceParameters   parameters,
                        Map                objectModel,
                        SourceResolver     resolver,
                        ComponentManager   manager)
    throws SAXException, ProcessingException, IOException {
        throw new ProcessingException("The context " + this.name + " does not support loading.");
    }

    /**
     * Try to save XML from the context.
     * If the context does not provide the ability of saving,
     * an exception is thrown.
     */
    public void saveXML(String path,
                        SourceParameters   parameters,
                        Map                objectModel,
                        SourceResolver     resolver,
                        ComponentManager   manager)
    throws SAXException, ProcessingException, IOException {
        throw new ProcessingException("The context " + this.name + " does not support saving.");
    }
}

