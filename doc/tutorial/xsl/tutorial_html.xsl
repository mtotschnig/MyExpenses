<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:custom="custom" exclude-result-prefixes="custom">
<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/html/chunk.xsl"/>
<xsl:import href="tutorial_strings.xsl"/>
<xsl:preserve-space elements="*"/>

<custom:supported-langs>
 <lang code="en">English</lang>
 <lang code="fr">Français</lang>
 <lang code="de">Deutsch</lang>
 <lang code="it">Italiano</lang>
 <lang code="es">Español</lang>
</custom:supported-langs>

<xsl:template name="chunk-element-content">
  <xsl:param name="prev"/>
  <xsl:param name="next"/>
  <xsl:param name="nav.context"/>
  <xsl:param name="content">
    <xsl:apply-imports/>
  </xsl:param>

  <xsl:call-template name="user.preroot"/>---
title: ""
layout: default
section: tutorial
metatitle: "<xsl:apply-templates select="/article" mode="object.title.markup.textonly"/><xsl:text> | </xsl:text><xsl:apply-templates select="." mode="object.title.markup.textonly"/>"
metadescription: "<xsl:value-of select="normalize-space(/article/articleinfo/subtitle)"/><xsl:text> | </xsl:text><xsl:value-of select="normalize-space(./sect1info/abstract)"/>"
lang: <xsl:value-of select="$doc.lang"/>
headstuff: |
  <link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/themes/base/jquery-ui.css" charset="UTF-8"/>
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript" charset="UTF-8"><xsl:text> </xsl:text></script>
  <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js" type="text/javascript" charset="UTF-8"><xsl:text> </xsl:text></script>
  <script type="text/javascript" src="/script/images.js" charset="UTF-8"><xsl:text> </xsl:text></script>
  <link rel="stylesheet" type="text/css" href="/css/rightmenu.css" charset="UTF-8"/>
  <xsl:if test="$prev and name($prev)!='article'">
  <link rel="prev">
    <xsl:attribute name="href">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="$prev"/>
      </xsl:call-template>
    </xsl:attribute>
    <xsl:attribute name="title">
      <xsl:apply-templates select="$prev" mode="object.title.markup.textonly"/>
      </xsl:attribute>
  </link>
  </xsl:if>
  <xsl:if test="$next">
  <link rel="next">
    <xsl:attribute name="href">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="$next"/>
      </xsl:call-template>
    </xsl:attribute>
    <xsl:attribute name="title">
      <xsl:apply-templates select="$next" mode="object.title.markup.textonly"/>
    </xsl:attribute>
  </link>
  </xsl:if>
---
      <xsl:call-template name="user.header.navigation"/>
      <xsl:call-template name="header.navigation">
        <xsl:with-param name="prev" select="$prev"/>
        <xsl:with-param name="next" select="$next"/>
        <xsl:with-param name="nav.context" select="$nav.context"/>
      </xsl:call-template>

      <xsl:call-template name="user.header.content"/>

      <xsl:copy-of select="$content"/>
  <xsl:value-of select="$chunk.append"/>
</xsl:template>

<xsl:param name="chunker.output.encoding" select="UTF-8"/>
<xsl:param name="chunker.output.omit-xml-declaration" select="'yes'"/>
<xsl:param name="use.id.as.filename" select="'1'"/>
<xsl:param name="chunk.first.sections" select="'1'"/>
<xsl:param name="toc.section.depth" select="'1'"/>
<xsl:param name="suppress.footer.navigation" select="1"/>
<xsl:param name="formal.object.break.after" select="0"/>
<xsl:param name="chunker.output.doctype-public" select="''"/>
<xsl:param name="chunker.output.doctype-system" select="''"/>

<!-- no title attribute for sections -->
<xsl:template name="generate.html.title"/>

<xsl:template name="header.navigation">
<xsl:variable name="toc-context" select="."/>
  <xsl:variable name="chunkname">
    <xsl:apply-templates select="." mode="recursive-chunk-filename"/>
  </xsl:variable>
  <div id="rightmenu">
    <ul>
      <li>
        <a href="#">
          <xsl:call-template name="getString">
            <xsl:with-param name="id" select="'go_to_chapter'"/>
          </xsl:call-template>
        </a>
        <ul class="last">
          <xsl:for-each select="../sect1">
              <li>
                <a>
                <xsl:if test="$toc-context/@id != @id">
                  <xsl:attribute name="href">
                    <xsl:call-template name="href.target">
                      <xsl:with-param name="context" select="$toc-context"/>
                      <xsl:with-param name="toc-context" select="$toc-context"/>
                    </xsl:call-template>
                  </xsl:attribute>
                  </xsl:if>
                  <xsl:apply-templates select="." mode="titleabbrev.markup"/>
                </a>
              </li>
          </xsl:for-each>
        </ul>
      </li>
    </ul>
  </div> 
</xsl:template>

<xsl:template name="sect1.titlepage.recto">
  <div style="position:relative">
    <h2>
      <span id="pdflink">
      <xsl:text> </xsl:text>
      <a href="tutorial_r4.pdf" target="_top">
        <img style="vertical-align: middle;" title="PDF" alt="PDF" src="/visuals/pdf.png"/>
      </a>
      </span>
      <xsl:value-of select="title"/>
    </h2>
  </div>
  <xsl:apply-templates mode="article.titlepage.recto.auto.mode" select="/article/articleinfo/releaseinfo[not(@role) or @role!='generate-for-pdf']"/>
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
</xsl:stylesheet>
