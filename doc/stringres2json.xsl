<?xml version='1.0' ?>
<xsl:stylesheet
    extension-element-prefixes="str"
    version="1.0" xmlns:str="http://exslt.org/strings"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
    <xsl:output encoding="UTF-8" method="text"/>
    <xsl:param name="key"/>
    <xsl:param name="langs"
               select="'en ar bg ca cs da de el es eu fr hr hu it iw ja km ko ms pl pt pt-rPT ro ru si sk tr vi zh-rCN zh-rTW'"/>


    <xsl:template match="/">
        <xsl:text>{
        </xsl:text>
        <xsl:for-each select="str:tokenize($langs)">
            <xsl:call-template name="extract">
                <xsl:with-param name="lang" select="."/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:text>
        }</xsl:text>
    </xsl:template>

    <xsl:template name="extract">
        <xsl:param name="lang"/>
        <xsl:variable name="dir">
            <xsl:text>../myExpenses/src/main/res/values</xsl:text>
            <xsl:choose>
                <xsl:when test="$lang='en'"/>
                <xsl:otherwise>-<xsl:value-of select="$lang"/></xsl:otherwise>
            </xsl:choose>
            <xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:if test="document($dir)/resources/string[@name=$key]">
            <xsl:text>
            "</xsl:text>
            <xsl:value-of select="$lang"/>
            <xsl:text>": "</xsl:text>
            <xsl:apply-templates select="document($dir)/resources/string[@name=$key]"/>
            <xsl:text>",</xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template match="string">
        <xsl:variable name="apos">'</xsl:variable>
        <xsl:variable name="quote">"</xsl:variable>
        <xsl:value-of
            select="str:replace(.,concat('\',$apos),$apos)"/>
    </xsl:template>

</xsl:stylesheet>
