/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.bind.v2.util;

import com.sun.xml.bind.v2.Messages;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Provides helper methods for creating properly configured XML parser
 * factory instances with namespace support turned on and configured for
 * security.
 * @author snajper
 *
 *  *  * Change for running on Android:
 *  * Replaced SAXParserFactory.newInstance(); which loads Android's internal SAXParserFactoryImpl by
 *  loading org.apache.xerces.jaxp.SAXParserFactoryImpl
 */
public class XmlFactory {

    // not in older JDK, so must be duplicated here, otherwise javax.xml.XMLConstants should be used
    public static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";
    public static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";

    private static final Logger LOGGER = Logger.getLogger(XmlFactory.class.getName());

    /**
     * If true XML security features when parsing XML documents will be disabled.
     * The default value is false.
     *
     * Boolean
     * @since 2.2.6
     */
    private static final String DISABLE_XML_SECURITY  = "com.sun.xml.bind.disableXmlSecurity";

    private static final boolean XML_SECURITY_DISABLED = AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return Boolean.getBoolean(DISABLE_XML_SECURITY);
                }
            }
    );

    private static boolean isXMLSecurityDisabled(boolean runtimeSetting) {
        return XML_SECURITY_DISABLED || runtimeSetting;
    }

    /**
     * Returns properly configured (e.g. security features) schema factory
     * - namespaceAware == true
     * - securityProcessing == is set based on security processing property, default is true
     */
    public static SchemaFactory createSchemaFactory(final String language, boolean disableSecureProcessing) throws IllegalStateException {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(language);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SchemaFactory instance: {0}", factory);
            }
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, !isXMLSecurityDisabled(disableSecureProcessing));
            return factory;
        } catch (SAXNotRecognizedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        } catch (SAXNotSupportedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        } catch (AbstractMethodError er) {
            LOGGER.log(Level.SEVERE, null, er);
            throw new IllegalStateException(Messages.INVALID_JAXP_IMPLEMENTATION.format(), er);
        }
    }

    /**
     * Returns properly configured (e.g. security features) parser factory
     * - namespaceAware == true
     * - securityProcessing == is set based on security processing property, default is true
     */
    public static SAXParserFactory createParserFactory(boolean disableSecureProcessing) throws IllegalStateException {
        try {
            SAXParserFactory factory = new SAXParserFactoryImpl();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SAXParserFactory instance: {0}", factory);
            }
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, !isXMLSecurityDisabled(disableSecureProcessing));
            return factory;
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException( ex);
        } catch (SAXNotRecognizedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException( ex);
        } catch (SAXNotSupportedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException( ex);
        } catch (AbstractMethodError er) {
            LOGGER.log(Level.SEVERE, null, er);
            throw new IllegalStateException(Messages.INVALID_JAXP_IMPLEMENTATION.format(), er);
        }
    }

    /**
     * Returns properly configured (e.g. security features) factory
     * - securityProcessing == is set based on security processing property, default is true
     */
    public static XPathFactory createXPathFactory(boolean disableSecureProcessing) throws IllegalStateException {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "XPathFactory instance: {0}", factory);
            }
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, !isXMLSecurityDisabled(disableSecureProcessing));
            return factory;
        } catch (XPathFactoryConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException( ex);
        } catch (AbstractMethodError er) {
            LOGGER.log(Level.SEVERE, null, er);
            throw new IllegalStateException(Messages.INVALID_JAXP_IMPLEMENTATION.format(), er);
        }
    }

    /**
     * Returns properly configured (e.g. security features) factory
     * - securityProcessing == is set based on security processing property, default is true
     */
    public static TransformerFactory createTransformerFactory(boolean disableSecureProcessing) throws IllegalStateException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "TransformerFactory instance: {0}", factory);
            }
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, !isXMLSecurityDisabled(disableSecureProcessing));
            return factory;
        } catch (TransformerConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException( ex);
        } catch (AbstractMethodError er) {
            LOGGER.log(Level.SEVERE, null, er);
            throw new IllegalStateException(Messages.INVALID_JAXP_IMPLEMENTATION.format(), er);
        }
    }

    /**
     * Returns properly configured (e.g. security features) factory
     * - namespaceAware == true
     * - securityProcessing == is set based on security processing property, default is true
     */
    public static DocumentBuilderFactory createDocumentBuilderFactory(boolean disableSecureProcessing) throws IllegalStateException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "DocumentBuilderFactory instance: {0}", factory);
            }
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, !isXMLSecurityDisabled(disableSecureProcessing));
            return factory;
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new IllegalStateException( ex);
        } catch (AbstractMethodError er) {
            LOGGER.log(Level.SEVERE, null, er);
            throw new IllegalStateException(Messages.INVALID_JAXP_IMPLEMENTATION.format(), er);
        }
    }

    public static SchemaFactory allowExternalAccess(SchemaFactory sf, String value, boolean disableSecureProcessing) {

        // if xml security (feature secure processing) disabled, nothing to do, no restrictions applied
        if (isXMLSecurityDisabled(disableSecureProcessing)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, Messages.JAXP_XML_SECURITY_DISABLED.format());
            }
            return sf;
        }

        if (System.getProperty("javax.xml.accessExternalSchema") != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, Messages.JAXP_EXTERNAL_ACCESS_CONFIGURED.format());
            }
            return sf;
        }

        try {
            sf.setProperty(ACCESS_EXTERNAL_SCHEMA, value);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, Messages.JAXP_SUPPORTED_PROPERTY.format(ACCESS_EXTERNAL_SCHEMA));
            }
        } catch (SAXException ignored) {
            // nothing to do; support depends on version JDK or SAX implementation
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.log(Level.CONFIG, Messages.JAXP_UNSUPPORTED_PROPERTY.format(ACCESS_EXTERNAL_SCHEMA), ignored);
            }
        }
        return sf;
    }

    public static SchemaFactory allowExternalDTDAccess(SchemaFactory sf, String value, boolean disableSecureProcessing) {

        // if xml security (feature secure processing) disabled, nothing to do, no restrictions applied
        if (isXMLSecurityDisabled(disableSecureProcessing)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, Messages.JAXP_XML_SECURITY_DISABLED.format());
            }
            return sf;
        }

        if (System.getProperty("javax.xml.accessExternalDTD") != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, Messages.JAXP_EXTERNAL_ACCESS_CONFIGURED.format());
            }
            return sf;
        }

        try {
            sf.setProperty(ACCESS_EXTERNAL_DTD, value);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, Messages.JAXP_SUPPORTED_PROPERTY.format(ACCESS_EXTERNAL_DTD));
            }
        } catch (SAXException ignored) {
            // nothing to do; support depends on version JDK or SAX implementation
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.log(Level.CONFIG, Messages.JAXP_UNSUPPORTED_PROPERTY.format(ACCESS_EXTERNAL_DTD), ignored);
            }
        }
        return sf;
    }

}
