package com.solace.swim.service.cache;

import com.solace.swim.cache.stdds.SMESManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import javax.json.*;
import java.io.StringReader;
import java.io.StringWriter;


@Service
public class CacheLookupService {
    @Autowired
    SMESManager smesManager;

    //@Async
    //public String lookupSMES(Map<String, ?> messageHeaders, String messagePayload) {
    public Message<?> lookupSMES(Message<?> msg) {
        String airport = (String)msg.getHeaders().get("airport");

        //String payload = "{\"asdexMsg\" : {\"airport\" : \"KPHL\", \"positionReport\" : {\"full\" : \"true\", \"seqNum\" : \"75\", \"time\" : \"2020-04-13T14:40:34.000Z\", \"track\" : \"1202\", \"stid\" : \"403118\", \"flightId\" : {\"aircraftId\" : \"AAL2622\", \"mode3ACode\" : \"2316\", \"acAddress\" : \"ABD010\"}, \"flightInfo\" : {\"tgtType\" : \"aircraft\", \"acType\" : \"B738\", \"wake\" : \"F\", \"fix\" : \"PHL\", \"runway\" : \"unassigned\"}, \"position\" : {\"x\" : \"122\", \"y\" : \"1471\", \"latitude\" : \"39.87851\", \"longitude\" : \"-75.23154\", \"altitude\" : \"12.5\", \"flightLevel\" : \"0.0\"}, \"movement\" : {\"speed\" : \"44\", \"heading\" : \"159.2852783203125\", \"vx\" : \"8.0\", \"vy\" : \"-21.0\", \"ax\" : \"-0.5\", \"ay\" : \"1.5\"}, \"status\" : {\"mon\" : \"0\", \"gbs\" : \"1\", \"mrh\" : \"barometric\", \"src\" : \"ground\", \"sim\" : \"0\", \"tse\" : \"0\", \"spi\" : \"0\", \"x\" : \"0\", \"gm\" : \"0\", \"nc\" : \"0\", \"ls\" : \"0\", \"aq\" : \"0\", \"ap\" : \"0\", \"op\" : \"0\", \"tc\" : \"0\", \"da\" : \"0\", \"lv\" : \"0\", \"st\" : \"0\", \"rt\" : \"0\", \"ss\" : \"1\", \"ms\" : \"1\", \"as\" : \"1\", \"a9s\" : \"0\", \"at\" : \"0\", \"si\" : \"0\", \"m3c\" : \"0\", \"di\" : \"1\", \"s1\" : \"1\", \"su\" : \"0\", \"af\" : \"unfiltered\", \"ua\" : \"0\", \"df\" : \"0\", \"quality\" : \"10\", \"aq1090\" : \"adsbicao\", \"lv1090\" : \"2\", \"aa\" : \"1\", \"av\" : \"valid\", \"sil\" : \"3\", \"nic\" : \"10\", \"NACp\" : \"10\", \"vs\" : \"geometric\", \"ud\" : \"down\", \"vertRate\" : \"0\", \"uncorrBaroAlt\" : \"13\"}, \"targetExtent\" : {\"estimate\" : \"35\", \"startRange\" : \"1498\", \"endRange\" : \"1538\", \"startAzimuth\" : \"3.4332275390625\", \"endAzimuth\" : \"4.5758056640625\"}, \"plotCount\" : \"5\", \"enhancedData\" : {\"eramGufi\" : \"KF39019200\", \"sfdpsGufi\" : \"us.fdps.2020-04-13T10:50:19Z.000/05/200\", \"departureAirport\" : \"KDFW\", \"destinationAirport\" : \"KPHL\"}}}}";
        JsonObject ingressJson = deserialize((String)msg.getPayload());

        // Build a new JSON off of the incoming message, this is the object that is updated, cached and returned.
        JsonObjectBuilder builder = Json.createObjectBuilder(ingressJson);
        // Airport is immediately added since it is not a part of the JSON after converting from XML
        builder.add("airport", airport);

        String trackIdentifier = ingressJson.getJsonObject("positionReport").getString("stid");

        // Look in the cache to determine if a msg w/ track identifier has been received
        JsonObject storedJson = deserialize(smesManager.getObjectByTrackId(trackIdentifier));

        JsonObject egressJson;
        if (storedJson==null || storedJson.isEmpty()) {
            //This is the first message of this aircraft
            //Check if ingress object contains aircraft identification, if so, then cache the message
            if ("true".equalsIgnoreCase(ingressJson.getJsonObject("positionReport").getString("full"))) {
                egressJson = builder.build();
                smesManager.insert(trackIdentifier, serialize(egressJson));
            } else {
                // This is an intermediate report.  Populate the data with UNKN until the next full report comes in
                JsonObject flightId = Json.createObjectBuilder()
                        .add("aircraftId","UNKN")
                        .add("mode3ACode", "UNKN")
                        .add("acAddress", "UNKN")
                        .build();
                JsonObject flightInfo = Json.createObjectBuilder()
                        .add("tgtType","UNKN")
                        .add("acType", "UNKN")
                        .add("wake", "UNKN")
                        .add("fix", "UNKN")
                        .add("runway","UNKN")
                        .build();
                JsonObjectBuilder positionReportBuilder = Json.createObjectBuilder(ingressJson.getJsonObject("positionReport"));
                builder.remove("positionReport");
                positionReportBuilder.add("flightId", flightId);
                positionReportBuilder.add("flightInfo", flightInfo);
                builder.add("positionReport", positionReportBuilder);
                egressJson = builder.build();
            }
        } else {
            // There was an existing object in cache, if the message is a full position report, then replace what is in cache
            // otherwise, copy just the flightId and flightInfo fields from the cached message
            if (!"true".equalsIgnoreCase(ingressJson.getJsonObject("positionReport").getString("full"))) {
                JsonObjectBuilder positionReportBuilder = Json.createObjectBuilder(ingressJson.getJsonObject("positionReport"));
                builder.remove("positionReport");
                if (ingressJson.getJsonObject("positionReport").getJsonObject("flightId") == null) {
                    // the incoming message does not contain information on the id of flight, use the previous message info
                    positionReportBuilder.remove("flightId");
                    positionReportBuilder.add("flightId", storedJson.getJsonObject("positionReport").getJsonObject("flightId"));
                }

                if (ingressJson.getJsonObject("positionReport").getJsonObject("flightInfo") == null) {
                    // the incoming message does not contain information on the aircraft info, use the previous message info
                    positionReportBuilder.remove("flightInfo");
                    positionReportBuilder.add("flightInfo", storedJson.getJsonObject("positionReport").getJsonObject("flightInfo"));
                }
                builder.add("positionReport", positionReportBuilder);
            }
            // Insert the new information into the cache
            egressJson = builder.build();
            smesManager.insert(trackIdentifier, serialize(egressJson));
        }

        return new GenericMessage<String>(serialize(egressJson), msg.getHeaders());

    }

    private String serialize(JsonObject jsonObj) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = Json.createWriter(stringWriter);
        writer.writeObject(jsonObj);
        writer.close();
        return stringWriter.getBuffer().toString();
    }

    private JsonObject deserialize(String jsonString) {
        JsonObject jsonObj = null;
        if (jsonString != null) {
            JsonReader cachedObjReader = Json.createReader(new StringReader(jsonString));
            jsonObj = cachedObjReader.readObject();
            cachedObjReader.close();
        }
        return jsonObj;
    }
}
