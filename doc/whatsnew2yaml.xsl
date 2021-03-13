<?xml version='1.0' ?>
	<xsl:stylesheet
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:str="http://exslt.org/strings" extension-element-prefixes="str"
		version="1.0">
		<xsl:output method="text" encoding="UTF-8"/>
        <xsl:include href="play_languages.xsl" />
		<xsl:param name="version" />
		<xsl:param name="version_date" select='"2014-xx-xx"'/>
    <xsl:param name="languages" select="$all-languages" />

  <xsl:template match="/">
      <xsl:text>  -</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:text>    - </xsl:text>
      <xsl:value-of select="$version"/>
      <xsl:value-of select="$newline"/>
      <xsl:text>    - "</xsl:text>
      <xsl:value-of select="$version_date"/><xsl:text>"</xsl:text>
      <xsl:value-of select="$newline"/>
      <xsl:text>    -</xsl:text>
    <xsl:for-each select="str:tokenize($languages)">
      <xsl:call-template name="extract">
        <xsl:with-param name="lang" select="."/>
      </xsl:call-template>
    </xsl:for-each>
      <xsl:value-of select="$newline"/>
  </xsl:template>

  <xsl:template name="extract">
    <xsl:param name="lang"/>
    <xsl:variable name="version_short" select="str:replace($version,'.','')"/>
    <xsl:variable name="dir">
      <xsl:text>../myExpenses/src/main/res/values</xsl:text>
        <xsl:call-template name="lang-file">
            <xsl:with-param name="lang" select="$lang"/>
        </xsl:call-template>
      <xsl:text>/upgrade.xml</xsl:text>
    </xsl:variable>
    <xsl:if test="document($dir)/resources/string-array[@name=concat('whats_new_',$version_short)]">
        <xsl:value-of select="$newline"/>
    <xsl:text>      </xsl:text>
      <xsl:value-of select="$lang"/>
      <xsl:text>: |</xsl:text>
        <xsl:value-of select="$newline"/>
        <xsl:text>       </xsl:text>
    <xsl:apply-templates select="document($dir)/resources/string-array[@name=concat('whats_new_',$version_short)]"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="string-array">
     <xsl:apply-templates select='item' mode="unescape"/>
  </xsl:template>

</xsl:stylesheet>
