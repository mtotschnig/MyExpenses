<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                    xmlns:fo="http://www.w3.org/1999/XSL/Format"> 
<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/fo/docbook.xsl"/>
<xsl:import href="tutorial_strings.xsl"/>

<xsl:attribute-set name="section.title.properties">
  <xsl:attribute name="clear">both</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="xref.properties">
    <xsl:attribute name="text-decoration">underline</xsl:attribute>
</xsl:attribute-set>

<xsl:param name="paper.type">A4</xsl:param>
<xsl:param name="generate.toc"></xsl:param>

<xsl:template name="inline.charseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <xsl:choose>
    <xsl:when test="@dir">
      <fo:inline>
        <xsl:attribute name="direction">
          <xsl:choose>
            <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
            <xsl:otherwise>rtl</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:copy-of select="$content"/>
      </fo:inline>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$content"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="inline.monoseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline xsl:use-attribute-sets="monospace.properties">
    <xsl:call-template name="anchor"/>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<xsl:template name="inline.boldseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline font-weight="bold">
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<xsl:template name="inline.italicseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline font-style="italic">
    <xsl:call-template name="anchor"/>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<xsl:template name="inline.boldmonoseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline font-weight="bold" xsl:use-attribute-sets="monospace.properties">
    <xsl:call-template name="anchor"/>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<xsl:template name="inline.italicmonoseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline font-style="italic" xsl:use-attribute-sets="monospace.properties">
    <xsl:call-template name="anchor"/>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<xsl:template name="inline.superscriptseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline xsl:use-attribute-sets="superscript.properties">
    <xsl:call-template name="anchor"/>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="$fop.extensions != 0">
        <xsl:attribute name="vertical-align">super</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="baseline-shift">super</xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>

<xsl:template name="inline.subscriptseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <fo:inline xsl:use-attribute-sets="subscript.properties">
    <xsl:call-template name="anchor"/>
    <xsl:if test="@dir">
      <xsl:attribute name="direction">
        <xsl:choose>
          <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
          <xsl:otherwise>rtl</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="$fop.extensions != 0">
        <xsl:attribute name="vertical-align">sub</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="baseline-shift">sub</xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:copy-of select="$content"/>
  </fo:inline>
</xsl:template>


<!-- overriden for setting end.indent -->
<xsl:template match="figure">
  <xsl:variable name="param.placement"
              select="substring-after(normalize-space($formal.title.placement),
                                      concat(local-name(.), ' '))"/>

  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="figure">
    <xsl:choose>
      <xsl:when test="@pgwide = '1'">
        <fo:block xsl:use-attribute-sets="pgwide.properties">
          <xsl:call-template name="formal.object">
            <xsl:with-param name="placement" select="$placement"/>
          </xsl:call-template>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="formal.object">
          <xsl:with-param name="placement" select="$placement"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="floatstyle">
    <xsl:call-template name="floatstyle"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$floatstyle != ''">
      <xsl:call-template name="floater">
        <xsl:with-param name="position" select="$floatstyle"/>
        <xsl:with-param name="content" select="$figure"/>
        <xsl:with-param name="end.indent" select="'1em'"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$figure"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="processing-instruction('float-clear')">
   <fo:block clear="both"/>
</xsl:template>
<!-- suppress caption in figure -->
<xsl:template name="formal.object.heading"/>

<xsl:template match="application">
  <fo:inline font-style="italic">
    <xsl:call-template name="inline.charseq"/>
  </fo:inline>
</xsl:template>
<xsl:template match="guilabel">
  <fo:inline font-weight="bold">
    <xsl:call-template name="inline.charseq"/>
  </fo:inline>
</xsl:template>

<xsl:template match="menuchoice">
  <xsl:variable name="shortcut" select="./shortcut"/>
  <fo:inline border="1pt solid black" padding="1pt">
  <xsl:call-template name="process.menuchoice"/>
  <xsl:if test="$shortcut">
    <xsl:text> (</xsl:text>
    <xsl:apply-templates select="$shortcut"/>
    <xsl:text>)</xsl:text>
  </xsl:if>
  </fo:inline>
</xsl:template>
 <xsl:template match="guibutton">
    <fo:inline border="1px outset #dddddd"
               background-color="#dddddd">
      <xsl:call-template name="inline.charseq"/>
    </fo:inline>
  </xsl:template>
  
  <xsl:template match="abstract" mode="titlepage.mode"/>
</xsl:stylesheet>
