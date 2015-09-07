package co.edu.unicauca.diabetescare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/resources/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
    	    http.authorizeRequests()
                .antMatchers("/gmail/**").hasRole("USER")
                .anyRequest().permitAll()
                .and()
            .httpBasic()
            	.and()
            .csrf().disable();
    	// @formatter:on
	}

}