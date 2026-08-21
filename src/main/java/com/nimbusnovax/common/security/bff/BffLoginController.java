package com.nimbusnovax.common.security.bff;

import com.nimbusnovax.common.security.NimbusNovaxSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class BffLoginController {

  private static final String REGISTRATION_ID = "nimbusnovax-bff";

  private final NimbusNovaxSecurityProperties props;

  @GetMapping("/bff/login")
  public String login(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token && token.isAuthenticated()) {
      return "redirect:" + props.getWeb().getSpaBaseUrl();
    }
    return "redirect:/oauth2/authorization/" + REGISTRATION_ID;
  }
}
