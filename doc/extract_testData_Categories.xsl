<?xml version='1.0' ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output encoding="UTF-8" />
    <xsl:template match="resources">
        <resources>
            <string name="testData_transaction1SubCat">
                <xsl:value-of select="string[@name='Sub_21_2']" /><!-- Metro -->
            </string>
            <string name="testData_transaction1MainCat">
                <xsl:value-of select="string[@name='Main_21']" /><!-- Transport -->
            </string>
            <string name="testData_transaction2SubCat">
                <xsl:value-of select="string[@name='Sub_6_5']" /><!-- Grocery -->
            </string>
            <string name="testData_transaction2MainCat">
                <xsl:value-of select="string[@name='Main_6']" /><!-- Food -->
            </string>
            <string name="testData_transaction3SubCat">
                <xsl:value-of select="string[@name='Sub_9_1']" /><!-- Fuel -->
            </string>
            <string name="testData_transaction3MainCat">
                <xsl:value-of select="string[@name='Main_9']" /><!-- Car -->
            </string>
            <string name="testData_transaction4SubCat">
                <xsl:value-of select="string[@name='Sub_6_6']" /><!-- Restaurant -->
            </string>
            <string name="testData_transaction6MainCat">
                <xsl:value-of select="string[@name='Main_10']" /><!-- Gifts -->
            </string>
            <string name="testData_templateMainCat">
                <xsl:value-of select="string[@name='Main_16']" /><!-- Housing -->
            </string>
            <string name="testData_templateSubCat">
                <xsl:value-of select="string[@name='Sub_16_2']" /><!-- Rent -->
            </string>
            <string name="testData_sync_backend_2_name">
                <xsl:value-of select="string[@name='Sub_22_3']" /><!-- Travels -->
            </string>
        </resources>
    </xsl:template>

</xsl:stylesheet>
