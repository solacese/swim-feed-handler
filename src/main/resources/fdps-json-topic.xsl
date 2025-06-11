<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://www.opengis.net/gml/3.2" xmlns:ns2="http://www.fixm.aero/base/3.0" xmlns:ns3="http://www.fixm.aero/flight/3.0" xmlns:ns4="http://www.fixm.aero/foundation/3.0" xmlns:ns5="http://www.faa.aero/nas/3.0">
    <xsl:output method="text" encoding="UTF-8"/>
    <xsl:template match="/message">
        <xsl:text>FDPS/position/</xsl:text>
        <xsl:value-of select="flight/flightPlan/@identifier"/><xsl:text>/</xsl:text>
        <xsl:value-of select="flight/flightStatus/@fdpsFlightStatus"/><xsl:text>/</xsl:text>
        <xsl:value-of select="flight/flightIdentification/@aircraftIdentification"/><xsl:text>/</xsl:text>
        <xsl:value-of select="translate(flight/departure/@departurePoint,'/','')"/><xsl:text>/</xsl:text>
        <xsl:value-of select="translate(flight/arrival/@arrivalPoint,'/','')"/><xsl:text>/</xsl:text>
        <xsl:value-of select="format-number(number(substring-before(flight/enRoute/position/position/location/pos,' ')),'000.0000;-000.0000')"/><xsl:text>/</xsl:text>
        <xsl:value-of select="format-number(number(substring-after(flight/enRoute/position/position/location/pos,' ')),'0000.0000;-000.0000')"/><xsl:text>/</xsl:text>
        <xsl:value-of select="format-number(number(flight/enRoute/position/actualSpeed/surveillance),'000')"/><xsl:text>/</xsl:text>
        <xsl:value-of select="format-number(number(flight/enRoute/position/altitude),'00000')"/>
    </xsl:template>
</xsl:stylesheet>