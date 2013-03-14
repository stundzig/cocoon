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
package org.apache.cocoon.forms.formmodel;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A WidgetDefinition holds all the static information about a Widget. It's
 * function is a lot like that of the class in Java. Users of the Cocoon Forms framework
 * usually won't have to bother with the WidgetDefinition's, but will rather use
 * the Widget's themselves.
 * 
 * @version CVS $Id: WidgetDefinition.java,v 1.5 2004/04/29 08:46:19 cziegeler Exp $
 */
public interface WidgetDefinition {

    /**
     * Gets the {@link FormDefinition}.
     */
    public FormDefinition getFormDefinition();

    /**
     * Sets the parent of this definition
     */
    public void setParent(WidgetDefinition definition);

    /**
     * Gets source location of this widget definition.
     */
    public String getLocation();

    /**
     * Gets id of this widget definition.
     */
    public String getId();
    
    /**
     * Validate a widget using the validators that were defined in its definition. If validation
     * fails, the validator has set a validation error on the widget or one of its children.
     * 
     * @param widget the widget
     * @return <code>true</code> if validation was successful.
     */
    public boolean validate(Widget widget);
    

    /**
     * Creates and returns a widget based on this widget definition.
     */
    public Widget createInstance();

    /**
     * Generates SAX events for named display data.
     */
    public void generateDisplayData(String name, ContentHandler contentHandler) throws SAXException;

    /**
     * Generates SAX events for display data.
     */
    public void generateDisplayData(ContentHandler contentHandler) throws SAXException;

    /**
     * Generates SAX events for the label of this widget.
     */
    public void generateLabel(ContentHandler contentHandler) throws SAXException;
}