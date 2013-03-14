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
package org.apache.cocoon.woody.datatype.validationruleimpl;

import org.apache.cocoon.woody.datatype.ValidationError;
import org.apache.cocoon.woody.formmodel.CannotYetResolveWarning;
import org.apache.cocoon.woody.util.I18nMessage;
import org.apache.cocoon.woody.Constants;
import org.outerj.expression.ExpressionContext;
import org.outerj.expression.Expression;

import java.math.BigDecimal;

/**
 * Checks the length of a String.
 *
 * <p>This validation rule can perform 4 different checks:
 * <ul>
 *  <li>check exact string length
 *  <li>check minimum string length
 *  <li>check maximum string length
 *  <li>check min and max string length
 * </ul>
 * 
 * @version $Id: LengthValidationRule.java,v 1.4 2004/02/11 09:53:44 antonio Exp $
 */
public class LengthValidationRule extends AbstractValidationRule {
    private Expression exactExpr;
    private Expression minExpr;
    private Expression maxExpr;

    public void setExactExpr(Expression exactExpr) {
        this.exactExpr = exactExpr;
    }

    public void setMinExpr(Expression minExpr) {
        this.minExpr = minExpr;
    }

    public void setMaxExpr(Expression maxExpr) {
        this.maxExpr = maxExpr;
    }

    public ValidationError validate(Object value, ExpressionContext expressionContext) {
        String string = (String)value;

        if (exactExpr != null) {
            Object result = evaluateNumeric(exactExpr, expressionContext, "exact", "length");
            if (result instanceof ValidationError)
                return (ValidationError)result;
            else if (result instanceof CannotYetResolveWarning)
                return null;
            int length = ((BigDecimal)result).intValue();
            if (string.length() != length)
                return hasFailMessage() ? getFailMessage() : new ValidationError(new I18nMessage("validation.string.exact-length", new String[] {String.valueOf(length)}, Constants.I18N_CATALOGUE));
            return null;
        } else if (minExpr != null && maxExpr != null) {
            Object result = evaluateNumeric(minExpr, expressionContext, "min", "length");
            if (result instanceof ValidationError)
                return (ValidationError)result;
            else if (result instanceof CannotYetResolveWarning)
                return null;
            int minLength = ((BigDecimal)result).intValue();

            result = evaluateNumeric(maxExpr, expressionContext, "max", "length");
            if (result instanceof ValidationError)
                return (ValidationError)result;
            else if (result instanceof CannotYetResolveWarning)
                return null;
            int maxLength = ((BigDecimal)result).intValue();

            if (string.length() < minLength || string.length() > maxLength)
                return hasFailMessage() ? getFailMessage() : new ValidationError(new I18nMessage("validation.string.range-length", new String[] {String.valueOf(minLength), String.valueOf(maxLength)}, Constants.I18N_CATALOGUE));
            return null;
        } else if (minExpr != null) {
            Object result = evaluateNumeric(minExpr, expressionContext, "min", "length");
            if (result instanceof ValidationError)
                return (ValidationError)result;
            else if (result instanceof CannotYetResolveWarning)
                return null;
            int length = ((BigDecimal)result).intValue();
            if (string.length() < length)
                return hasFailMessage() ? getFailMessage() : new ValidationError(new I18nMessage("validation.string.min-length", new String[] {String.valueOf(length)}, Constants.I18N_CATALOGUE));
            return null;
        } else if (maxExpr != null) {
            Object result = evaluateNumeric(maxExpr, expressionContext, "max", "length");
            if (result instanceof ValidationError)
                return (ValidationError)result;
            else if (result instanceof CannotYetResolveWarning)
                return null;
            int length = ((BigDecimal)result).intValue();
            if (string.length() > length)
                return hasFailMessage() ? getFailMessage() : new ValidationError(new I18nMessage("validation.string.max-length", new String[] {String.valueOf(length)}, Constants.I18N_CATALOGUE));
            return null;
        }
        return null;
    }

    public boolean supportsType(Class clazz, boolean arrayType) {
        return clazz.isAssignableFrom(String.class) && !arrayType;
    }
}