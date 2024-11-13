<?xml version='1.0' ?>
<xsl:stylesheet xmlns:str="http://exslt.org/strings"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" extension-element-prefixes="str" version="1.0">
    <xsl:output encoding="UTF-8" method="text" />
    <xsl:include href="helpers_v1.xsl" />
    <xsl:param name="languages" select="$all-languages" />

    <xsl:template match="/">
        <xsl:for-each select="str:tokenize($languages)">
            <xsl:call-template name="pro">
                <xsl:with-param name="lang" select="." />
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="pro">
        <xsl:param name="lang" />
        <xsl:variable name="app_file">
            <xsl:text>../myExpenses/src/main/res/values</xsl:text>
            <xsl:call-template name="lang-file">
                <xsl:with-param name="lang" select="$lang" />
            </xsl:call-template>
            <xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="addon_file">
            <xsl:text>../business/ProductCatalog/values</xsl:text>
            <xsl:call-template name="lang-file">
                <xsl:with-param name="lang" select="$lang" />
            </xsl:call-template>
            <xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:text>SubscriptionListing().setLanguageCode("</xsl:text>
        <xsl:call-template name="lang-play">
            <xsl:with-param name="lang" select="$lang" />
        </xsl:call-template>
        <xsl:text>").setTitle("</xsl:text>
        <xsl:if test="count(document($app_file)/resources/string[@name='professional_key']) = 0">
            <xsl:message terminate="yes">Missing key professional_key for lang
                <xsl:value-of select="$lang" />
            </xsl:message>
        </xsl:if>
        <xsl:apply-templates mode="unescape"
            select="document($app_file)/resources/string[@name='professional_key']" />
        <xsl:text>")</xsl:text>
        <xsl:if
            test="count(document($addon_file)/resources/string[starts-with(@name,'professional_benefit_')]) > 0">
            <xsl:text>.setBenefits(listOf(</xsl:text>
            <xsl:for-each
                select="document($addon_file)/resources/string[starts-with(@name,'professional_benefit_')]">
                <xsl:text>"</xsl:text>
                <xsl:if test="string-length(.) > 40">
                    <xsl:message terminate="yes">Benefit for lang
                        <xsl:value-of select="$lang" />
                        must not exceed 40 characters.
                    </xsl:message>
                </xsl:if>
                <xsl:apply-templates mode="unescape" select="." />
                <xsl:text>"</xsl:text>
                <xsl:if test="position() != last()">
                    <xsl:text>,</xsl:text>
                </xsl:if>
            </xsl:for-each>
            <xsl:text>))</xsl:text>
        </xsl:if>
        <xsl:if test="position() != last()">
            <xsl:text>,
</xsl:text>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
