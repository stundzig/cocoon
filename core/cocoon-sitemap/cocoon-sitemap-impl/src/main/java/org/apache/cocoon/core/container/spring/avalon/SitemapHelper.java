/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
package org.apache.cocoon.core.container.spring.avalon;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.cocoon.classloader.reloading.Monitor;
import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.processing.ProcessInfoProvider;
import org.apache.cocoon.spring.configurator.WebAppContextUtils;
import org.apache.cocoon.spring.configurator.impl.ChildXmlWebApplicationContext;
import org.apache.cocoon.util.Deprecation;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 *
 * @version $Id$
 * @since 2.2
 */
public class SitemapHelper {

    public static final ThreadLocal PARENT_CONTEXT = new ThreadLocal();

    private static final String CLASSLOADER_CONFIG_NAME = "classloader";

    private static final String DEFAULT_CONFIG_XCONF  = "config/avalon";

    protected static String createDefinition(String     uriPrefix,
                                             String     sitemapLocation,
                                             String     runningMode,
                                             boolean    useDefaultIncludes,
                                             List       beanIncludes,
                                             List       propertyIncludes,
                                             Properties props) {
        final StringBuffer buffer = new StringBuffer();
        addHeader(buffer);
        // Child settings for sitemap
        buffer.append("  <configurator:child-settings");
        addAttribute(buffer, "location", sitemapLocation);
        addAttribute(buffer, "runningMode", runningMode);
        addAttribute(buffer, "useDefaultIncludes", String.valueOf(useDefaultIncludes));
        buffer.append(">\n");
        if ( beanIncludes != null ) {
            final Iterator i = beanIncludes.iterator();
            while ( i.hasNext() ) {
                final IncludeInfo info = (IncludeInfo)i.next();
                buffer.append("    <configurator:include-beans");
                addAttribute(buffer, "src", info.src);
                addAttribute(buffer, "dir", info.dir);
                addAttribute(buffer, "pattern", info.pattern);
                addAttribute(buffer, "optional", String.valueOf(info.optional));
                buffer.append("/>\n");
            }
        }
        if ( propertyIncludes != null ) {
            final Iterator i = propertyIncludes.iterator();
            while ( i.hasNext() ) {
                final IncludeInfo info = (IncludeInfo)i.next();
                buffer.append("    <configurator:include-properties");
                addAttribute(buffer, "dir", info.dir);
                buffer.append("/>\n");
            }
        }
        if ( props != null ) {
            final Iterator kI = props.keySet().iterator();
            while ( kI.hasNext() ) {
                final String key = (String)kI.next();
                buffer.append("    <configurator:property");
                addAttribute(buffer, "name", key.toString());
                addAttribute(buffer, "value", props.getProperty(key));
                buffer.append("/>\n");
            }
        }
        buffer.append("  </configurator:child-settings>\n");
        // Avalon
        buffer.append("  <avalon:sitemap");
        addAttribute(buffer, "location", sitemapLocation);
        addAttribute(buffer, "uriPrefix", uriPrefix);
        buffer.append("/>\n");
        addFooter(buffer);
        return buffer.toString();
    }

    /**
     * Add the header for the xml configuration file.
     */
    protected static void addHeader(StringBuffer buffer) {
        buffer.append("<beans xmlns=\"http://www.springframework.org/schema/beans\"");
        buffer.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        buffer.append(" xmlns:util=\"http://www.springframework.org/schema/util\"");
        buffer.append(" xmlns:configurator=\"http://cocoon.apache.org/schema/configurator\"");
        buffer.append(" xmlns:avalon=\"http://cocoon.apache.org/schema/avalon\"");
        buffer.append(" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd");
        buffer.append(" http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util-2.0.xsd");
        buffer.append(" http://cocoon.apache.org/schema/configurator http://cocoon.apache.org/schema/configurator/cocoon-configurator-1.0.xsd");
        buffer.append(" http://cocoon.apache.org/schema/avalon http://cocoon.apache.org/schema/avalon/cocoon-avalon-1.0.xsd\">\n");
    }

    /**
     * Add the footer for the xml configuration file.
     */
    protected static void addFooter(StringBuffer buffer) {
        buffer.append("</beans>\n");
    }

    /**
     * Append an attribute to the xml stream if it has a value.
     */
    protected static void addAttribute(StringBuffer buffer, String name, String value) {
        if ( value != null ) {
            buffer.append(' ');
            buffer.append(name);
            buffer.append("=\"");
            buffer.append(value);
            buffer.append("\"");
        }
    }

    /**
     * Should the default includes be read for this sitemap?
     */
    protected static boolean isUsingDefaultIncludes(Configuration config) {
        return config.getChild("components").getAttributeAsBoolean("use-default-includes", true);
    }

    /**
     * Get all includes for bean configurations from the sitemap.
     * @param sitemap
     * @return
     */
    protected static List getBeanIncludes(Configuration sitemap)
    throws ConfigurationException {
        final List includes = new ArrayList();
        final Configuration[] includeConfigs = sitemap.getChild("components").getChildren("include-beans");
        for(int i = 0 ; i < includeConfigs.length; i++ ) {
            final String src = includeConfigs[i].getAttribute("src", null);
            final String dir = includeConfigs[i].getAttribute("dir", null);
            final String pattern = includeConfigs[i].getAttribute("pattern", "*.xml");
            final boolean optional = includeConfigs[i].getAttributeAsBoolean("optional", false);

            if ( src != null && dir != null ) {
                throw new ConfigurationException("Element include-beans can either be configured with a directory or with a src, but not with both.", includeConfigs[i]);
            }
            if ( src == null && dir == null ) {
                throw new ConfigurationException("Element include-beans must either be configured with a directory or with a src.", includeConfigs[i]);
            }
            includes.add(new IncludeInfo(src, dir, pattern, optional));
        }
        return includes;
    }

    /**
     * Get all includes for properties from the sitemap.
     * @param sitemap
     * @return
     */
    protected static List getPropertiesIncludes(Configuration sitemap)
    throws ConfigurationException {
        final List includes = new ArrayList();
        final Configuration[] includeConfigs = sitemap.getChild("components").getChildren("include-properties");
        for(int i = 0 ; i < includeConfigs.length; i++ ) {
            final String dir = includeConfigs[i].getAttribute("dir");

            includes.add(new IncludeInfo(null, dir, null, true));
        }
        return includes;
    }

    /**
     * compatibility with 2.1.x - check for global variables in sitemap
     * TODO - This will be removed in later versions!
     */
    protected static Properties getGlobalSitemapVariables(Configuration sitemap)
    throws ConfigurationException {
        Properties variables = null;
        final Configuration varConfig = sitemap.getChild("pipelines").getChild("component-configurations").getChild("global-variables", false);
        if ( varConfig != null ) {
            Deprecation.logger.warn("The 'component-configurations' section in the sitemap is deprecated. Please check for alternatives.");
            variables = new Properties();
            final Configuration[] variableElements = varConfig.getChildren();
            for(int v=0; v<variableElements.length; v++) {
                variables.setProperty(variableElements[v].getName(), variableElements[v].getValue());
            }
        }
        return variables;
    }

    public static Configuration createSitemapConfiguration(Configuration config)
    throws ConfigurationException {
        Configuration componentConfig = config.getChild("components", false);
        Configuration classPathConfig = null;

        // by default we include configuration files and properties from
        // predefined locations
        final boolean useDefaultIncludes = isUsingDefaultIncludes(config);

        // if we want to add the default includes and have no component section
        // we have to create one!
        if ( componentConfig == null && useDefaultIncludes ) {
            componentConfig = new DefaultConfiguration("components",
                                                       config.getLocation(),
                                                       config.getNamespace(),
                                                       "");
        }

        if ( componentConfig != null ) {
            // before we pass the configuration we have to strip the
            // additional configuration parts, like classpath as these
            // are not configurations for the component container
            final DefaultConfiguration c = new DefaultConfiguration(componentConfig.getName(), 
                                                                    componentConfig.getLocation(),
                                                                    componentConfig.getNamespace(),
                                                                    "");
            c.addAll(componentConfig);
            classPathConfig = c.getChild(CLASSLOADER_CONFIG_NAME, false);
            if ( classPathConfig != null ) {
                c.removeChild(classPathConfig);
            }
            // and now add default includes
            if ( useDefaultIncludes ) {
                DefaultConfiguration includeElement;
                includeElement = new DefaultConfiguration("include", 
                                                          c.getLocation(),
                                                          c.getNamespace(),
                                                          "");
                includeElement.setAttribute("dir", DEFAULT_CONFIG_XCONF);
                includeElement.setAttribute("pattern", "*.xconf");
                includeElement.setAttribute("optional", "true");
                c.addChild(includeElement);
            }
            componentConfig = c;
        }
        return componentConfig;
    }

    /**
     * Create the per sitemap container.
     *
     * @param config          The sitemap as a configuration object.
     * @param sitemapLocation The uri of the sitemap
     * @param fam
     * @param servletContext  The servlet context
     * @return
     * @throws Exception
     */
    public static WebApplicationContext createContainer(Configuration  config,
                                                        String         sitemapLocation,
                                                        Monitor        fam,
                                                        ServletContext servletContext)
    throws Exception {
        // let's get the root container first
        final WebApplicationContext rootContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        final ProcessInfoProvider infoProvider = (ProcessInfoProvider) rootContext.getBean(ProcessInfoProvider.ROLE);
        final Request request = ObjectModelHelper.getRequest(infoProvider.getObjectModel());
        // let's determine our context url
        int pos = sitemapLocation.lastIndexOf('/');
        if ( sitemapLocation.lastIndexOf(File.separatorChar) > pos ) {
            pos = sitemapLocation.lastIndexOf(File.separatorChar);
        }
        final String contextUrl = sitemapLocation.substring(0, pos + 1);

        final WebApplicationContext parentContext = WebAppContextUtils.getCurrentWebApplicationContext();

        // get classloader
//      TODO rcl            
//        final ClassLoader classloader = createClassLoader(parentContext, config, fam, servletContext, sitemapResolver);
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        
        // create root bean definition
        final String definition = createDefinition(request.getSitemapURIPrefix(),
                                                   sitemapLocation.substring(pos+1),
                                                   ((Settings)parentContext.getBean(Settings.ROLE)).getRunningMode(),
                                                   isUsingDefaultIncludes(config),
                                                   getBeanIncludes(config),
                                                   getPropertiesIncludes(config),
                                                   getGlobalSitemapVariables(config));
        PARENT_CONTEXT.set(parentContext);
        try {
            final ChildXmlWebApplicationContext context = new ChildXmlWebApplicationContext(contextUrl,
                                                                                        definition);
            context.setServletContext(servletContext);
            context.setParent(parentContext);
            if ( classloader != null ) {
                context.setClassLoader(classloader);
            }
            context.refresh();
            return context;
        } finally {
            PARENT_CONTEXT.set(null);
        }
    }

// TODO rcl    
//    /**
//     * Build a processing tree from a <code>Configuration</code>.
//     * @param processor 
//     */
//    protected static ClassLoader createClassLoader(BeanFactory    parentFactory,
//                                                   Configuration  config,
//                                                   Monitor fam,
//                                                   ServletContext servletContext,
//                                                   SourceResolver sitemapResolver)
//    throws Exception {
//        final Configuration componentConfig = config.getChild("components", false);
//        Configuration classPathConfig = null;
//
//        if ( componentConfig != null ) {
//            classPathConfig = componentConfig.getChild(CLASSLOADER_CONFIG_NAME, false);
//        }
//        // Create class loader
//        // we don't create a new class loader if there is no new configuration
//        if ( classPathConfig == null ) {
//            return Thread.currentThread().getContextClassLoader();            
//        }
//        final String factoryRole = classPathConfig.getAttribute("factory-role", ClassLoaderFactory.ROLE);
//
//        // Create a new classloader
//        ReloadingClassLoaderConfiguration configBean = AvalonUtils.createConfiguration(sitemapResolver, classPathConfig);
//        configBean.setMonitor(fam);
//        ClassLoaderFactory clFactory = (ClassLoaderFactory)parentFactory.getBean(factoryRole);
//        return clFactory.createClassLoader(Thread.currentThread().getContextClassLoader(),
//                                           configBean,
//                                           servletContext);
//    }

    protected static final class IncludeInfo {
        public final String src;
        public final String dir;
        public final String pattern;
        public final boolean optional;

        public IncludeInfo(String s, String d, String p, boolean o) {
            this.src = s;
            this.dir = d;
            this.pattern = p;
            this.optional = o;
        }
    }
}
