<?xml version='1.0' ?>
<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:str="http://exslt.org/strings" extension-element-prefixes="str"
		version="1.0"
		>
  <xsl:output method="text" encoding="UTF-8"/>
	<xsl:param name="versionCode" />
	<xsl:param name="versionName" />
	<xsl:param name="langs" select="'en ca cs de es fr hr hu it ja ms pl pt ro ru si tr'"/>

  <xsl:template match="/">
    <xsl:for-each select="str:tokenize($langs)">
      <xsl:call-template name="extract">
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
  <xsl:text>
</xsl:text>
  </xsl:template>

  <xsl:template name="extract">
    <xsl:param name="lang"/>
    <xsl:variable name="dir">
      <xsl:text>../myExpenses/src/main/res/values</xsl:text>
      <xsl:choose>
        <xsl:when test="$lang='en'"></xsl:when>
        <xsl:otherwise>-<xsl:value-of select="$lang"/></xsl:otherwise>
      </xsl:choose>
      <xsl:text>/upgrade.xml</xsl:text>
    </xsl:variable>
    <xsl:variable name="changelog"><xsl:apply-templates select="document($dir)/resources/string-array"/></xsl:variable>
    <xsl:if test="$changelog != ''">    
    <xsl:value-of select="$lang"/><xsl:text>:
</xsl:text>
    <xsl:value-of select="$versionName"/><xsl:text>
</xsl:text>
<xsl:value-of select="$changelog"/>
  </xsl:if>        
  </xsl:template>

  <xsl:template match="string-array">
    <xsl:if test="@name=concat('whats_new_',$versionCode)">
     <xsl:apply-templates select='item'/>
    </xsl:if>
  </xsl:template>
  <xsl:template match="item">
    <xsl:value-of select="concat('- ',.,'.')" /><xsl:text>
</xsl:text>
  </xsl:template>
</xsl:stylesheet>
