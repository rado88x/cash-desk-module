package com.fibank.cashdesk;

import com.fibank.cashdesk.service.CashDeskService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication (exclude = { SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class })
public class CashdeskApplication {

	public static void main(String[] args) {
		SpringApplication.run(CashdeskApplication.class, args);
	}

}


