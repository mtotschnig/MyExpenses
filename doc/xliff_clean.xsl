<?xml version='1.0' ?>
<xsl:stylesheet
    version="2.0"
    xmlns:xliff="xliff"
    exclude-result-prefixes="xliff"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
    <xsl:output encoding="UTF-8" method="xml"/>

    <xsl:template match="plurals">
        <plurals>
            <xsl:copy-of select="@name" />
            <xsl:apply-templates />
        </plurals>
    </xsl:template>


    <xsl:template match="item">
        <item>
            <xsl:copy-of select="@quantity" />
            <xsl:apply-templates />
        </item>
    </xsl:template>

    <xsl:template match="xliff:g">
        <xsl:value-of select="."/>
    </xsl:template>

</xsl:stylesheet>
