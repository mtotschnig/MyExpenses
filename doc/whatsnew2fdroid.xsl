<?xml version='1.0' ?>
<xsl:stylesheet xmlns:my="http://myexpenses.mobi/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">
    <xsl:output encoding="UTF-8" method="xml" />
    <xsl:include href="helpers_v1.xsl" />
    <xsl:include href="helpers.xsl" />
    <xsl:param name="version" />
    <xsl:param name="versionCode" />
    <xsl:param name="languages" select="'ar bg de en es fr hu it iw pl pt ro tr'" />
   <!-- <xsl:param name="languages" select="'ar bg de en es fr hu it iw ja ko ms pl pt ro ru tr zh'" />-->

    <xsl:template name="main" match="/">
        <xsl:if test="$versionCode =''">
            <xsl:message terminate="yes">Required parameter versionCode is missing</xsl:message>
        </xsl:if>
        <xsl:for-each select="tokenize($languages)">
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
        <xsl:variable name="info">
            <xsl:text>../myExpenses/src/main/res/values/version_info.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="changelog">
            <xsl:variable name="entry">
                <xsl:variable name="special-version-info">
                    <xsl:call-template name="special-version-info">
                        <xsl:with-param name="version" select="$version" />
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
                        <xsl:for-each
                            select="document($upgrade)/resources/string[starts-with(@name,my:changeLogResourceName($version))]">
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
                <xsl:value-of select="$newline" />
            </xsl:if>
        </xsl:variable>
        <xsl:variable name="output">
            <xsl:text>metadata/</xsl:text>
            <xsl:call-template name="lang-metadata">
                <xsl:with-param name="lang" select="$lang" />
            </xsl:call-template>
            <xsl:text>/changelogs/</xsl:text>
            <xsl:value-of select="$versionCode" />
            <xsl:text>.txt</xsl:text>
        </xsl:variable>
        <xsl:variable name="github"
            select="document($info)/resources/string[@name=my:githubBoardResourceName($version)]" />
        <xsl:result-document href="{$output}" method="text">
            <xsl:value-of select="$changelog" />
            <xsl:value-of select="$newline" />
            <xsl:if test="$github != ''">
                <xsl:text>https://github.com/mtotschnig/MyExpenses/projects/</xsl:text>
                <xsl:value-of select="$github" />
                <xsl:value-of select="$newline" />
            </xsl:if>
            <!-- if needed manually add bug fix version:
            <xsl:text>https://github.com/mtotschnig/MyExpenses/projects/</xsl:text>
            <xsl:value-of select="document($info)/resources/string[@name=my:githubBoardResourceName('3.5.1.1')]" />
            <xsl:value-of select="$newline" />-->
        </xsl:result-document>
    </xsl:template>
</xsl:stylesheet>
