package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class AddressDtoDeserializer
        extends JsonDeserializer<AddressDto> {
    public AddressDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = (JsonNode) codec.readTree(p);
        AddressDto dto = new AddressDto();
        if (node.isTextual()) {
            String text = node.asText();
            String[] parts = text.split(",");
            if (parts.length >= 2) {
                try {
                    dto.setLatitude(Double.parseDouble(parts[0].trim()));
                    dto.setLongitude(Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException numberFormatException) {
                }
            }
        } else if (node.isObject()) {
            JsonNode latNode = node.get("latitude");
            JsonNode lngNode = node.get("longitude");
            JsonNode addrNode = node.get("addressText");
            if (latNode != null && latNode.isNumber()) {
                dto.setLatitude(latNode.doubleValue());
            }
            if (lngNode != null && lngNode.isNumber()) {
                dto.setLongitude(lngNode.doubleValue());
            }
            if (addrNode != null && addrNode.isTextual()) {
                dto.setAddressText(addrNode.asText());
            }
        }
        return dto;
    }
}

