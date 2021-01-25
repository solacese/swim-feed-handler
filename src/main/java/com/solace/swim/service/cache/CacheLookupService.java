package com.solace.swim.service.cache;

import com.solace.swim.cache.stdds.SMESManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class CacheLookupService {
    @Autowired
    SMESManager smesManager;

    //@Async
    //public String lookupSMES(Map<String, ?> messageHeaders, String messagePayload) {
    public Message<?> lookupSMES(Message<?> msg) {
        MessageHeaders headers = msg.getHeaders();
        String airport = (String)headers.get("airport");

        String messagePayload = (String)msg.getPayload();

        ConcurrentHashMap<String, String> ingressJson = serializeJson(messagePayload);

        //CacheableJson ingressJsonObj = new CacheableJson(messagePayload);

        String stid = ingressJson.get("identifier");

        ConcurrentHashMap<String, String> egressJson = smesManager.getObjectByTrackId(stid);
        //CacheableJson egressJsonObj = smesManager.getObjectByTrackId(stid);

        if (egressJson==null || egressJson.isEmpty()) {
            egressJson = ingressJson;
            egressJson.put("airport", airport);

            //This is the first message of this aircraft
            //Check in ingress object contains aircraft identification, if so, then cache the message
            if (ingressJson.get("aircraftIdentification")!=null && !"".equals(ingressJson.get("aircraftIdentification"))) {
                smesManager.insert(stid, egressJson);
            } else {
                egressJson.put("aircraftIdentification", "UNKN");
            }

        } else {

            ingressJson.put("airport", airport);
            ingressJson.put("aircraftIdentification", egressJson.get("aircraftIdentification"));
            ingressJson.put("aircraftType", egressJson.get("aircraftType"));
            smesManager.insert(stid, ingressJson);
            egressJson=ingressJson;

        }

        return new GenericMessage<String>(deserializeJson(egressJson), msg.getHeaders());
    }

    private ConcurrentHashMap<String, String> serializeJson(String json) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonObj = jsonReader.readObject();
        jsonReader.close();

        for (String key: jsonObj.keySet()) {
            map.put(key, jsonObj.getString(key));
        }
        return map;
    }

    private String deserializeJson(ConcurrentHashMap<String,String> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (String key: map.keySet()) {
            builder.add(key, map.get(key));
        }
        return builder.build().toString();
    }
}
