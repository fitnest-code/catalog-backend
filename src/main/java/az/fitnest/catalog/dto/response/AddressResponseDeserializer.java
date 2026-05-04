package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import az.fitnest.catalog.dto.response.AddressResponse;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class AddressResponseDeserializer extends JsonDeserializer<AddressResponse> {
    @Override
    public AddressResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        Double lat = null;
        Double lng = null;
        String addressText = null;

        if (node.isTextual()) {
            String text = node.asText();
            String[] parts = text.split(",");
            if (parts.length >= 2) {
                try {
                    lat = Double.parseDouble(parts[0].trim());
                    lng = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        } else if (node.isObject()) {
            JsonNode latNode = node.get("latitude");
            JsonNode lngNode = node.get("longitude");
            JsonNode addrNode = node.get("addressText");
            if (latNode != null && latNode.isNumber()) {
                lat = latNode.doubleValue();
            }
            if (lngNode != null && lngNode.isNumber()) {
                lng = lngNode.doubleValue();
            }
            if (addrNode != null && addrNode.isTextual()) {
                addressText = addrNode.asText();
            }
        }

        return AddressResponse.builder()
                .latitude(lat)
                .longitude(lng)
                .addressText(addressText)
                .build();
    }
}
