<?xml version='1.0' ?>
<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:str="http://exslt.org/strings" extension-element-prefixes="str"
		version="1.0"
		>
  <xsl:output method="text" encoding="UTF-8"/>
  <xsl:include href="helpers.xsl" />
  <xsl:param name="languages" select="$all-languages" />

  <xsl:template match="/">
    <xsl:for-each select="str:tokenize($langs)">
      <xsl:call-template name="pro">
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="pro">
    <xsl:param name="lang"/>
    <xsl:variable name="app_file">
      <xsl:text>../myExpenses/src/main/res/values</xsl:text>
      <xsl:call-template name="lang-file">
        <xsl:with-param name="lang" select="$lang"/>
      </xsl:call-template>
      <xsl:text>/strings.xml</xsl:text>
    </xsl:variable>
    <xsl:variable name="addon_file">
      <xsl:text>../../Documents/MyExpenses.business/AddOns/strings</xsl:text>
      <xsl:call-template name="lang-file">
        <xsl:with-param name="lang" select="$lang"/>
      </xsl:call-template>
      <xsl:text>.xml</xsl:text>
    </xsl:variable>
    <xsl:text>    "</xsl:text>
    <xsl:call-template name="lang-play">
      <xsl:with-param name="lang" select="$lang"/>
    </xsl:call-template>
    <xsl:text>" to InAppProductListing()
        .setTitle("</xsl:text>
    <xsl:if test="count(document($app_file)/resources/string[@name='professional_key']) = 0">
      <xsl:message terminate="yes">Missing key professional_key for lang <xsl:value-of select="$lang"/> </xsl:message>
    </xsl:if>
    <xsl:apply-templates select="document($app_file)/resources/string[@name='professional_key']" mode="unescape" />
    <xsl:text>")
        .setDescription("</xsl:text>
    <xsl:apply-templates select="document($app_file)/resources/string[@name='pref_contrib_purchase_summary']" mode="unescape" />
    <xsl:text>")</xsl:text>
    <xsl:if test="count(document($addon_file)/resources/string[starts-with(@name,'professional_benefit_')]) > 0">
      <xsl:text>
        .setBenefits(listOf(</xsl:text>
      <xsl:for-each select="document($addon_file)/resources/string[starts-with(@name,'professional_benefit_')]">
        <xsl:text>"</xsl:text>
        <xsl:if test="string-length(.) > 40">
          <xsl:message terminate="yes">Benefit for lang <xsl:value-of select="$lang"/> must not exceed 40 characters.</xsl:message>
        </xsl:if>
        <xsl:apply-templates select="." mode="unescape"/>
        <xsl:text>"</xsl:text>
        <xsl:if test="position() != last()"><xsl:text>, </xsl:text></xsl:if>
      </xsl:for-each>
      <xsl:text>))</xsl:text>
    </xsl:if>
    <xsl:if test="position() != last()"><xsl:text>,
</xsl:text></xsl:if>
  </xsl:template>
  </xsl:stylesheet>
