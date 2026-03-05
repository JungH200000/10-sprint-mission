package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DiscodeitApplication {
	public static void main(String[] args) {
		SpringApplication.run(DiscodeitApplication.class, args);
		UserRepository userRepository;
		UserStatusRepository userStatusRepository;

	}
}
