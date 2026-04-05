package com.coffeehut.coffeehut;

import com.coffeehut.coffeehut.model.Member;
import com.coffeehut.coffeehut.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CoffeehutApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeehutApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(MemberRepository memberRepository) {
		return args -> {
			if (memberRepository.findByEmail("staff@coffeehut.com").isEmpty()) {
				Member staff = new Member();
				staff.setName("Staff User");
				staff.setEmail("staff@coffeehut.com");
				staff.setPassword("123456");
				staff.setTotalOrders(0);
				memberRepository.save(staff);
				System.out.println("测试账号已创建: staff@coffeehut.com / 123456");
			} else {
				System.out.println("测试账号已存在");
			}
		};
	}
}