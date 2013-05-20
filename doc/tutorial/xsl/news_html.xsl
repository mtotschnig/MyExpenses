<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:custom="custom" exclude-result-prefixes="custom">
<xsl:import href="/usr/share/xml/docbook/stylesheet/docbook-xsl/html/chunk.xsl"/>

<xsl:param name="use.id.as.filename" select="'1'"/>
<xsl:param name="chunk.first.sections" select="'1'"/>
<xsl:param name="chunker.output.omit-xml-declaration" select="'yes'"/>
<xsl:param name="toc.section.depth" select="'1'"/>
<xsl:param name="suppress.footer.navigation" select="1"/>
<xsl:param name="formal.object.break.after" select="0"/>
<xsl:param name="toc.list.type" select="'ul'"/>
<xsl:param name="chunker.output.doctype-public" select="''"/>
<xsl:param name="chunker.output.doctype-system" select="''"/>
<xsl:param name="chunker.output.encoding" select="'UTF-8'"/>

<!-- no title attribute for sections -->
<xsl:template name="generate.html.title"/>
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
section: news_old
metatitle: "<xsl:apply-templates select="/article" mode="object.title.markup.textonly"/><xsl:text> | </xsl:text><xsl:apply-templates select="." mode="object.title.markup.textonly"/>"
headstuff: |
  <link rel="stylesheet" href="/css/rightmenu.css" charset="UTF-8"/>
  <link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/themes/base/jquery-ui.css" charset="UTF-8"/>
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript" charset="UTF-8"><xsl:text> </xsl:text></script>
  <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js" type="text/javascript" charset="UTF-8"><xsl:text> </xsl:text></script>
  <script type="text/javascript" src="/script/images.js" charset="UTF-8"><xsl:text> </xsl:text></script>
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
styles: |
  h2 {margin-bottom: 0;}
---
      <xsl:call-template name="user.header.navigation"/>
      <xsl:call-template name="header.navigation">
        <xsl:with-param name="prev" select="$prev"/>
        <xsl:with-param name="next" select="$next"/>
        <xsl:with-param name="nav.context" select="$nav.context"/>
      </xsl:call-template>
      <xsl:copy-of select="$content"/>
<script type="text/javascript" src="https://apis.google.com/js/plusone.js" data-height="69"></script>
<div class="g-plus" data-href="https://plus.google.com/116736113799210525299"></div>
  <xsl:value-of select="$chunk.append"/>
</xsl:template>


<xsl:template name="sect1.titlepage.recto">
  <xsl:apply-templates mode="sect1.titlepage.recto.auto.mode" select="title"/>
  <xsl:apply-templates mode="sect1.titlepage.recto.auto.mode" select="sect1info/pubdate"/>
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
                <xsl:text> ( </xsl:text>
                <xsl:value-of select="sect1info/pubdate"/>
                <xsl:text> )</xsl:text>
              </a>
            </li>
          </xsl:if>
				</xsl:for-each>
			</ul>
		</li>
	</ul>
</div>
</xsl:template>

<xsl:template match="phrase[@role='br']">
<br />
</xsl:template>


<!-- do not float figures and suppress caption-->
<xsl:template name="floatstyle"/>
<xsl:template name="formal.object.heading"/>
</xsl:stylesheet>
