package com.yoti.forgerock.auth;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import org.forgerock.json.JsonValue;

import com.yoti.api.client.ActivityDetails;
import com.yoti.api.client.Image;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class YotiAttributeMapper {

    public static final String IPLANET_AM_USER_ALIAS_LIST = "iplanet-am-user-alias-list";
    private final Map<String, String> mappings;

    public YotiAttributeMapper(final Map<String, String> mappings) {
        this.mappings = mappings;
    }

    public Map<String, List<String>> mapAttributes(final ActivityDetails activityDetails) {
        Map<String, List<String>> attributes = activityDetails.getUserProfile().getAttributes().stream()
                .collect(Collectors.toMap(att -> mappings.getOrDefault(att.getName(), att.getName()),
                                          att -> singletonList(mapToString(att.getValue()))));
        attributes.put(IPLANET_AM_USER_ALIAS_LIST, singletonList(activityDetails.getRememberMeId()));
        return attributes;
    }

    private String mapToString(final Object value) {
        if (value instanceof Image) {
            return ((Image) value).getBase64Content();
        } else if (value instanceof Collection) {
            return JsonValue.json(value).asString();
        }
        return value.toString();
    }

    public Map<String, Set<String>> mapAccountAttributes(final ActivityDetails activityDetails) {
        Map<String, Set<String>> userNames = new HashMap<>();
        userNames.put(IPLANET_AM_USER_ALIAS_LIST, singleton(activityDetails.getRememberMeId()));
        return userNames;
    }

}
