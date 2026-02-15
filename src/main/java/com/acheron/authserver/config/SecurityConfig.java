package com.acheron.authserver.config;

import com.acheron.authserver.entity.User;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final Oauth2AccessTokenCustomizer oauth2AccessTokenCustomizer;
    private final PasswordEncoder passwordEncoder;
    private final FederatedIdentityAuthenticationSuccessHandler auth2LoginSuccessHandler;
    private final CustomWebAuthenticationDetailsSource authenticationDetailsSource;
    private final MFADaoAuthProvider daoAuthenticationProvider;
    private final DynamicCorsConfigurationSource dynamicCorsConfigurationSource;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        authorizationServerConfigurer
                .clientAuthentication(clientAuth -> clientAuth
                        .authenticationConverter(new PublicClientRefreshTokenAuthenticationConverter())
                        .authenticationProvider(new PublicClientRefreshTokenAuthenticationProvider(
                                registeredClientRepository, authorizationService))
                );
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                )
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/mfa_qr", "/spa/logout", "/login", "/registration", "/.well-known/appspecific/**"
                                , "/favicon.ico", "/actuator/prometheus", "/error"
                        ).permitAll().anyRequest().authenticated());
        http.oidcLogout(Customizer.withDefaults());
        http
                .exceptionHandling((exceptions) ->
                        exceptions.defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults()));
        http.cors(cors -> cors.configurationSource(dynamicCorsConfigurationSource));
        http.formLogin(formLogin -> formLogin.loginPage("/login").permitAll().authenticationDetailsSource(authenticationDetailsSource));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(Customizer.withDefaults()));
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(formLogin -> formLogin.loginPage("/login").permitAll().authenticationDetailsSource(authenticationDetailsSource))
                .oauth2Login(oauth2Login ->
                        oauth2Login.loginPage("/login").permitAll()
                                .successHandler(auth2LoginSuccessHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/spa/logout", "/login"
                                , "/registration", "/.well-known/appspecific/**", "/actuator/prometheus",
                                "/reset_password",
                                "/reset_password_token",
                                "/img.png"
                                , "/favicon.ico",
                                "/front/**", "/mfa_qr", "/error"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll().anyRequest().authenticated());
        http.cors(cors -> cors.configurationSource(dynamicCorsConfigurationSource));
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {

        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);

        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();

        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(User.class)
                .allowIfSubType("com.acheron.authserver.entity")
                .allowIfSubType("java.util")
                .allowIfSubType("org.springframework.security");

        var securityModules = SecurityJacksonModules.getModules(classLoader, typeValidatorBuilder);

        JsonMapper jsonMapper = JsonMapper.builder()
                .addModules(securityModules)
                .activateDefaultTyping(typeValidatorBuilder.build(),
                        DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY)
                .build();

        JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper rowMapper =
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(registeredClientRepository, jsonMapper);

        JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper paramsMapper =
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper);

        service.setAuthorizationRowMapper(rowMapper);
        service.setAuthorizationParametersMapper(paramsMapper);

        return service;
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);
        //TODO move clients to changelogs
        String gatewayClientId = "gateway-client";
        if (repository.findByClientId(gatewayClientId) == null) {
            RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(gatewayClientId)
                    .clientSecret(passwordEncoder.encode("zxczxczxc"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("http://localhost:8080/login/oauth2/code/messaging-client-oidc")
                    .postLogoutRedirectUri("http://localhost:8080/logout")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .scope("message.read")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(5))
                            .refreshTokenTimeToLive(Duration.ofDays(20))
                            .reuseRefreshTokens(false)
                            .build())
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                    .build();
            repository.save(webClient);
        }

        return repository;
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> tokenGenerator(JwtEncoder jwtEncoder) {
        JwtGenerator jwtAccessTokenGenerator = new JwtGenerator(jwtEncoder);
        jwtAccessTokenGenerator.setJwtCustomizer(oauth2AccessTokenCustomizer);
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        OAuth2PublicClientRefreshTokenGenerator publicClientRefreshTokenGenerator =
                new OAuth2PublicClientRefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(
                jwtAccessTokenGenerator,
                refreshTokenGenerator,
                publicClientRefreshTokenGenerator
        );
    }
}
