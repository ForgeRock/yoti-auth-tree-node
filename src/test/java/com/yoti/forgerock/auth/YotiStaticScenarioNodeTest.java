package com.yoti.forgerock.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.when;

import com.yoti.api.client.FileKeyPairSource;
import com.yoti.api.client.YotiClient;
import com.yoti.api.client.YotiClientBuilder;

import com.sun.identity.authentication.spi.RedirectCallback;
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
    public void createYotiLoginCallback() {
        RedirectCallback result = (RedirectCallback) testObj.createYotiLoginCallback();

        assertEquals("GET", result.getMethod());
        assertEquals("someRedirectUri" + "someAppId", result.getRedirectUrl());
        assertTrue(result.getTrackingCookie());
    }

    private static class TestConfig implements YotiStaticScenarioNode.Config {

        @Override
        public String pemFileLocation() {
            return "somePemFilePath";
        }

        @Override
        public String appId() {
            return "someAppId";
        }

        @Override
        public String sdkId() {
            return null;
        }

        @Override
        public String redirectUri() {
            return "someRedirectUri";
        }

    }

}
