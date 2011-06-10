/*
 *  JBoss, Home of Professional Open Source Copyright 2010, Red Hat, Inc., and
 *  individual contributors by the @authors tag. See the copyright.txt in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This software is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this software; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 *  site: http://www.fsf.org.
 */
package org.jboss.logging.generator.validation.validator;

import org.jboss.logging.generator.Annotations;
import org.jboss.logging.generator.ToolLogger;
import org.jboss.logging.generator.validation.ValidationErrorMessage;
import org.jboss.logging.generator.validation.ValidationMessage;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jboss.logging.generator.LoggingTools.annotations;
import static org.jboss.logging.generator.util.ElementHelper.findByName;
import static org.jboss.logging.generator.util.ElementHelper.findMessage;
import static org.jboss.logging.generator.util.ElementHelper.findMessageFormat;
import static org.jboss.logging.generator.util.ElementHelper.parameterCount;

/**
 * Checks to make sure that only one {@link org.jboss.logging.generator.Annotations#message()}
 * annotation is present on like named methods.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class MessageAnnotationValidator extends AbstractValidator {

    private static final String ERROR_MESSAGE = "Only one method with the same name and parameter count is allowed to be annotated the %s annotation.";

    private final List<ValidationMessage> messages = new LinkedList<ValidationMessage>();

    public MessageAnnotationValidator(final Types typeUtil) {
        super(typeUtil);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ValidationMessage> validate(final TypeElement element, final Collection<ExecutableElement> elementMethods) {
         // Clear any previous messages.
         messages.clear();
        // Set for the method names that have been processed
        final Set<String> methodNames = new HashSet<String>();
        for (ExecutableElement method : elementMethods) {
            // The name should be the method name, plus the number of parameters
            final int paramCount = parameterCount(method.getParameters());
            final String name = method.getSimpleName().toString() + paramCount;
            // Only adds methods which have not been processed
            if (methodNames.add(name)) {
                // Find all like named methods
                final Collection<ExecutableElement> likeMethods = findByName(elementMethods, method.getSimpleName(), paramCount);
                boolean foundFirst = false;
                for (ExecutableElement m : likeMethods) {
                    boolean found = m.getAnnotation(annotations().message()) != null;
                    if (foundFirst && found) {
                        messages.add(ValidationErrorMessage.of(m, ERROR_MESSAGE, annotations().message().getName()));
                    }
                    foundFirst = found;
                }
                validateMessage(elementMethods, method);
            }
        }

        return messages;
    }

    private void validateMessage(final Collection<ExecutableElement> methods, final ExecutableElement method) {
        final String msg = findMessage(methods, method);
        final Annotations.FormatType format = findMessageFormat(methods, method);
        if (msg != null) {
            final FormatValidator formatValidator = (format == Annotations.FormatType.MESSAGE_FORMAT ? MessageFormatValidator
                    .of(msg) : StringFormatValidator.of(msg));
            if (!formatValidator.isValid()) {
                messages.add(ValidationErrorMessage.of(method, formatValidator.summaryMessage()));
            }
            final int paramCount = parameterCount(method.getParameters());
            // Check the parameter count
            if (paramCount != formatValidator.argumentCount()) {
                messages.add(ValidationErrorMessage.of(method, "Parameter count does not match for format '%s'. Required: %d Provided: %d", msg, formatValidator.argumentCount(), paramCount));
            }
        }
    }
}
