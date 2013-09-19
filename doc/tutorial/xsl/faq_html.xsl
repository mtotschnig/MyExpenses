<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:custom="custom" exclude-result-prefixes="custom">
<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/html/onechunk.xsl"/>
<xsl:import href="tutorial_strings.xsl"/>
<xsl:preserve-space elements="*"/>

<xsl:template name="chunk-element-content">
  <xsl:param name="prev"/>
  <xsl:param name="next"/>
  <xsl:param name="nav.context"/>
  <xsl:param name="content">
    <xsl:apply-imports/>
  </xsl:param>
  <div class="block" id="faq" style="display:none">
      <xsl:call-template name="user.header.navigation"/>
      <xsl:call-template name="header.navigation">
        <xsl:with-param name="prev" select="$prev"/>
        <xsl:with-param name="next" select="$next"/>
        <xsl:with-param name="nav.context" select="$nav.context"/>
      </xsl:call-template>

      <xsl:call-template name="user.header.content"/>

      <xsl:copy-of select="$content"/>
  <xsl:value-of select="$chunk.append"/>
  </div>
</xsl:template>

<xsl:param name="chunker.output.encoding" select="UTF-8"/>
<xsl:param name="chunker.output.omit-xml-declaration" select="'yes'"/>
<xsl:param name="use.id.as.filename" select="'1'"/>
<xsl:param name="chunk.first.sections" select="'1'"/>
<xsl:param name="suppress.footer.navigation" select="1"/>
<xsl:param name="formal.object.break.after" select="0"/>
<xsl:param name="chunker.output.doctype-public" select="''"/>
<xsl:param name="chunker.output.doctype-system" select="''"/>
<xsl:param name="root.filename"  select="''"/>
<xsl:param name="generate.id.attributes" select="1"/>
<xsl:param name="html.ext">_<xsl:value-of select="$doc.lang"/>.html</xsl:param>
<!-- no title attribute for sections -->
<xsl:template name="generate.html.title"/>

<!-- we do not display a title since it is visible from the navigation select box -->
<xsl:template name="sect1.titlepage.recto"/>

<xsl:template name="header.navigation">
  <xsl:variable name="chunkname">
    <xsl:apply-templates select="." mode="recursive-chunk-filename"/>
  </xsl:variable>
    <h2>
      <xsl:value-of select="title"/>
    </h2>
</xsl:template>

<xsl:template match="phrase[@role='br']">
<br />
</xsl:template>

<xsl:template name="inline.charseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <!-- * if you want output from the inline.charseq template wrapped in -->
  <!-- * something other than a Span, call the template with some value -->
  <!-- * for the 'wrapper-name' param -->
  <xsl:param name="wrapper-name">span</xsl:param>
  <xsl:element name="{$wrapper-name}">
    <xsl:attribute name="class">
      <xsl:value-of select="local-name(.)"/>
    </xsl:attribute>
    <xsl:call-template name="dir"/>
    <xsl:call-template name="generate.html.title"/>
    <xsl:copy-of select="$content"/>
    <xsl:call-template name="apply-annotations"/>
  </xsl:element>
</xsl:template>

<xsl:template name="inline.monoseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <code>
    <xsl:apply-templates select="." mode="common.html.attributes"/>
    <xsl:copy-of select="$content"/>
    <xsl:call-template name="apply-annotations"/>
  </code>
</xsl:template>

<xsl:template name="inline.boldseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>

  <span>
    <xsl:apply-templates select="." mode="common.html.attributes"/>

    <!-- don't put <strong> inside figure, example, or table titles -->
    <xsl:choose>
      <xsl:when test="local-name(..) = 'title'
                      and (local-name(../..) = 'figure'
                      or local-name(../..) = 'example'
                      or local-name(../..) = 'table')">
        <xsl:copy-of select="$content"/>
      </xsl:when>
      <xsl:otherwise>
        <strong>
          <xsl:copy-of select="$content"/>
        </strong>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="apply-annotations"/>
  </span>
</xsl:template>

<xsl:template name="inline.italicseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <em>
    <xsl:call-template name="common.html.attributes"/>
    <xsl:copy-of select="$content"/>
    <xsl:call-template name="apply-annotations"/>
  </em>
</xsl:template>

<xsl:template name="inline.boldmonoseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <!-- don't put <strong> inside figure, example, or table titles -->
  <!-- or other titles that may already be represented with <strong>'s. -->
  <xsl:choose>
    <xsl:when test="local-name(..) = 'title'
                    and (local-name(../..) = 'figure'
                         or local-name(../..) = 'example'
                         or local-name(../..) = 'table'
                         or local-name(../..) = 'formalpara')">
      <code>
        <xsl:call-template name="common.html.attributes"/>
        <xsl:copy-of select="$content"/>
        <xsl:call-template name="apply-annotations"/>
      </code>
    </xsl:when>
    <xsl:otherwise>
      <strong>
        <xsl:call-template name="common.html.attributes"/>
        <code>
          <xsl:call-template name="generate.html.title"/>
          <xsl:call-template name="dir"/>
          <xsl:copy-of select="$content"/>
        </code>
        <xsl:call-template name="apply-annotations"/>
      </strong>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="inline.italicmonoseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <em>
    <xsl:call-template name="common.html.attributes"/>
    <code>
      <xsl:call-template name="generate.html.title"/>
      <xsl:call-template name="dir"/>
      <xsl:copy-of select="$content"/>
      <xsl:call-template name="apply-annotations"/>
    </code>
  </em>
</xsl:template>

<xsl:template name="inline.superscriptseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <sup>
    <xsl:call-template name="generate.html.title"/>
    <xsl:call-template name="dir"/>
    <xsl:copy-of select="$content"/>
    <xsl:call-template name="apply-annotations"/>
  </sup>
</xsl:template>

<xsl:template name="inline.subscriptseq">
  <xsl:param name="content">
    <xsl:call-template name="inline.content"/>
  </xsl:param>
  <sub>
    <xsl:call-template name="generate.html.title"/>
    <xsl:call-template name="dir"/>
    <xsl:copy-of select="$content"/>
    <xsl:call-template name="apply-annotations"/>
  </sub>
</xsl:template>

<!-- do not float figures and suppress caption-->
<xsl:template name="floatstyle"/>
<xsl:template name="formal.object.heading"/>
<xsl:template match="question">
  <xsl:variable name="deflabel">
    <xsl:apply-templates select="." mode="qanda.defaultlabel"/>
  </xsl:variable>

  <tr>
    <xsl:apply-templates select="." mode="common.html.attributes"/>
    <xsl:attribute name="id"><xsl:value-of select="../@id"/></xsl:attribute>
    <td align="{$direction.align.start}" valign="top">

      <xsl:variable name="label.content">
        <xsl:apply-templates select="." mode="qanda.label"/>
      </xsl:variable>

      <xsl:if test="string-length($label.content) &gt; 0">
        <p><b>
          <xsl:copy-of select="$label.content"/>
        </b></p>
      </xsl:if>
    </td>
    <td align="{$direction.align.start}" valign="top">
      <xsl:choose>
        <xsl:when test="$deflabel = 'none' and not(label)">
          <b><xsl:apply-templates select="*[local-name(.) != 'label']"/></b>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="*[local-name(.) != 'label']"/>
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </tr>
</xsl:template>

</xsl:stylesheet>
