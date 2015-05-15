<?xml version='1.0' ?>
	<xsl:stylesheet 
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  
		version="1.0"
		>
		<xsl:output encoding="UTF-8"/>
  <xsl:template match="resources">
  <resources>
    <string name="testData_transaction1SubCat"><xsl:value-of select="string[@name='Sub_23_2']"/><!-- Metro --></string>
    <string name="testData_transaction1MainCat"><xsl:value-of select="string[@name='Main_23']"/><!-- Transport --></string>
    <string name="testData_transaction2SubCat"><xsl:value-of select="string[@name='Sub_6_6']"/><!-- Grocery --></string>
    <string name="testData_transaction2MainCat"><xsl:value-of select="string[@name='Main_6']"/><!-- Food --></string>
    <string name="testData_transaction3SubCat"><xsl:value-of select="string[@name='Sub_9_1']"/><!-- Fuel --></string>
    <string name="testData_transaction3MainCat"><xsl:value-of select="string[@name='Main_9']"/><!-- Car --></string>
    <string name="testData_transaction4SubCat"><xsl:value-of select="string[@name='Sub_6_7']"/><!-- Restaurant --></string>
    <string name="testData_transaction6MainCat"><xsl:value-of select="string[@name='Main_11']"/><!-- Gifts --></string>
    <string name="testData_templateMainCat"><xsl:value-of select="string[@name='Main_18']"/><!-- Housing --></string>
    <string name="testData_templateSubCat"><xsl:value-of select="string[@name='Sub_18_2']"/><!-- Rent --></string>
  </resources>
  </xsl:template>

</xsl:stylesheet>
