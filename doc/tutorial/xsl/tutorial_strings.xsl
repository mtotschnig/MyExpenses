<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template name="inline.content">
  <xsl:variable name="id" select="@role"/>
  <xsl:choose>
  <xsl:when test="normalize-space(.)">
    <xsl:call-template name="anchor"/>
    <xsl:call-template name="simple.xlink">
      <xsl:with-param name="content">
        <xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="getString">
      <xsl:with-param name="id" select="$id"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="getString">
  <xsl:param name="id"/>
  <xsl:variable name="resdir">
    <xsl:choose>
      <xsl:when test="$doc.lang = 'en'">
        <xsl:text>values</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat('values-',$doc.lang)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="resfile" select="concat('../../../res/',$resdir,'/strings.xml')"/>
  <xsl:call-template name="unescape-android-string-resources">
      <xsl:with-param name="string" select="document($resfile)/resources/string[@name = $id]"/>
  </xsl:call-template>
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
