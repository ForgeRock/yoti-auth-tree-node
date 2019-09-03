package com.yoti.forgerock.auth;

import static java.util.Arrays.asList;

import static com.yoti.forgerock.auth.matcher.WantedAttributeMatcher.forName;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.yoti.api.client.FileKeyPairSource;
import com.yoti.api.client.YotiClient;
import com.yoti.api.client.YotiClientBuilder;
import com.yoti.api.client.shareurl.DynamicScenario;
import com.yoti.api.client.shareurl.ShareUrlResult;
import com.yoti.api.client.shareurl.policy.DynamicPolicy;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import org.forgerock.openam.auth.node.api.Action;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileKeyPairSource.class, YotiClientBuilder.class, Action.class })
public class YotiDynamicScenarioNodeTest {

    private static final int SELFIE_AUTH_TYPE = 1;

    YotiDynamicScenarioNode testObj;

    @Mock FileKeyPairSource fileKeyPairSourceMock;
    @Mock(answer = RETURNS_SELF) YotiClientBuilder yotiClientBuilderMock;
    @Mock YotiClient yotiClientMock;
    @Mock ShareUrlResult shareUrlResultMock;
    @Captor ArgumentCaptor<DynamicScenario> scenarioCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(FileKeyPairSource.class);
        PowerMockito.when(FileKeyPairSource.class, "fromFile", any()).thenReturn(fileKeyPairSourceMock);
        PowerMockito.mockStatic(YotiClientBuilder.class);
        PowerMockito.when(YotiClientBuilder.class, "newInstance").thenReturn(yotiClientBuilderMock);

        when(yotiClientBuilderMock.build()).thenReturn(yotiClientMock);
        when(yotiClientMock.createShareUrl(any(DynamicScenario.class))).thenReturn(shareUrlResultMock);

        testObj = new YotiDynamicScenarioNode(new TestConfig());
    }

    @Test
    public void createYotiLoginCallback_shouldCreateScenarioFromConfiguredValues() throws Exception {
        testObj.createYotiLoginCallback();

        verify(yotiClientMock).createShareUrl(scenarioCaptor.capture());

        DynamicScenario capturedScenario = scenarioCaptor.getValue();
        assertEquals("someCallbackEndpoint", capturedScenario.callbackEndpoint());

        DynamicPolicy capturedPolicy = capturedScenario.policy();
        assertTrue(capturedPolicy.isWantedRememberMe());
        assertTrue(capturedPolicy.getWantedAuthTypes().contains(SELFIE_AUTH_TYPE));
        assertThat(capturedPolicy.getWantedAttributes(), hasItems(forName("attribute1"), forName("attribute2")));
    }

    @Test
    public void createYotiLoginCallback_shouldSubstitueValuesIntoTheScript() throws Exception {
        when(shareUrlResultMock.getUrl()).thenReturn("someUrl");

        ScriptTextOutputCallback result = (ScriptTextOutputCallback) testObj.createYotiLoginCallback();

        assertTrue(result.getMessage().contains("someSdkId"));
        assertTrue(result.getMessage().contains("someUrl"));
        assertTrue(result.getMessage().contains("DYNAMIC"));
        assertTrue(result.getMessage().contains("https://yoti.com/someScriptPath"));
    }

    private static class TestConfig implements YotiDynamicScenarioNode.Config {

        @Override
        public String pemFileLocation() {
            return "somePemFilePath";
        }

        @Override
        public String sdkId() {
            return "someSdkId";
        }

        @Override
        public String yotiScriptPath() {
            return "/someScriptPath";
        }

        @Override
        public String callbackUrl() {
            return "someCallbackEndpoint";
        }

        @Override
        public List<String> requestedAttributes() {
            return asList("attribute1", "attribute2");
        }

        @Override
        public boolean requireSelfieAuthentication() {
            return true;
        }

    }

}
