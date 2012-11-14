<?xml version='1.0' encoding="ISO-8859-1" ?>
<!-- ********************************************************************
	This file is part of the net.sf.docbooksml project.
	author joerg.moebius@hamburg.de
	Software and documentation are released under the terms of the GNU LGPL license 
	(see http://www.gnu.org/copyleft/lesser.html) and comes without a warranty of any kind.
	Documentation (synopsis,changelog,etc) is located at the end of this file.  
	******************************************************************** -->
	
	<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  
		version="1.0" 
		> 
		
    <xsl:param name="language">en</xsl:param>

    	<xsl:template match="*">
			<xsl:if test="not(@lang) or @lang=$language">
				<xsl:choose>
					<xsl:when test="count(.//*[not(@lang) or @lang=$language])=0">
						<xsl:copy-of select="." />
					</xsl:when>
					<xsl:otherwise>
						<xsl:element name="{name(.)}">
							<xsl:for-each select="@*"><xsl:copy/></xsl:for-each>
							<xsl:apply-templates/>
						</xsl:element>	
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:template>
		<xsl:template match="releaseinfo[@role='generate-for-pdf']">
		<releaseinfo role="generate-for-pdf">
		  <ulink>
		  <xsl:attribute name="url">http://myexpenses.totschnig.org/?lang=<xsl:value-of select="$language"/>#tutorial</xsl:attribute>
		  HTML
		  </ulink>
		</releaseinfo>
		</xsl:template>
		
		<xsl:template match="processing-instruction()">
		  <xsl:copy/>
		</xsl:template>

		<doc:documentation 
			xmlns:doc="http://www.opsdesign.eu/docscript/1.0"
			exclude-result-prefixes="doc"
			>
			<doc:synopsis>
				<section>
					<title>Extracts Language-specific content</title>
					<para>
						This script extracts a) all language-insensitiv content plus b) the content of a specific language.
					</para>
				</section>
			</doc:synopsis>            
			<doc:changelog>
				<revision>
					<revnumber>1</revnumber>
					<date>2006-11-26</date>
					<authorinitials>jmo</authorinitials>
					<revremark>first release</revremark>
				</revision>        
			</doc:changelog>
			<doc:copyright>
				<year>2006</year>
				<holder>joerg.moebius@hamburg.de</holder>
			</doc:copyright>
		</doc:documentation>
			
</xsl:stylesheet>
