<?xml version='1.0' ?>
	<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  
		version="1.0"
		>
		<xsl:output encoding="UTF-8"/>
  <xsl:template match="resources">
  <Grisbi_categ>
  <xsl:apply-templates select="string"/>
  </Grisbi_categ>
  </xsl:template>
  <xsl:template match="string">
  <xsl:variable name="unescaped_value">
    <xsl:call-template name="unescape-android-string-resources">
      <xsl:with-param name="string" select="."/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:text>
</xsl:text>
  <xsl:choose>
  <xsl:when test="substring(@name,1,4)='Main'">
  <Category Nb="{substring(@name,6)}" Na="{$unescaped_value}"/> 
  </xsl:when>
  <xsl:otherwise>
  <Sub_category Nbc="{substring-before(substring-after(@name,'_'),'_')}" Nb="{substring-after(substring-after(@name,'_'),'_')}"  Na="{$unescaped_value}"/>
  </xsl:otherwise>
  </xsl:choose>
  </xsl:template>
  
  <xsl:template name="unescape-android-string-resources">
  <xsl:param name="string"/>
  <xsl:call-template name="string-replace-all">
    <xsl:with-param name="text">
      <xsl:call-template name="string-replace-all">
        <xsl:with-param name="text" select="$string" />
        <xsl:with-param name="replace" select="&quot;\'&quot;"/>
        <xsl:with-param name="by" select="&quot;'&quot;" />
      </xsl:call-template>
    </xsl:with-param>
    <xsl:with-param name="replace" select='&apos;\"&apos;'/>
    <xsl:with-param name="by" select='&apos;"&apos;' />
  </xsl:call-template>
</xsl:template>

 <xsl:template name="string-replace-all">
    <xsl:param name="text" />
    <xsl:param name="replace" />
    <xsl:param name="by" />
    <xsl:choose>
      <xsl:when test="contains($text, $replace)">
        <xsl:value-of select="substring-before($text,$replace)" />
        <xsl:value-of select="$by" />
        <xsl:call-template name="string-replace-all">
          <xsl:with-param name="text"
          select="substring-after($text,$replace)" />
          <xsl:with-param name="replace" select="$replace" />
          <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
