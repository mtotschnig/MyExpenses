<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:custom="custom" exclude-result-prefixes="custom"> 
<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/html/chunk.xsl"/>
<xsl:import href="tutorial_strings.xsl"/>

<custom:supported-langs>
 <lang code="en">English</lang>
 <lang code="fr">Français</lang>
 <lang code="de">Deutsch</lang>
 <lang code="it">Italiano</lang>
 <lang code="es">Español</lang>
</custom:supported-langs>

<xsl:param name="html.stylesheet" select="'../../tutorial/style.css'"/>
<xsl:param name="use.id.as.filename" select="'1'"/>
<xsl:param name="chunk.first.sections" select="'1'"/>
<xsl:param name="toc.section.depth" select="'1'"/>
<xsl:param name="suppress.footer.navigation" select="1"/>
<xsl:param name="formal.object.break.after" select="0"/>

<xsl:template name="user.head.content">
	<link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/themes/base/jquery-ui.css" />
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
	<script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js" type="text/javascript"></script>
	<script type="text/javascript" src="../../tutorial/images.js"></script>
	<meta name="viewport" content="width=device-width;"/>
</xsl:template>

<xsl:template name="header.navigation">
  <xsl:variable name="doclang" select="/article/articleinfo/title/phrase/@lang"/>
  <xsl:variable name="chunkname">
    <xsl:apply-templates select="." mode="recursive-chunk-filename"/>
  </xsl:variable>
  <h1>
  <!-- we want the title to be invisible from inapp webview: we set it to display none,
  and make it visible through javascript. the webview has javascript disabled-->
  <span id="navigtitle" style="display:none">
  <a href="../../index.html"><span class="application">
  <xsl:call-template name="getString">
      <xsl:with-param name="id" select="'app_name'"/>
  </xsl:call-template>
  </span></a>
  <xsl:text> </xsl:text>
    <xsl:call-template name="getString">
      <xsl:with-param name="id" select="'tutorial'"/>
  </xsl:call-template>
  </span>
  <span class="langselector">
    <xsl:for-each select="document('')/*/custom:supported-langs/lang">
      <xsl:choose>
        <xsl:when test="@code != $doclang">
          <a href="../{@code}/{$chunkname}">
            <img title="{.}" src="../../tutorial/flags/{@code}.png" />
          </a>
        </xsl:when>
        <xsl:otherwise>
          <img width="22" title="{.}" src="../../tutorial/flags/{@code}.png" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text> </xsl:text>
    </xsl:for-each>
  </span>
  <span style="clear:both">&#160;</span>
  </h1>
  <xsl:call-template name="make.toc">
    <xsl:with-param name="toc.title.p" select="false()"/>
    <xsl:with-param name="nodes" select="../sect1"/>
  </xsl:call-template>
  <xsl:apply-templates mode="article.titlepage.recto.auto.mode" select="/article/articleinfo/releaseinfo"/>
</xsl:template>

<!-- add separator between entries in toc and do not create link for current section-->
<xsl:template name="toc.line">
  <xsl:param name="toc-context" select="."/>
  <xsl:param name="depth" select="1"/>
  <xsl:param name="depth.from.context" select="8"/>
 <span>
  <xsl:attribute name="class"><xsl:value-of select="local-name(.)"/></xsl:attribute>

  <!-- * if $autotoc.label.in.hyperlink is zero, then output the label -->
  <!-- * before the hyperlinked title (as the DSSSL stylesheet does) -->
  <xsl:if test="$autotoc.label.in.hyperlink = 0">
    <xsl:variable name="label">
      <xsl:apply-templates select="." mode="label.markup"/>
    </xsl:variable>
    <xsl:copy-of select="$label"/>
    <xsl:if test="$label != ''">
      <xsl:value-of select="$autotoc.label.separator"/>
    </xsl:if>
  </xsl:if>

  <a>
  <xsl:if test="$toc-context/@id != @id">
    <xsl:attribute name="href">
      <xsl:call-template name="href.target">
        <xsl:with-param name="context" select="$toc-context"/>
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:call-template>
    </xsl:attribute>
    </xsl:if>
  <!-- * if $autotoc.label.in.hyperlink is non-zero, then output the label -->
  <!-- * as part of the hyperlinked title -->
  <xsl:if test="not($autotoc.label.in.hyperlink = 0)">
    <xsl:variable name="label">
      <xsl:apply-templates select="." mode="label.markup"/>
    </xsl:variable>
    <xsl:copy-of select="$label"/>
    <xsl:if test="$label != ''">
      <xsl:value-of select="$autotoc.label.separator"/>
    </xsl:if>
  </xsl:if>

    <xsl:apply-templates select="." mode="titleabbrev.markup"/>
  </a>
  </span>
  
  <xsl:if test="position()!=last()">
  <span> | </span>
  </xsl:if>
</xsl:template>

<xsl:template match="phrase[@role='br']">
<br />
</xsl:template>

<xsl:template name="inline.charseq">
  <xsl:param name="content">
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

<!-- do not float figures and suppress caption-->
<xsl:template name="floatstyle"/>
<xsl:template name="formal.object.heading"/>
</xsl:stylesheet>
