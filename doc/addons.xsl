<?xml version='1.0' ?>
<xsl:stylesheet xmlns:str="http://exslt.org/strings"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" extension-element-prefixes="str" version="1.0">
    <xsl:output encoding="UTF-8" method="text" />
    <xsl:include href="helpers_v1.xsl" />
    <xsl:param name="format" select="'play'" />

    <xsl:template match="/">
        <xsl:param name="proAddons" select="'splittemplate history budget ocr webui categorytree'" />
        <xsl:param name="addons" select="'accountsunlimited adfree csvimport distribution plansunlimited print splittransaction synchronization'" />
        <xsl:choose>
            <xsl:when test="$format = 'play'">
                <xsl:value-of
                    select="'Product ID,Published State,Purchase Type,Auto Translate,Locale; Title; Description,Auto Fill Prices,Price,Pricing Template ID'" />
                <xsl:value-of select="$newline" />
                <xsl:for-each select="str:tokenize($proAddons)">
                    <xsl:call-template name="linePlay">
                        <xsl:with-param name="addon" select="." />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:when>

            <xsl:when test="$format = 'amazon'">
                <xsl:value-of
                    select="'Product ID (SKU),Locale; Title; Description,Auto Fill Prices,Price,IAP Type'" />
                <xsl:value-of select="$newline" />
                <xsl:for-each select="str:tokenize($proAddons)">
                    <xsl:call-template name="lineAmazon">
                        <xsl:with-param name="addon" select="." />
                        <xsl:with-param name="prize" select="'US;5190000;MX;63000000;BR;15990000;GB;4270000;AU;7760000;JP;709000000;FR;4900000;DE;4900000;ES;4900000;IN;220000000;IT;4900000;CA;7110000'" />
                    </xsl:call-template>
                </xsl:for-each>
                <xsl:for-each select="str:tokenize($addons)">
                    <xsl:call-template name="lineAmazon">
                        <xsl:with-param name="addon" select="." />
                        <xsl:with-param name="prize" select="'US;3240000;MX;44000000;BR;9490000;GB;2670000;AU;4850000;JP;443000000;FR;3060000;DE;3060000;ES;3060000;IN;140000000;IT;3060000;CA;4440000'" />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="linePlay">
        <xsl:param name="addon" />
        <xsl:variable name="languages" select="$all-languages" />
        <xsl:value-of select="$addon" />
        <xsl:text>,published,managed_by_android,false,"</xsl:text>
        <xsl:for-each select="str:tokenize($languages)">
            <xsl:call-template name="extract">
                <xsl:with-param name="addon" select="$addon" />
                <xsl:with-param name="langFile" select="." />
                <xsl:with-param name="langOutput">
                    <xsl:call-template name="lang-play">
                        <xsl:with-param name="lang" select="." />
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:text>",false,,4637809629993162912</xsl:text>
        <xsl:value-of select="$newline" />
    </xsl:template>

    <xsl:template name="lineAmazon">
        <xsl:param name="addon" />
        <xsl:param name="prize" />
        <xsl:variable name="languages" select="'de es fr it ja pt zh en'" />
        <xsl:value-of select="$addon" />
        <xsl:text>,"</xsl:text>
        <xsl:for-each select="str:tokenize($languages)">
            <xsl:call-template name="extract">
                <xsl:with-param name="addon" select="$addon" />
                <xsl:with-param name="langFile" select="." />
                <xsl:with-param name="langOutput">
                    <xsl:call-template name="lang-amazon">
                        <xsl:with-param name="lang" select="." />
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:text>",FALSE,</xsl:text>
        <xsl:value-of select="$prize" />
        <xsl:text>,Entitlement</xsl:text>
        <xsl:value-of select="$newline" />
    </xsl:template>

    <xsl:template name="extract">
        <xsl:param name="addon" />
        <xsl:param name="langFile" />
        <xsl:param name="langOutput" />
        <xsl:variable name="file">
            <xsl:text>../business/ProductCatalog/values</xsl:text>
            <xsl:call-template name="lang-file">
                <xsl:with-param name="lang" select="$langFile" />
            </xsl:call-template>
            <xsl:text>/strings.xml</xsl:text>
        </xsl:variable>
        <xsl:variable name="title">
            <xsl:apply-templates mode="unescape"
                select="document($file)/resources/string[@name=concat($addon,'_title')]" />
        </xsl:variable>
        <xsl:variable name="description">
            <xsl:apply-templates mode="unescape"
                select="document($file)/resources/string[@name=concat($addon,'_description')]" />
        </xsl:variable>
        <xsl:if test="$title != '' and $description != ''">
            <xsl:value-of select="$langOutput" />
            <xsl:text>;</xsl:text>
            <xsl:value-of select="$title" />
            <xsl:text>;</xsl:text>
            <xsl:value-of select="$description" />
            <xsl:if test="position() != last()">
                <xsl:text>;</xsl:text>
            </xsl:if>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
