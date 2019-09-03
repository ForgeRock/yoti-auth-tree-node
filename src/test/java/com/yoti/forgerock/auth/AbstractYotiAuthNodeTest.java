package com.yoti.forgerock.auth;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import static com.yoti.forgerock.auth.AbstractYotiAuthNode.SS_ATTRIBUTES;
import static com.yoti.forgerock.auth.AbstractYotiAuthNode.SS_USER_INFO;
import static com.yoti.forgerock.auth.AbstractYotiAuthNode.SS_USER_NAMES;
import static com.yoti.forgerock.auth.AbstractYotiAuthNode.TOKEN;

import static com.google.common.collect.ImmutableMap.of;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;

import com.google.common.base.Optional;
import com.yoti.api.client.ActivityDetails;
import com.yoti.api.client.FileKeyPairSource;
import com.yoti.api.client.YotiClient;
import com.yoti.api.client.YotiClientBuilder;
import com.yoti.forgerock.auth.AbstractYotiAuthNode.YotiAuthOutcome;

import com.google.common.collect.Maps;
import org.apache.commons.lang.reflect.FieldUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileKeyPairSource.class, YotiClientBuilder.class, Action.class })
public class AbstractYotiAuthNodeTest {

    private static final String SOME_SDK_ID = "someSdkId";
    private static final String SOME_REALM = "someRealm";
    private static final String SOME_TOKEN = "someToken";
    private static final String SOME_YOTI_ATTRIBUTE_NAME = "someYotiAttributeName";
    private static final String SOME_YOTI_ATTRIBUTE_VALUE = "someYotiAttributeValue";

    AbstractYotiAuthNode testSpy;

    @Mock FileKeyPairSource fileKeyPairSourceMock;
    @Mock(answer = RETURNS_SELF) YotiClientBuilder yotiClientBuilderMock;
    @Mock YotiClient yotiClientMock;
    @Mock YotiAttributeMapper yotiAttributeMapperMock;
    @Mock YotiRepositoryUtil yotiRepositoryUtilMock;

    @Mock Callback loginCallbackMock;
    @Mock(answer = RETURNS_DEEP_STUBS) ActivityDetails activityDetailsMock;
    Map<String, Set<String>> usernamesMap = emptyMap();
    Map<String, List<String>> mappedAttributes = of(SOME_YOTI_ATTRIBUTE_NAME, asList(SOME_YOTI_ATTRIBUTE_VALUE));
    JsonValue sharedStateJson = new JsonValue(Maps.newHashMap(singletonMap(REALM, SOME_REALM)));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(FileKeyPairSource.class);
        PowerMockito.when(FileKeyPairSource.class, "fromFile", Mockito.any()).thenReturn(fileKeyPairSourceMock);
        PowerMockito.mockStatic(YotiClientBuilder.class);
        PowerMockito.when(YotiClientBuilder.class, "newInstance").thenReturn(yotiClientBuilderMock);

        when(yotiClientBuilderMock.build()).thenReturn(yotiClientMock);
        when(yotiClientMock.getActivityDetails(SOME_TOKEN)).thenReturn(activityDetailsMock);

        when(yotiAttributeMapperMock.mapAccountAttributes(activityDetailsMock)).thenReturn(usernamesMap);
        when(yotiAttributeMapperMock.mapAttributes(activityDetailsMock)).thenReturn(mappedAttributes);
    }

    @Test
    public void constructor_shouldCreateYotiClient() throws Exception {
        testSpy = new TestableYotiAuthNode(createConfig(false));

        verify(yotiClientBuilderMock).forApplication(SOME_SDK_ID);
        verify(yotiClientBuilderMock).withKeyPair(fileKeyPairSourceMock);
        assertEquals(yotiClientMock, FieldUtils.getField(AbstractYotiAuthNode.class, "yotiClient", true).get(testSpy));
    }

    @Test
    public void process_shouldRedirectToSignIn() throws Exception {
        testSpy = spy(new TestableYotiAuthNode(createConfig(false)));
        TreeContext treeContext = createTreeContext(emptyMap(), sharedStateJson);
        doReturn(loginCallbackMock).when(testSpy).createYotiLoginCallback();

        Action result = testSpy.process(treeContext);

        assertTrue(result.callbacks.contains(loginCallbackMock));
        assertEquals(1, result.callbacks.size());
    }

    @Test
    public void process_shouldFailForEmptyToken() throws Exception {
        testSpy = new TestableYotiAuthNode(createConfig(false));
        TreeContext treeContext = createTreeContext(of(TOKEN, new String[] { "" }), sharedStateJson);

        Action result = testSpy.process(treeContext);

        assertEquals(YotiAuthOutcome.FAILED.name(), result.outcome);
    }

    @Test
    public void process_shouldHandleExistingUser() throws Exception {
        createAndPrepareTestSpy(createConfig(false));

        TreeContext treeContext = createTreeContext(of(TOKEN, new String[] { SOME_TOKEN }), sharedStateJson);
        when(yotiRepositoryUtilMock.userExistsInTheDataStore(SOME_REALM, usernamesMap)).thenReturn(Optional.of("existing"));

        Action result = testSpy.process(treeContext);

        assertEquals(YotiAuthOutcome.ACCOUNT_EXISTS.name(), result.outcome);
        assertEquals("existing", result.sharedState.get(USERNAME).asString());
        assertEquals(SOME_REALM, result.sharedState.get(REALM).asString());
        assertTrue(result.sessionProperties.isEmpty());
    }

    @Test
    public void process_shouldHandleExistingUserAndSaveAttributesToSession() throws Exception {
        createAndPrepareTestSpy(createConfig(true));

        TreeContext treeContext = createTreeContext(of(TOKEN, new String[] { SOME_TOKEN }), sharedStateJson);
        when(yotiRepositoryUtilMock.userExistsInTheDataStore(SOME_REALM, usernamesMap)).thenReturn(Optional.of("existing"));

        Action result = testSpy.process(treeContext);

        assertEquals(YotiAuthOutcome.ACCOUNT_EXISTS.name(), result.outcome);
        assertEquals("existing", result.sharedState.get(USERNAME).asString());
        assertEquals(SOME_REALM, result.sharedState.get(REALM).asString());
        assertEquals(SOME_YOTI_ATTRIBUTE_VALUE, result.sessionProperties.get(SOME_YOTI_ATTRIBUTE_NAME));
    }

    @Test
    public void process_shouldHandleNewUser() throws Exception {
        createAndPrepareTestSpy(createConfig(false));

        TreeContext treeContext = createTreeContext(of(TOKEN, new String[] { SOME_TOKEN }), sharedStateJson);
        when(yotiRepositoryUtilMock.userExistsInTheDataStore(SOME_REALM, usernamesMap)).thenReturn(Optional.absent());
        when(activityDetailsMock.getUserProfile().getEmailAddress().getValue()).thenReturn("someEmailAddress");

        Action result = testSpy.process(treeContext);

        assertEquals(YotiAuthOutcome.NO_ACCOUNT.name(), result.outcome);
        assertEquals(mappedAttributes, result.sharedState.get(SS_USER_INFO).get(SS_ATTRIBUTES).asMap());
        assertTrue(result.sharedState.get(SS_USER_INFO).get(SS_USER_NAMES).asMap().isEmpty());
        assertEquals("someEmailAddress", result.sharedState.get(SharedStateConstants.EMAIL_ADDRESS).asString());
    }

    @Test
    public void process_shouldHandleNewUserAndSaveAttributesToSession() throws Exception {
        createAndPrepareTestSpy(createConfig(true));

        TreeContext treeContext = createTreeContext(of(TOKEN, new String[] { SOME_TOKEN }), sharedStateJson);
        when(yotiRepositoryUtilMock.userExistsInTheDataStore(SOME_REALM, usernamesMap)).thenReturn(Optional.absent());
        when(activityDetailsMock.getUserProfile().getEmailAddress().getValue()).thenReturn("someEmailAddress");

        Action result = testSpy.process(treeContext);

        assertEquals(YotiAuthOutcome.NO_ACCOUNT.name(), result.outcome);
        assertEquals(mappedAttributes, result.sharedState.get(SS_USER_INFO).get(SS_ATTRIBUTES).asMap());
        assertTrue(result.sharedState.get(SS_USER_INFO).get(SS_USER_NAMES).asMap().isEmpty());
        assertEquals("someEmailAddress", result.sharedState.get(SharedStateConstants.EMAIL_ADDRESS).asString());
        assertEquals(SOME_YOTI_ATTRIBUTE_VALUE, result.sessionProperties.get(SOME_YOTI_ATTRIBUTE_NAME));
    }

    private void createAndPrepareTestSpy(TestableYotiAuthNode.Config config) throws Exception {
        testSpy = new TestableYotiAuthNode(config);
        overwriteField(testSpy, "yotiRepositoryUtil", yotiRepositoryUtilMock);
        overwriteField(testSpy, "yotiAttributeMapper", yotiAttributeMapperMock);
    }

    private static TestableYotiAuthNode.Config createConfig(boolean saveAttributes) {
        return new TestableYotiAuthNode.Config() {
            @Override
            public String pemFileLocation() {
                return "somePemFilePath";
            }

            @Override
            public String sdkId() {
                return SOME_SDK_ID;
            }

            @Override
            public boolean saveUserAttributesToSession() {
                return saveAttributes;
            }
        };
    }

    private static void overwriteField(AbstractYotiAuthNode obj, String name, Object value) throws Exception {
        Field field = FieldUtils.getDeclaredField(AbstractYotiAuthNode.class, name, true);
        FieldUtils.writeField(field, obj, value);
    }

    private static TreeContext createTreeContext(Map map, JsonValue sharedStateJson) {
        ExternalRequestContext requestContext = new ExternalRequestContext.Builder()
                .parameters(map)
                .build();
        return new TreeContext(sharedStateJson, requestContext, emptyList());
    }

    public static class TestableYotiAuthNode extends AbstractYotiAuthNode<TestableYotiAuthNode.Config> {

        public interface Config extends AbstractYotiAuthNode.Config {

        }

        protected TestableYotiAuthNode(Config config) throws NodeProcessException {
            super(config);
        }

        @Override
        protected Callback createYotiLoginCallback() throws NodeProcessException {
            return null;
        }

    }

}
