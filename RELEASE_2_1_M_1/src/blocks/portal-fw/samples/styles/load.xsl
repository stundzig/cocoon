<?xml version="1.0"?>
<!-- $Id: load.xsl,v 1.1 2003/03/09 00:05:32 pier Exp $ 

-->

<xsl:stylesheet version="1.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

 <xsl:template match="ROWSET">
  <xsl:apply-templates/>
  </xsl:template>

 <xsl:template match="ROW">
  <xsl:apply-templates/>
  </xsl:template>
  
  <!-- Copy all and apply templates -->
  <xsl:template match="@*|node()">
   <xsl:copy>
    <xsl:apply-templates select="@*|node()" />
   </xsl:copy>
  </xsl:template>


</xsl:stylesheet>
