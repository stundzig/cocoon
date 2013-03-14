
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

package org.apache.cocoon.components.elementprocessor.impl.poi.hssf.elements;

import org.apache.cocoon.components.elementprocessor.types.NumericConverter;
import org.apache.cocoon.components.elementprocessor.types.Validator;

import java.io.IOException;

/**
 * Vertical alignment is written as an integer, and each bit in the
 * integer specifies a particular boolean attribute. This class deals
 * with all that information in an easily digested form.
 *
 * @author Marc Johnson (marc_johnson27591@hotmail.com)
 * @version CVS $Id: VerticalAlignment.java,v 1.2 2003/03/11 19:05:02 vgritsenko Exp $
 */
public class VerticalAlignment
{
    private int                    _alignment;
    private static final int       _top       = 1;
    private static final int       _bottom    = 2;
    private static final int       _center    = 4;
    private static final int       _justify   = 8;
    private static final Validator _validator = new Validator()
    {
        public IOException validate(final Number number)
        {
            int value = number.intValue();

            return ((value >= 0) && (value <= 15)) ? null
                                                   : new IOException(
                                                       "\"" + number
                                                       + "\" is out of range");
        }
    };

    /**
     * Create a VerticalAlignment object
     *
     * @param value the string containing the vertical alignment data
     *
     * @exception IOException if the data is malformed
     */

    public VerticalAlignment(final String value)
        throws IOException
    {
        _alignment = NumericConverter.extractInteger(value,
                _validator).intValue();
    }

    /**
     * @return true if top bit is set
     */

    public boolean isTop()
    {
        return (_alignment & _top) == _top;
    }

    /**
     * @return true if bottom bit is set
     */

    public boolean isBottom()
    {
        return (_alignment & _bottom) == _bottom;
    }

    /**
     * @return true if center bit is set
     */

    public boolean isCenter()
    {
        return (_alignment & _center) == _center;
    }

    /**
     * @return true if justify bit is set
     */

    public boolean isJustify()
    {
        return (_alignment & _justify) == _justify;
    }
    
    public short getCode() {
        return (short)_alignment;
    }
}   // end public class VerticalAlignment