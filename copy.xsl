<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" version="1.0">
	<xsl:output method="xml" encoding="utf-8"/>
	<xsl:template match="/">
		<xsl:copy-of select="node()"/>
	</xsl:template>
</xsl:stylesheet>
