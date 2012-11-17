<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:custom="custom" exclude-result-prefixes="custom"> 
<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/html/chunk.xsl"/>

<xsl:param name="use.id.as.filename" select="'1'"/>
<xsl:param name="chunk.first.sections" select="'1'"/>
<xsl:param name="toc.section.depth" select="'1'"/>
<xsl:param name="suppress.footer.navigation" select="1"/>
<xsl:param name="formal.object.break.after" select="0"/>
<xsl:param name="toc.list.type" select="'ul'"/>

<xsl:template name="html.head">
  <head>
    <xsl:call-template name="system.head.content"/>
    <title>
      <xsl:apply-templates select="/article" mode="object.title.markup.textonly"/>
      <xsl:text> | </xsl:text>
      <xsl:apply-templates select="." mode="object.title.markup.textonly"/>
    </title>
    <link rel="stylesheet" type="text/css" href="../styles.css"/>
    <meta name="generator" content="DocBook {$DistroTitle} V{$VERSION}"/>
    <xsl:variable name="description">
      <xsl:for-each select="/article/articleinfo/abstract[1]/*|./sect1info/abstract[1]/*">
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </xsl:variable>
    <meta name="description" content="{normalize-space($description)}"/>
    <meta http-equiv="content-language" content="en" />
    <link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/themes/base/jquery-ui.css" />
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js" type="text/javascript"></script>
    <script type="text/javascript" src="../../images.js"></script>
    <meta name="viewport" content="width=device-width;"/>
   </head>
</xsl:template>

<xsl:template name="sect1.titlepage.recto">
  <xsl:apply-templates mode="sect1.titlepage.recto.auto.mode" select="title"/>
</xsl:template>

<xsl:template name="header.navigation">
<xsl:variable name="toc-context" select="."/>
<div id="rightmenu">
	<ul>
		<li><a href="#">More news</a>
			<ul class="last">
			  <xsl:for-each select="../sect1">
			    <xsl:if test="$toc-context/@id != @id">
			      <li>
              <a>
                <xsl:attribute name="href">
                  <xsl:call-template name="href.target">
                    <xsl:with-param name="context" select="$toc-context"/>
                    <xsl:with-param name="toc-context" select="$toc-context"/>
                  </xsl:call-template>
                </xsl:attribute>
                <xsl:apply-templates select="." mode="titleabbrev.markup"/>
              </a>
            </li>
          </xsl:if>
				</xsl:for-each>
			</ul>
		</li>
	</ul>
</div>
</xsl:template>

<xsl:template name="toc.line">
  <xsl:param name="toc-context" select="."/>
  <xsl:param name="depth" select="1"/>
  <xsl:param name="depth.from.context" select="8"/>
  
  <xsl:if test="$toc-context/@id != @id">
  <a>
    <xsl:attribute name="href">
      <xsl:call-template name="href.target">
        <xsl:with-param name="context" select="$toc-context"/>
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:call-template>
    </xsl:attribute>
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
  </xsl:if>
</xsl:template>

<xsl:template match="phrase[@role='br']">
<br />
</xsl:template>


<!-- do not float figures and suppress caption-->
<xsl:template name="floatstyle"/>
<xsl:template name="formal.object.heading"/>
</xsl:stylesheet>
