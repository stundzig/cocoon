/*
 * Copyright 2006 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core.container.spring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Stack;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.scope.RequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Own implementation of a {@link XmlWebApplicationContext} which is configured with
 * a base url specifying the root directory for this web application context.
 *
 * @since 2.2
 * @version $Id$
 */
public class CocoonWebApplicationContext extends XmlWebApplicationContext {

    private static final String BEAN_FACTORY_STACK_REQUEST_ATTRIBUTE = CocoonWebApplicationContext.class.getName() + "/Stack";

    /** The base url (already postfixed with a '/'). */
    protected final String baseUrl;

    /** The class loader for this context (or null). */
    protected final ClassLoader classLoader;

    /** The bean definition for this context. */
    protected final String beanDefinition;

    public CocoonWebApplicationContext(ClassLoader           classloader,
                                       WebApplicationContext parent,
                                       String                url,
                                       String                rootDefinition) {
        this.setParent(parent);
        if ( url.endsWith("/") ) {
            this.baseUrl = url;
        } else {
            this.baseUrl = url + '/';
        }
        this.classLoader = (classloader != null ? classloader : ClassUtils.getDefaultClassLoader());
        this.beanDefinition = rootDefinition;
    }

    /**
     * @see org.springframework.web.context.support.XmlWebApplicationContext#loadBeanDefinitions(org.springframework.beans.factory.xml.XmlBeanDefinitionReader)
     */
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
        if ( this.beanDefinition != null ) {
            reader.loadBeanDefinitions(new InputStreamResource(new ByteArrayInputStream(this.beanDefinition.getBytes("utf-8"))));
        }
        super.loadBeanDefinitions(reader);
    }

    /**
     * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext#getResourceByPath(java.lang.String)
     */
    protected Resource getResourceByPath(String path) {
        // only if the path does not start with a "/" and is not a url
        // we assume it is relative
        if ( path != null && !path.startsWith("/") && !ResourceUtils.isUrl(path) ) {
            return super.getResourceByPath(this.baseUrl + path);
        }
        return super.getResourceByPath(path);
    }

    /**
     * A child application context has no default configuration.
     * @see org.springframework.web.context.support.XmlWebApplicationContext#getDefaultConfigLocations()
     */
    protected String[] getDefaultConfigLocations() {
        return null;
    }

    public Object enteringContext(RequestAttributes attributes) {
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        final Object oldContext = attributes.getAttribute(CocoonBeanFactory.BEAN_FACTORY_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if ( oldContext != null ) {
            Stack stack = (Stack)attributes.getAttribute(BEAN_FACTORY_STACK_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if ( stack == null ) {
                stack = new Stack();
                attributes.setAttribute(BEAN_FACTORY_STACK_REQUEST_ATTRIBUTE, stack, RequestAttributes.SCOPE_REQUEST);
            }
            stack.push(oldContext);
        }
        attributes.setAttribute(CocoonBeanFactory.BEAN_FACTORY_REQUEST_ATTRIBUTE, this, RequestAttributes.SCOPE_REQUEST);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        return oldClassLoader;
    }

    public void leavingContext(RequestAttributes attributes, Object handle) {
        Thread.currentThread().setContextClassLoader((ClassLoader)handle);
        final Stack stack = (Stack)attributes.getAttribute(BEAN_FACTORY_STACK_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if ( stack == null ) {
            attributes.removeAttribute(CocoonBeanFactory.BEAN_FACTORY_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        } else {
            final Object oldContext = stack.pop();
            attributes.setAttribute(CocoonBeanFactory.BEAN_FACTORY_REQUEST_ATTRIBUTE, oldContext, RequestAttributes.SCOPE_REQUEST);
            if ( stack.size() == 0 ) {
                attributes.removeAttribute(BEAN_FACTORY_STACK_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            }
        }
    }

    public static WebApplicationContext getCurrentContext(ServletContext servletContext, RequestAttributes attributes) {
        WebApplicationContext parentContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        if (attributes.getAttribute(CocoonBeanFactory.BEAN_FACTORY_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) != null) {
            parentContext = (CocoonWebApplicationContext) attributes
                    .getAttribute(CocoonBeanFactory.BEAN_FACTORY_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        }
        return parentContext;
    }

}
