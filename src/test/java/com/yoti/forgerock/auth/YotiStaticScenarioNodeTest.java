package com.yoti.forgerock.auth;

import static com.yoti.forgerock.auth.matcher.WantedAttributeMatcher.forName;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.yoti.api.client.FileKeyPairSource;
import com.yoti.api.client.YotiClient;
import com.yoti.api.client.YotiClientBuilder;

import com.yoti.api.client.shareurl.DynamicScenario;
import com.yoti.api.client.shareurl.policy.DynamicPolicy;
import org.forgerock.openam.auth.node.api.Action;
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
public class YotiStaticScenarioNodeTest {

    YotiStaticScenarioNode testObj;

    @Mock FileKeyPairSource fileKeyPairSourceMock;
    @Mock(answer = RETURNS_SELF) YotiClientBuilder yotiClientBuilderMock;
    @Mock YotiClient yotiClientMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(FileKeyPairSource.class);
        PowerMockito.when(FileKeyPairSource.class, "fromFile", Mockito.any()).thenReturn(fileKeyPairSourceMock);
        PowerMockito.mockStatic(YotiClientBuilder.class);
        PowerMockito.when(YotiClientBuilder.class, "newInstance").thenReturn(yotiClientBuilderMock);

        when(yotiClientBuilderMock.build()).thenReturn(yotiClientMock);

        testObj = new YotiStaticScenarioNode(new TestConfig());
    }

    @Test
    public void createYotiLoginCallback_shouldSubstitueValuesIntoTheScript() throws Exception {
        ScriptTextOutputCallback result = (ScriptTextOutputCallback) testObj.createYotiLoginCallback();

        assertTrue(result.getMessage().contains("someSdkId"));
        assertTrue(result.getMessage().contains("someScenarioId"));
        assertTrue(result.getMessage().contains("STATIC"));
        assertTrue(result.getMessage().contains("https://yoti.com/someScriptPath"));
    }

    private static class TestConfig implements YotiStaticScenarioNode.Config {

        @Override
        public String pemFileLocation() {
            return "somePemFilePath";
        }

        @Override
        public String scenarioId() {
            return "someScenarioId";
        }

        @Override
        public String sdkId() {
            return "someSdkId";
        }

        @Override
        public String yotiScriptPath() {
            return "/someScriptPath";
        }

    }

}
