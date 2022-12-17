<?xml version='1.0' ?>
<xsl:stylesheet xmlns:my="http://myexpenses.mobi/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">

    <xsl:template match="item|string" mode="unescape">
        <xsl:variable name="apostrophe">'</xsl:variable>
        <xsl:variable name="quote">"</xsl:variable>
        <xsl:variable name="trim">
            <xsl:choose>
                <xsl:when test="starts-with(., $quote)">
                    <xsl:value-of select="substring-before(substring-after(., $quote), $quote) " />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="." />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of
            select="replace(replace($trim,concat('\\',$apostrophe),$apostrophe),concat('\\',$quote),$quote)" />
    </xsl:template>

    <xsl:function name="my:changeLogResourceName">
        <xsl:param name="version" />
        <xsl:value-of select="concat('whats_new_',replace($version,'\.',''))" />
    </xsl:function>
    <xsl:function name="my:githubBoardResourceName">
        <xsl:param name="version" />
        <xsl:value-of select="concat('project_board_',replace($version,'\.',''))" />
    </xsl:function>
    <xsl:function name="my:simpleFormatRes">
        <xsl:param name="format" />
        <xsl:param name="arg" />
        <xsl:value-of select="replace($format,'%s',$arg)" />
    </xsl:function>
</xsl:stylesheet>
