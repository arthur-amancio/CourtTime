package br.com.courttime.config;

import br.com.courttime.security.RevalidacaoSessaoUsuarioFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RevalidacaoSessaoUsuarioFilter revalidacaoSessaoUsuarioFilter,
            @Value("${courttime.https-obrigatorio:false}") boolean httpsObrigatorio) throws Exception {
        if (httpsObrigatorio) {
            // A sonda interna do Render pode usar HTTP; nunca rebaixar HTTPS.
            http.requiresChannel(channel -> channel.requestMatchers(request ->
                    !request.getRequestURI().equals(request.getContextPath() + "/healthz"))
                    .requiresSecure());
        }
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/img/**", "/login", "/healthz").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/secretaria/**").hasRole("SECRETARIA")
                        .requestMatchers("/seguranca/**").hasRole("SEGURANCA")
                        .requestMatchers("/solicitacoes/**").hasRole("ALUNO")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/inicio", true)
                        .failureUrl("/login?erro")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .addFilterBefore(revalidacaoSessaoUsuarioFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
