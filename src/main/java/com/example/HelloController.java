package com.example;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
	@GetMapping(path = "/")
	public String sayHello(@AuthenticationPrincipal UserDetails user) {
		return "Hello " + user.getUsername() + "!";
	}
}
