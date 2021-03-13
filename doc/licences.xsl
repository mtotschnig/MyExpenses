<?xml version='1.0' ?>
<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:str="http://exslt.org/strings" extension-element-prefixes="str"
		version="1.0"
		>
  <xsl:output method="text" encoding="UTF-8"/>
  <xsl:param name="languages" select="$all-languages" />

  <xsl:template match="/">
    <xsl:text>"</xsl:text>
    <xsl:for-each select="str:tokenize($languages)">
      <xsl:call-template name="extract">
        <xsl:with-param name="key" select="'contrib_key'"/>
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:text>"
"</xsl:text>
    <xsl:for-each select="str:tokenize($languages)">
      <xsl:call-template name="extract">
        <xsl:with-param name="key" select="'extended_key'"/>
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:text>"
"</xsl:text>
    <xsl:for-each select="str:tokenize($languages)">
      <xsl:call-template name="upgrade">
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="extract">
    <xsl:param name="key"/>
    <xsl:param name="lang"/>
    <xsl:variable name="file">
      <xsl:text>../myExpenses/src/main/res/values</xsl:text>
      <xsl:call-template name="lang-file">
        <xsl:with-param name="lang" select="$lang"/>
      </xsl:call-template>
      <xsl:text>/strings.xml</xsl:text>
    </xsl:variable>
    <xsl:call-template name="lang-play">
      <xsl:with-param name="lang" select="$lang"/>
    </xsl:call-template>
    <xsl:text>;</xsl:text>
    <xsl:if test="count(document($file)/resources/string[@name=$key]) = 0">
      <xsl:message terminate="yes">Missing key <xsl:value-of select="$key"/> for lang <xsl:value-of select="$lang"/> </xsl:message>
    </xsl:if>
    <xsl:apply-templates select="document($file)/resources/string[@name=$key]" mode="unescape" />
    <xsl:text>;</xsl:text>
    <xsl:if test="count(document($file)/resources/string[@name='pref_contrib_purchase_summary']) = 0">
      <xsl:message terminate="yes">Missing key pref_contrib_purchase_summary for lang <xsl:value-of select="$lang"/> </xsl:message>
    </xsl:if>
    <xsl:apply-templates select="document($file)/resources/string[@name='pref_contrib_purchase_summary']" mode="unescape" />
    <xsl:if test="position() != last()"><xsl:text>;</xsl:text></xsl:if>
  </xsl:template>

  <xsl:template name="upgrade">
    <xsl:param name="key"/>
    <xsl:param name="lang"/>
    <xsl:variable name="file">
      <xsl:text>../myExpenses/src/main/res/values</xsl:text>
      <xsl:call-template name="lang-file">
        <xsl:with-param name="lang" select="$lang"/>
      </xsl:call-template>
      <xsl:text>/strings.xml</xsl:text>
    </xsl:variable>
    <xsl:call-template name="lang-play">
      <xsl:with-param name="lang" select="$lang"/>
    </xsl:call-template>
    <xsl:text>;</xsl:text>
    <xsl:apply-templates select="document($file)/resources/string[@name='contrib_key']" mode="unescape" />
    <xsl:text> => </xsl:text>
    <xsl:apply-templates select="document($file)/resources/string[@name='extended_key']" mode="unescape" />
    <xsl:text>;</xsl:text>
    <xsl:apply-templates select="document($file)/resources/string[@name='pref_contrib_purchase_summary']" mode="unescape" />
    <xsl:if test="position() != last()"><xsl:text>;</xsl:text></xsl:if>
  </xsl:template>
  </xsl:stylesheet>
