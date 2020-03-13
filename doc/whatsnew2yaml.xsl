<?xml version='1.0' ?>
	<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:str="http://exslt.org/strings" extension-element-prefixes="str"
		version="1.0"
		>
		<xsl:output method="text" encoding="UTF-8"/>
		<xsl:param name="version" />
		<xsl:param name="version_date" select='"2014-xx-xx"'/>
  	<xsl:param name="langs" select="'en fr de it es pt tr el eu bg ca cs ru hr ms ja ro pl si iw ta'"/>
  	

  <xsl:template match="/">
    <xsl:text>  -
    - </xsl:text><xsl:value-of select="$version"/><xsl:text>
    - "</xsl:text><xsl:value-of select="$version_date"/><xsl:text>"
    -</xsl:text>
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
    <xsl:variable name="version_short" select="str:replace($version,'.','')"/>
    <xsl:variable name="dir">
      <xsl:text>../myExpenses/src/main/res/values</xsl:text>
      <xsl:choose>
        <xsl:when test="$lang='en'"></xsl:when>
        <xsl:otherwise>-<xsl:value-of select="$lang"/></xsl:otherwise>
      </xsl:choose>
      <xsl:text>/upgrade.xml</xsl:text>
    </xsl:variable>
    <xsl:if test="document($dir)/resources/string-array[@name=concat('whats_new_',$version_short)]">
    <xsl:text>
      </xsl:text>
      <xsl:value-of select="$lang"/>
      <xsl:text>: |
       </xsl:text>
    <xsl:apply-templates select="document($dir)/resources/string-array[@name=concat('whats_new_',$version_short)]"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="string-array">
     <xsl:apply-templates select='item'/>
  </xsl:template>

  <xsl:template match="item">
  <xsl:variable name="apos">'</xsl:variable>
  <xsl:variable name="quote">"</xsl:variable>
    <xsl:value-of select="concat(' ',str:replace(str:replace(.,concat('\',$apos),$apos),concat('\',$quote),$quote),'.')" />
  </xsl:template>

</xsl:stylesheet>
