package br.org.edu.ifrn.LojaCarro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/carro/**", "/usuario/**", "/log/**")
                // Rotas liberadas: entrar, sair, verificar sessão e criar conta
                .excludePathPatterns("/usuario/login", "/usuario/logout", "/usuario/logado", "/usuario/salvar");
    }
}
