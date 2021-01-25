<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://www.opengis.net/gml/3.2" xmlns:ns2="http://www.fixm.aero/base/3.0" xmlns:ns3="http://www.fixm.aero/flight/3.0" xmlns:ns4="http://www.fixm.aero/foundation/3.0" xmlns:ns5="http://www.faa.aero/nas/3.0">
    <xsl:output method="text" encoding="UTF-8"/>
    <xsl:template match="/message">
        <xsl:text>{</xsl:text>
        <xsl:text>"identifier":"</xsl:text><xsl:value-of select="flight/flightPlan/@identifier"/><xsl:text>",</xsl:text>
        <xsl:text>"fdpsFlightStatus":"</xsl:text><xsl:value-of select="flight/flightStatus/@fdpsFlightStatus"/><xsl:text>",</xsl:text>
        <xsl:text>"operator":"</xsl:text><xsl:value-of select="flight/operator/operatingOrganization/organization/@name"/><xsl:text>",</xsl:text>
        <xsl:text>"aircraftIdentification":"</xsl:text><xsl:value-of select="flight/flightIdentification/@aircraftIdentification"/><xsl:text>",</xsl:text>
        <xsl:text>"departurePoint":"</xsl:text><xsl:value-of select="flight/departure/@departurePoint"/><xsl:text>",</xsl:text>
        <xsl:text>"arrivalPoint":"</xsl:text><xsl:value-of select="flight/arrival/@arrivalPoint"/><xsl:text>",</xsl:text>
        <xsl:text>"lat":"</xsl:text><xsl:value-of select="substring-before(flight/enRoute/position/position/location/pos,' ')"/><xsl:text>",</xsl:text>
        <xsl:text>"lon":"</xsl:text><xsl:value-of select="substring-after(flight/enRoute/position/position/location/pos,' ')"/><xsl:text>",</xsl:text>
        <xsl:text>"surveillance":"</xsl:text><xsl:value-of select="flight/enRoute/position/actualSpeed/surveillance"/><xsl:text>",</xsl:text>
        <xsl:text>"altitude":"</xsl:text><xsl:value-of select="flight/enRoute/position/altitude"/><xsl:text>",</xsl:text>
        <xsl:text>"trackVelocityX":"</xsl:text><xsl:value-of select="flight/enRoute/position/trackVelocity/x"/><xsl:text>",</xsl:text>
        <xsl:text>"trackVelocityY":"</xsl:text><xsl:value-of select="flight/enRoute/position/trackVelocity/y"/><xsl:text>"</xsl:text>
        <xsl:text>}</xsl:text>
    </xsl:template>
</xsl:stylesheet>