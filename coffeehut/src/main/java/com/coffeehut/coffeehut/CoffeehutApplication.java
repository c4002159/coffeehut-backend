// CoffeehutApplication.java — App entry point and seed data -WeiqiWang

package com.coffeehut.coffeehut;

import com.coffeehut.coffeehut.model.ScheduleHours;
import com.coffeehut.coffeehut.model.StaffAccount;
import com.coffeehut.coffeehut.model.StoreSettings;
import com.coffeehut.coffeehut.repository.ScheduleHoursRepository;
import com.coffeehut.coffeehut.repository.StaffAccountRepository;
import com.coffeehut.coffeehut.repository.StoreSettingsRepository;
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

    private static final String[][] STAFF_ACCOUNTS = {
        { "StaffUser",  "staff@coffeehut.com", "123456"   },
        { "Weiqi Wang", "weiqi@coffeehut.com", "123456"   },
        { "Manager",    "admin@coffeehut.com",  "admin123" },
    };

    @Bean
    public CommandLineRunner initData(
            StaffAccountRepository    staffAccountRepository,
            StoreSettingsRepository   storeSettingsRepository,
            ScheduleHoursRepository   scheduleHoursRepository) {

        return args -> {

            // ── Staff accounts ──────────────────────────────────────────────
            Set<String> authorisedEmails = Arrays.stream(STAFF_ACCOUNTS)
                    .map(acc -> acc[1]).collect(Collectors.toSet());

            for (StaffAccount a : staffAccountRepository.findAll()) {
                if (!authorisedEmails.contains(a.getEmail()))
                    staffAccountRepository.delete(a);
            }
            for (String[] acc : STAFF_ACCOUNTS) {
                if (staffAccountRepository.findByEmail(acc[1]).isEmpty()) {
                    StaffAccount a = new StaffAccount();
                    a.setName(acc[0]); a.setEmail(acc[1]); a.setPassword(acc[2]);
                    staffAccountRepository.save(a);
                }
            }

            // ── Store settings (singleton id = 1) ───────────────────────────
            if (storeSettingsRepository.findById(1L).isEmpty()) {
                StoreSettings defaults = new StoreSettings();
                defaults.setId(1L);
                defaults.setAutoCancelEnabled(true);
                defaults.setAutoCancelMins(15);
                defaults.setAutoCollectEnabled(true);
                defaults.setAutoCollectMins(15);
                defaults.setIsTemporarilyClosed(false);
                storeSettingsRepository.save(defaults);
            }

            // ── Weekly schedule defaults (only insert if table is empty) ────
            // Staff can edit these via the Schedule page; we never overwrite. -WeiqiWang
            if (scheduleHoursRepository.count() == 0) {
                scheduleHoursRepository.saveAll(List.of(
                    makeHours(1L, "Monday - Friday", "9:00 AM", "6:00 PM", false),
                    makeHours(2L, "Saturday",         "10:00 AM","4:00 PM", false),
                    makeHours(3L, "Sunday",            null,      null,      true)
                ));
            }
        };
    }

    private ScheduleHours makeHours(Long id, String label, String open, String close, boolean closed) {
        ScheduleHours h = new ScheduleHours();
        h.setId(id);
        h.setDayLabel(label);
        h.setOpenTime(open);
        h.setCloseTime(close);
        h.setIsClosed(closed);
        return h;
    }
}
