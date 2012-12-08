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
layout: default
title: "TODO: construct appropriate title"
section: news
headstuff: |
  <link rel="stylesheet" href="/css/news.css"/>
  <link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/themes/base/jquery-ui.css" />
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js" type="text/javascript"></script>
  <script type="text/javascript" src="/script/images.js"></script>
---
      <xsl:call-template name="user.header.navigation"/>
      <xsl:call-template name="header.navigation">
        <xsl:with-param name="prev" select="$prev"/>
        <xsl:with-param name="next" select="$next"/>
        <xsl:with-param name="nav.context" select="$nav.context"/>
      </xsl:call-template>
      <xsl:copy-of select="$content"/>
      <div id="disqus_thread"></div>
    <script type="text/javascript">
            /* * * CONFIGURATION VARIABLES: EDIT BEFORE PASTING INTO YOUR WEBPAGE * * */
            var disqus_shortname = 'myexpenses'; // required: replace example with your forum shortname
            /* * * DON'T EDIT BELOW THIS LINE * * */
            (function() {
                var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true;
                dsq.src = 'http://' + disqus_shortname + '.disqus.com/embed.js';
                (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq);
            })();
    </script>
    <noscript>Please enable JavaScript to view the <a href="http://disqus.com/?ref_noscript">comments powered by Disqus.</a></noscript>
    <a href="http://disqus.com" class="dsq-brlink">comments powered by <span class="logo-disqus">Disqus</span></a>
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
