package com.ururulab.ururu.auth;

import com.ururulab.ururu.config.TestAiServiceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAiServiceConfig.class)
class OAuth2AuthorizationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void springSecurityCreatesStateAndPkceAuthorizationRequest() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")))
                .andExpect(header().string("Location", containsString("state=")))
                .andExpect(header().string("Location", containsString("code_challenge=")))
                .andExpect(header().string("Location", containsString("code_challenge_method=S256")));
    }

    @Test
    void doesNotMakeFutureAuthEndpointsPublicByDefault() throws Exception {
        mockMvc.perform(get("/api/auth/not-registered"))
                .andExpect(status().isUnauthorized());
    }
}
