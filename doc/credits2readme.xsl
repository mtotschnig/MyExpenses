<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output encoding="UTF-8" method="text"/>
    <xsl:template match="credits"><xsl:apply-templates select="project" /></xsl:template>
    <xsl:template match="project">
        <xsl:text>- [</xsl:text>
        <xsl:value-of select="name" />
        <xsl:text>](</xsl:text>
        <xsl:value-of select="url" />
        <xsl:text>)</xsl:text>
        <xsl:if test="extra_info"> (<xsl:value-of select="extra_info" />)</xsl:if>
        <xsl:text>
</xsl:text>
</xsl:template>
</xsl:stylesheet>