<?xml version='1.0' ?>
<xsl:stylesheet xmlns:str="http://exslt.org/strings"
    xmlns:my="http://myexpenses.mobi/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output encoding="UTF-8" method="text" />
    <xsl:include href="helpers_v1.xsl" />
    <xsl:include href="helpers.xsl" />
    <xsl:param name="version" />
    <xsl:param name="versionDate"  />
    <xsl:param name="languages" select="$all-languages" />
    <xsl:param name="appendDot" select="false" />

    <xsl:template match="/" name="main">
        <xsl:value-of select="str:padding(2, '&#032;')" />
        <xsl:text>-</xsl:text>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')" />
        <xsl:text>-&#032;</xsl:text>
        <xsl:value-of select="$version" />
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')" />
        <xsl:text>-&#032;"</xsl:text>
        <xsl:value-of select="$versionDate" />
        <xsl:text>"</xsl:text>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')" />
        <xsl:text>-</xsl:text>
        <xsl:for-each select="tokenize($languages)">
            <xsl:call-template name="extract">
                <xsl:with-param name="lang" select="." />
            </xsl:call-template>
        </xsl:for-each>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(4, '&#032;')" />
        <xsl:text>-</xsl:text>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(6, '&#032;')" />
        <xsl:variable name="versionInfo"
            select="'../myExpenses/src/main/res/values/version_info.xml'" />
        <xsl:text>github: </xsl:text>
        <xsl:value-of select="document($versionInfo)/resources/string[@name=concat('project_board_',replace($version,'\.',''))]"/>
        <xsl:value-of select="$newline" />
        <xsl:value-of select="str:padding(6, '&#032;')" />
        <xsl:text>mastodon: </xsl:text>
        <xsl:value-of select="document($versionInfo)/resources/string[@name=concat('version_more_info_',replace($version,'\.',''))]"/>
        <xsl:value-of select="$newline" />
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
            <xsl:for-each select="tokenize($version)">
                <xsl:variable name="special-version-info">
                    <xsl:call-template name="special-version-info">
                        <xsl:with-param name="version" select="." />
                        <xsl:with-param name="strings" select="$strings" />
                        <xsl:with-param name="aosp" select="$aosp" />
                        <xsl:with-param name="help" select="$help" />
                        <xsl:with-param name="upgrade" select="$upgrade" />
                        <xsl:with-param name="lang" select="$lang" />
                        <xsl:with-param name="itemize" select="false()" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$special-version-info != ''">
                        <xsl:value-of select="$special-version-info" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:for-each select="document($upgrade)/resources/string[starts-with(@name,my:changeLogResourceName($version))]">
                            <xsl:apply-templates mode="unescape" select='.' />
                            <xsl:if test="$appendDot = 'true'">
                                <xsl:text>.</xsl:text>
                            </xsl:if>
                            <xsl:if test="position() != last()">
                                <xsl:text>&#032;</xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:variable>
        <xsl:if test="$changelog != ''">
            <xsl:value-of select="$newline" />
            <xsl:value-of select="str:padding(6, '&#032;')" />
            <xsl:value-of select="$lang" />
            <xsl:text>: |</xsl:text>
            <xsl:value-of select="$newline" />
            <xsl:value-of select="str:padding(8, '&#032;')" />
            <xsl:value-of select="$changelog" />
        </xsl:if>
    </xsl:template>

    <xsl:function name="str:padding">
        <xsl:param name="length" />
        <xsl:param name="chars" />
        <xsl:choose>
            <xsl:when test="not($length) or not($chars)" />
            <xsl:otherwise>
                <xsl:variable name="string"
                    select="concat($chars, $chars, $chars, $chars, $chars, $chars, $chars, $chars, $chars, $chars)" />
                <xsl:choose>
                    <xsl:when test="string-length($string) >= $length">
                        <xsl:value-of select="substring($string, 1, $length)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="str:padding($length, $string)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
</xsl:stylesheet>
