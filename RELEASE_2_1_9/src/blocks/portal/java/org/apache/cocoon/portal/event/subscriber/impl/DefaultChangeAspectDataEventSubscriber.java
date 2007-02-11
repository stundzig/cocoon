/*
 * Copyright 1999-2002,2004 The Apache Software Foundation.
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
package org.apache.cocoon.portal.event.subscriber.impl;

import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.aspect.Aspectalizable;
import org.apache.cocoon.portal.event.Receiver;
import org.apache.cocoon.portal.event.impl.ChangeAspectDataEvent;

/**
 *
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * 
 * @version CVS $Id$
 */
public final class DefaultChangeAspectDataEventSubscriber 
    implements Receiver {

    public DefaultChangeAspectDataEventSubscriber() {
        // nothing to do 
    }

    /**
     * @see Receiver
     */
    public void inform(ChangeAspectDataEvent event, PortalService service) {
        final Aspectalizable target = event.getAspectalizable();
        target.setAspectData(event.getAspectName(), event.getData());
    }

}
