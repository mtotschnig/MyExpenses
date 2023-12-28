<?xml version='1.0' ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:my="http://myexpenses.mobi/"
    version="2.0">
    <xsl:output encoding="UTF-8" method="xml" />
    <xsl:include href="helpers_v1.xsl" />
    <xsl:include href="helpers.xsl" />
    <xsl:param name="version" />
    <xsl:param name="languages" select="$all-languages" />

    <xsl:template name="main" match="/">
        <xsl:for-each select="tokenize($languages, ' ')">
            <xsl:call-template name="extract">
                <xsl:with-param name="lang" select="." />
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="extract">
        <xsl:param name="lang" />
        <xsl:variable name="dir">
            <xsl:call-template name="values-dir">
                <xsl:with-param name="lang" select="$lang" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="strings">
            <xsl:value-of select="$dir" />
            <xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="aosp">
            <xsl:value-of select="$dir" />
            <xsl:text>/aosp.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="help">
            <xsl:value-of select="$dir" />
            <xsl:text>/help.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="upgrade">
            <xsl:value-of select="$dir" />
            <xsl:text>/whats_new.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="changelog">
            <xsl:for-each select="tokenize($version, ' ')">
                <xsl:variable name="entry">
                    <xsl:variable name="special-version-info">
                        <xsl:call-template name="special-version-info">
                            <xsl:with-param name="version" select="." />
                            <xsl:with-param name="strings" select="$strings" />
                            <xsl:with-param name="aosp" select="$aosp" />
                            <xsl:with-param name="help" select="$help" />
                            <xsl:with-param name="upgrade" select="$upgrade" />
                            <xsl:with-param name="lang" select="$lang" />
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:choose>
                        <xsl:when test="$special-version-info != ''">
                            <xsl:value-of select="$special-version-info" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:for-each select="document($upgrade)/resources/string[starts-with(@name,my:changeLogResourceName($version))]">
                                <xsl-text>•&#032;</xsl-text>
                                <xsl:apply-templates mode="unescape" select="." />
                                <xsl:if test="position() != last()">
                                    <xsl:value-of select="$newline" />
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:if test="$entry != ''">
                    <xsl:value-of select="$entry" />
                    <xsl:if test="position() != last()">
                        <xsl:value-of select="$newline" />
                    </xsl:if>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <xsl:if test="$changelog != ''">
            <xsl:variable name="element-name">
                <xsl:call-template name="lang-play">
                    <xsl:with-param name="lang" select="$lang" />
                </xsl:call-template>
            </xsl:variable>
            <xsl:element name="{$element-name}">
                <xsl:value-of select="$newline" />
                <xsl:value-of select="$changelog" />
                <xsl:value-of select="$newline" />
            </xsl:element>
            <xsl:value-of select="$newline" />
        </xsl:if>
    </xsl:template>

    <xsl:template match="string">
        <xsl:param name="version" />
        <xsl:if test="starts-with(@name,my:changeLogResourceName($version))">
            <xsl-text>•&#032;</xsl-text>
            <xsl:value-of select="position()" />
            <xsl:apply-templates mode="unescape" select="." />
            <xsl:if test="position() != last()">
                <xsl:value-of select="$newline" />
            </xsl:if>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
