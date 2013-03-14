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
package org.apache.cocoon.woody.formmodel;

/**
 * A {@link WidgetDefinition} which holds a collection of {@link Widget}s
 * and which can be instantiated with a {@link NewDefinition}.
 *
 * @author Timothy Larson
 * @version $Id: ClassDefinition.java,v 1.5 2004/03/09 13:53:56 reinhard Exp $
 */
public class ClassDefinition extends AbstractContainerDefinition {
    public Widget createInstance() {
        return null;
    }
}