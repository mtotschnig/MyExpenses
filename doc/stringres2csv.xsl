<?xml version='1.0' ?>
<xsl:stylesheet
    extension-element-prefixes="str"
    version="1.0" xmlns:str="http://exslt.org/strings"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
    <xsl:output encoding="UTF-8" method="text"/>
    <xsl:param name="lang"/>


    <xsl:template match="resources">
        <xsl:apply-templates select="string"><xsl:sort select="."/></xsl:apply-templates>
    </xsl:template>

    <xsl:template match="string">
        <xsl:variable name="key" select="@name"/>
        <xsl:variable name="dir">
            <xsl:text>../myExpenses/src/main/res/values-</xsl:text><xsl:value-of select="$lang"/><xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:call-template name="quote">
            <xsl:with-param name="string" select="."/>
        </xsl:call-template>
        <xsl:text>;</xsl:text>
        <xsl:if test="document($dir)/resources/string[@name=$key]">
            <xsl:call-template name="quote">
                <xsl:with-param name="string" select="document($dir)/resources/string[@name=$key]"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template name="quote">
        <xsl:param name="string"/>
        <xsl:variable name="apos">'</xsl:variable>
        <xsl:variable name="quote">"</xsl:variable>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="str:replace($string,concat('\',$apos),$apos)"/><xsl:text>"</xsl:text>
    </xsl:template>

</xsl:stylesheet>
