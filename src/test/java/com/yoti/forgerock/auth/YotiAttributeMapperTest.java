package com.yoti.forgerock.auth;

import static java.util.Arrays.asList;

import static com.yoti.forgerock.auth.YotiAttributeMapper.IPLANET_AM_USER_ALIAS_LIST;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yoti.api.client.ActivityDetails;
import com.yoti.api.client.Attribute;
import com.yoti.api.client.Image;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YotiAttributeMapperTest {

    private static final String YOTI_STRING_ATTRIBUTE_NAME = "yotiStringAttributeName";
    private static final String AM_STRING_ATTRIBUTE_NAME = "amStringAttributeName";
    private static final String SOME_REMEMBER_ME_ID = "someRememberMeId";
    private static final String SOME_STRING_VALUE = "someStringValue";

    YotiAttributeMapper testObj;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) ActivityDetails activityDetailsMock;
    @Mock Image imageMock;
    @Mock Attribute<String> stringAttributeMock;
    @Mock Attribute<Image> imageAttributeMock;
    @Mock Attribute<Map<String, String>> simpleMapAttMock;
    @Mock Attribute<Map<String, Map>> complexMapAttMock;

    @Before
    public void setUp() {
        when(activityDetailsMock.getRememberMeId()).thenReturn(SOME_REMEMBER_ME_ID);

        when(stringAttributeMock.getName()).thenReturn(YOTI_STRING_ATTRIBUTE_NAME);
        when(stringAttributeMock.getValue()).thenReturn(SOME_STRING_VALUE);
    }

    @Test
    public void mapAttributes_shouldMapAttributeMatchingConfiguredMapping() {
        when(activityDetailsMock.getUserProfile().getAttributes()).thenReturn(asList(stringAttributeMock));

        testObj = new YotiAttributeMapper(ImmutableMap.of(YOTI_STRING_ATTRIBUTE_NAME, AM_STRING_ATTRIBUTE_NAME));
        Map<String, List<String>> result = testObj.mapAttributes(activityDetailsMock);

        assertEquals(result.get(AM_STRING_ATTRIBUTE_NAME).get(0), SOME_STRING_VALUE);
        assertEquals(result.get(IPLANET_AM_USER_ALIAS_LIST).get(0), SOME_REMEMBER_ME_ID);
    }

    @Test
    public void mapAttributes_shouldUseDefaultNameWhenNoMappingConfigured() {
        when(activityDetailsMock.getUserProfile().getAttributes()).thenReturn(asList(stringAttributeMock));

        testObj = new YotiAttributeMapper(Collections.emptyMap());
        Map<String, List<String>> result = testObj.mapAttributes(activityDetailsMock);

        assertEquals(result.get(YOTI_STRING_ATTRIBUTE_NAME).get(0), SOME_STRING_VALUE);
        assertEquals(result.get(IPLANET_AM_USER_ALIAS_LIST).get(0), SOME_REMEMBER_ME_ID);
    }

    @Test
    public void mapAttributes_shouldConvertImageToBase64String() {
        when(imageMock.getBase64Content()).thenReturn("someImageBase64Content");
        when(imageAttributeMock.getName()).thenReturn("imageAttributeName");
        when(imageAttributeMock.getValue()).thenReturn(imageMock);
        when(activityDetailsMock.getUserProfile().getAttributes()).thenReturn(asList(imageAttributeMock));

        testObj = new YotiAttributeMapper(Collections.emptyMap());
        Map<String, List<String>> result = testObj.mapAttributes(activityDetailsMock);

        assertEquals("someImageBase64Content", result.get("imageAttributeName").get(0));
        assertEquals(SOME_REMEMBER_ME_ID, result.get(IPLANET_AM_USER_ALIAS_LIST).get(0));
    }

    @Test
    public void mapAttributes_shouldConvertMapsToJson() {
        Map<String, String> simpleMapValue = ImmutableMap.of("simpleKey", "simpleValue");
        when(simpleMapAttMock.getName()).thenReturn("simpleMapAttName");
        when(simpleMapAttMock.getValue()).thenReturn(simpleMapValue);
        Map<String, Map> complexMapValue = ImmutableMap.of("outerKey", simpleMapValue);
        when(complexMapAttMock.getName()).thenReturn("complexMapAttName");
        when(complexMapAttMock.getValue()).thenReturn(complexMapValue);
        when(activityDetailsMock.getUserProfile().getAttributes()).thenReturn(asList(simpleMapAttMock, complexMapAttMock));

        testObj = new YotiAttributeMapper(Collections.emptyMap());
        Map<String, List<String>> result = testObj.mapAttributes(activityDetailsMock);

        assertEquals("{simpleKey=simpleValue}", result.get("simpleMapAttName").get(0));
        assertEquals("{outerKey={simpleKey=simpleValue}}", result.get("complexMapAttName").get(0));
        assertEquals(SOME_REMEMBER_ME_ID, result.get(IPLANET_AM_USER_ALIAS_LIST).get(0));
    }

    @Test
    public void mapAccountAttributes_shouldSetIPlanetAlias() {
        testObj = new YotiAttributeMapper(null);
        Map<String, Set<String>> result = testObj.mapAccountAttributes(activityDetailsMock);

        assertEquals(SOME_REMEMBER_ME_ID, result.get(IPLANET_AM_USER_ALIAS_LIST).iterator().next());
    }

}
