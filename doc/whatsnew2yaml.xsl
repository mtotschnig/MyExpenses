<?xml version='1.0' ?>
<xsl:stylesheet xmlns:str="http://exslt.org/strings"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" extension-element-prefixes="str" version="1.0">
    <xsl:output encoding="UTF-8" method="text" />
    <xsl:include href="helpers.xsl" />
    <xsl:param name="version" />
    <xsl:param name="version_date" select='"2014-xx-xx"' />
    <xsl:param name="languages" select="$all-languages" />
    <xsl:param name="appendDot" select="false" />

    <xsl:template match="/">
        <xsl:value-of select="str:padding(2, '&#032;')"/>
        <xsl:text>-</xsl:text>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')"/>
        <xsl:text>-&#032;</xsl:text>
        <xsl:value-of select="$version" />
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')"/>
        <xsl:text>-&#032;"</xsl:text>
        <xsl:value-of select="$version_date" />
        <xsl:text>"</xsl:text>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')"/>
        <xsl:text>-</xsl:text>
        <xsl:for-each select="str:tokenize($languages)">
            <xsl:call-template name="extract">
                <xsl:with-param name="lang" select="." />
            </xsl:call-template>
        </xsl:for-each>
        <xsl:value-of select="$newline" />
    </xsl:template>

    <xsl:template name="extract">
        <xsl:param name="lang" />
        <xsl:variable name="version_short" select="str:replace($version,'.','')" />
        <xsl:variable name="dir">
            <xsl:call-template name="values-dir">
                <xsl:with-param name="lang" select="$lang" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="upgrade">
            <xsl:value-of select="$dir" />
            <xsl:text>/upgrade.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="strings">
            <xsl:value-of select="$dir" />
            <xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="aosp">
            <xsl:value-of select="$dir" />
            <xsl:text>/aosp.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="changelog">
            <xsl:for-each select="str:tokenize($version)">
                <xsl:variable name="special-version-info">
                    <xsl:call-template name="special-version-info">
                        <xsl:with-param name="version" select="." />
                        <xsl:with-param name="strings" select="$strings" />
                        <xsl:with-param name="aosp" select="$aosp" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$special-version-info != ''">
                        <xsl:value-of select="$special-version-info" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates
                            select="document($upgrade)/resources/string-array[@name=concat('whats_new_',$version_short)]" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:variable>
        <xsl:if test="$changelog != ''">
            <xsl:value-of select="$newline" />
            <xsl:value-of select="str:padding(6, '&#032;')"/>
            <xsl:value-of select="$lang" />
            <xsl:text>: |</xsl:text>
            <xsl:value-of select="$newline" />
            <xsl:value-of select="str:padding(8, '&#032;')"/>
            <xsl:value-of select="$changelog" />
        </xsl:if>
    </xsl:template>

    <xsl:template match="string-array">
        <xsl:for-each select="item">
            <xsl:apply-templates mode="unescape" select='.' />
            <xsl:if test="$appendDot"><xsl:text>.</xsl:text></xsl:if>
            <xsl:if test="position() != last()">
                <xsl:text>&#032;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
