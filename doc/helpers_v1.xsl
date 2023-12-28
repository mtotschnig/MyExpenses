<?xml version='1.0' ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">

    <xsl:variable name="all-languages"
        select="'ar bg cs de el es fr hr hu it iw nl pl pt ro tr vi en'" />
<!--    <xsl:variable name="all-languages"
        select="'ar bg ca cs da de el es eu fr hr hu it iw ja kn ko km ms nl pl pt ro ru si ta te tr vi zh zh-TW en'" />-->
    <xsl:variable name='newline'>
        <xsl:text>&#xa;</xsl:text>
    </xsl:variable>

    <xsl:template name="values-dir">
        <xsl:param name="lang" />
        <xsl:text>../myExpenses/src/main/res/values</xsl:text>
        <xsl:call-template name="lang-file">
            <xsl:with-param name="lang" select="$lang" />
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="lang-file">
        <xsl:param name="lang" />
        <xsl:choose>
            <xsl:when test="$lang='en'" />
            <xsl:when test="$lang='zh'">-zh-rCN</xsl:when>
            <xsl:when test="$lang='zh-TW'">-zh-rTW</xsl:when>
            <xsl:when test="$lang='pt'">-pt-rBR</xsl:when>
            <xsl:otherwise>
                <xsl:text>-</xsl:text>
                <xsl:value-of select="$lang" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="lang-amazon">
        <xsl:param name="lang" />
        <xsl:choose>
            <xsl:when test="$lang = 'de'">de_DE</xsl:when>
            <xsl:when test="$lang = 'en'">en_US</xsl:when>
            <xsl:when test="$lang = 'es'">es_ES</xsl:when>
            <xsl:when test="$lang = 'fr'">fr_FR</xsl:when>
            <xsl:when test="$lang = 'it'">it_IT</xsl:when>
            <xsl:when test="$lang = 'ja'">ja_JP</xsl:when>
            <xsl:when test="$lang = 'pt'">pt_BR</xsl:when>
            <xsl:when test="$lang = 'zh'">zh_CN</xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">Lang not supported: <xsl:value-of select="$lang"/></xsl:message>
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
            <xsl:when test="$lang = 'kn'">kn-IN</xsl:when>
            <xsl:when test="$lang = 'ko'">ko-KR</xsl:when>
            <xsl:when test="$lang = 'nl'">nl-NL</xsl:when>
            <xsl:when test="$lang = 'pl'">pl-PL</xsl:when>
            <xsl:when test="$lang = 'pt'">pt-PT</xsl:when>
            <xsl:when test="$lang = 'ru'">ru-RU</xsl:when>
            <xsl:when test="$lang = 'si'">si-LK</xsl:when>
            <xsl:when test="$lang = 'ta'">ta-IN</xsl:when>
            <xsl:when test="$lang = 'te'">te-IN</xsl:when>
            <xsl:when test="$lang = 'tr'">tr-TR</xsl:when>
            <xsl:when test="$lang = 'zh'">zh-CN</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$lang" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="lang-metadata">
        <xsl:param name="lang" />
        <xsl:choose>
            <xsl:when test="$lang = 'bg'">bg-BG</xsl:when>
            <xsl:when test="$lang = 'de'">de-DE</xsl:when>
            <xsl:when test="$lang = 'en'">en-US</xsl:when>
            <xsl:when test="$lang = 'es'">es-ES</xsl:when>
            <xsl:when test="$lang = 'fr'">fr-FR</xsl:when>
            <xsl:when test="$lang = 'it'">it-IT</xsl:when>
            <xsl:when test="$lang = 'iw'">he</xsl:when>
            <xsl:when test="$lang = 'ja'">ja-JP</xsl:when>
            <xsl:when test="$lang = 'pl'">pl-PL</xsl:when>
            <xsl:when test="$lang = 'pt'">pt-PT</xsl:when>
            <xsl:when test="$lang = 'ru'">ru-RU</xsl:when>
            <xsl:when test="$lang = 'tr'">tr-TR</xsl:when>
            <xsl:when test="$lang = 'zh'">zh-Hans</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$lang" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
