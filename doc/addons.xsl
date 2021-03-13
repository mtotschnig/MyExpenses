<?xml version='1.0' ?>
<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:str="http://exslt.org/strings" extension-element-prefixes="str"
		version="1.0"
		>
  <xsl:output method="text" encoding="UTF-8"/>
  <xsl:include href="helpers.xsl" />
  <xsl:param name="addons" select="'splittemplate history budget ocr webui'"/>
  <xsl:param name="languages" select="$all-languages" />

  <xsl:template match="/">
    <xsl:text>Product ID,Published State,Purchase Type,Auto Translate,Locale; Title; Description,Auto Fill Prices,Price,Pricing Template ID</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:for-each select="str:tokenize($addons)">
      <xsl:variable name="addon" select="."/>
      <xsl:value-of select="."/><xsl:text>,published,managed_by_android,false,"</xsl:text>
    <xsl:for-each select="str:tokenize($languages)">
      <xsl:call-template name="extract">
        <xsl:with-param name="addon" select="$addon"/>
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
      <xsl:text>",false,,4637809629993162912</xsl:text>
      <xsl:value-of select="$newline"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="extract">
    <xsl:param name="addon"/>
    <xsl:param name="lang"/>
    <xsl:variable name="file">
      <xsl:text>../../Documents/MyExpenses.business/AddOns/strings</xsl:text>
      <xsl:call-template name="lang-file">
        <xsl:with-param name="lang" select="$lang"/>
      </xsl:call-template>
      <xsl:text>.xml</xsl:text>
    </xsl:variable>
    <xsl:variable name="title">
      <xsl:apply-templates select="document($file)/resources/string[@name=concat($addon,'_title')]" mode="unescape" />
    </xsl:variable>
    <xsl:variable name="description">
      <xsl:apply-templates select="document($file)/resources/string[@name=concat($addon,'_description')]" mode="unescape" />
    </xsl:variable>
    <xsl:if test="$title != '' and $description != ''">
      <xsl:call-template name="lang-play">
        <xsl:with-param name="lang" select="$lang"/>
      </xsl:call-template>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$title"/>
      <xsl:text>;</xsl:text>
      <xsl:value-of select="$description"/>
      <xsl:if test="position() != last()"><xsl:text>;</xsl:text></xsl:if>
    </xsl:if>
  </xsl:template>
  </xsl:stylesheet>
