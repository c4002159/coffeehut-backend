// CoffeehutApplication.java — App entry point and seed data -WeiqiWang

package com.coffeehut.coffeehut;

import com.coffeehut.coffeehut.model.Member;
import com.coffeehut.coffeehut.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class CoffeehutApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeehutApplication.class, args);
	}

	// The authoritative list of staff accounts. -WeiqiWang
	// On every startup, the database is synced to match this list exactly:
	//   - Accounts in this list but not in the DB are created.
	//   - Accounts in the DB but not in this list are deleted.
	// To add an account: add a row here and restart the backend.
	// To remove an account: delete the row here and restart the backend.
	// Format: { "display name", "email", "password" }
	private static final String[][] STAFF_ACCOUNTS = {
		{ "StaffUser",  "staff@coffeehut.com", "123456"   },
		{ "Weiqi Wang", "weiqi@coffeehut.com", "123456"   },
		{ "Manager",    "admin@coffeehut.com",  "admin123" },
	};

	@Bean
	public CommandLineRunner initData(MemberRepository memberRepository) {
		return args -> {
			Set<String> authorisedEmails = Arrays.stream(STAFF_ACCOUNTS)
					.map(acc -> acc[1])
					.collect(Collectors.toSet());

			// Remove any DB accounts that are no longer in the list. -WeiqiWang
			List<Member> allMembers = memberRepository.findAll();
			for (Member m : allMembers) {
				if (!authorisedEmails.contains(m.getEmail())) {
					memberRepository.delete(m);
				}
			}

			// Create accounts that are in the list but not yet in the DB. -WeiqiWang
			for (String[] acc : STAFF_ACCOUNTS) {
				if (memberRepository.findByEmail(acc[1]).isEmpty()) {
					Member m = new Member();
					m.setName(acc[0]);
					m.setEmail(acc[1]);
					m.setPassword(acc[2]);
					m.setTotalOrders(0);
					memberRepository.save(m);
				}
			}
		};
	}
}
