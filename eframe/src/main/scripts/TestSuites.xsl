<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <xsl:for-each select="testrun/suite">
            <xsl:value-of select="@locationUrl"/>

        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>