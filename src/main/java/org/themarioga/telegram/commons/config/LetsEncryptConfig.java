package org.themarioga.telegram.commons.config;

import com.github.valb3r.letsencrypthelper.tomcat.TomcatWellKnownLetsEncryptChallengeEndpointConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("pro")
@ConditionalOnProperty(prefix = "lets-encrypt-helper", name = "enabled", havingValue = "true")
@Import(TomcatWellKnownLetsEncryptChallengeEndpointConfig.class)
public class LetsEncryptConfig {

}
