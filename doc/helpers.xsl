<?xml version='1.0' ?>
<xsl:stylesheet xmlns:str="http://exslt.org/strings"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" extension-element-prefixes="str" version="1.0">
    <xsl:variable name="all-languages"
        select="'ar bg ca cs da de el es eu fr hr hu it iw ja ms km ko pl pt ro ru si ta tr vi zh en'" />
    <xsl:variable name='newline'>
        <xsl:text>&#xa;</xsl:text>
    </xsl:variable>

    <xsl:template name="values-dir">
        <xsl:param name="lang"/>
        <xsl:text>../myExpenses/src/main/res/values</xsl:text>
        <xsl:call-template name="lang-file">
            <xsl:with-param name="lang" select="$lang"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="special-version-info">
        <xsl:param name="version"/>
        <xsl:param name="strings"/>
        <xsl:param name="aosp"/>
        <xsl:if test="$version = '3.2.5'">
            <xsl:apply-templates select="document($strings)/resources/string[@name='contrib_feature_csv_import_label']" mode="unescape"/>
            <xsl:text>: </xsl:text>
            <xsl:apply-templates select="document($aosp)/resources/string[@name='autofill']" mode="unescape"/>
        </xsl:if>
    </xsl:template>

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
            select="str:replace(str:replace($trim,concat('\',apostrophe),apostrophe),concat('\',$quote),$quote)" />
    </xsl:template>

    <xsl:template name="lang-file">
        <xsl:param name="lang" />
        <xsl:choose>
            <xsl:when test="$lang='en'" />
            <xsl:when test="$lang='zh'">-zh-rTW</xsl:when>
            <xsl:otherwise>
                <xsl:text>-</xsl:text>
                <xsl:value-of select="$lang" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="lang-play">
        <xsl:param name="lang" />
        <xsl:choose>
            <xsl:when test="$lang = 'cs'">cs-CZ</xsl:when>
            <xsl:when test="$lang = 'da'">da-DK</xsl:when>
            <xsl:when test="$lang = 'de'">de-DE</xsl:when>
            <xsl:when test="$lang = 'en'">en-US</xsl:when>
            <xsl:when test="$lang = 'el'">el-GR</xsl:when>
            <xsl:when test="$lang = 'es'">es-ES</xsl:when>
            <xsl:when test="$lang = 'eu'">eu-ES</xsl:when>
            <xsl:when test="$lang = 'fr'">fr-FR</xsl:when>
            <xsl:when test="$lang = 'hu'">hu-HU</xsl:when>
            <xsl:when test="$lang = 'it'">it-IT</xsl:when>
            <xsl:when test="$lang = 'iw'">iw-IL</xsl:when>
            <xsl:when test="$lang = 'ja'">ja-JP</xsl:when>
            <xsl:when test="$lang = 'km'">km-KH</xsl:when>
            <xsl:when test="$lang = 'ko'">ko-KR</xsl:when>
            <xsl:when test="$lang = 'pl'">pl-PL</xsl:when>
            <xsl:when test="$lang = 'pt'">pt-PT</xsl:when>
            <xsl:when test="$lang = 'ru'">ru-RU</xsl:when>
            <xsl:when test="$lang = 'si'">si-LK</xsl:when>
            <xsl:when test="$lang = 'ta'">ta-IN</xsl:when>
            <xsl:when test="$lang = 'tr'">tr-TR</xsl:when>
            <xsl:when test="$lang = 'zh'">zh-TW</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$lang" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
