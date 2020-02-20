package com.examples.school.controller;

import static com.examples.school.repository.mongo.StudentMongoRepository.SCHOOL_DB_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.examples.school.model.Student;
import com.examples.school.repository.StudentRepository;
import com.examples.school.repository.mongo.StudentMongoRepository;
import com.examples.school.view.StudentView;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * Communicates with a MongoDB server on localhost; start MongoDB with Docker with
 * 
 * <pre>
 * docker run -p 27017:27017 --rm mongo:4.2.3
 * </pre>
 * 
 * @author Lorenzo Bettini
 *
 */
public class SchoolControllerRaceConditionIT {

	@Mock
	private StudentView studentView;

	private StudentRepository studentRepository;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		MongoClient client = new MongoClient("localhost");
		MongoDatabase database = client.getDatabase(SCHOOL_DB_NAME);
		// make sure we always start with a clean database
		database.drop();
		studentRepository = new StudentMongoRepository(client);
	}

	@Test
	public void testNewStudentConcurrent() {
		Student student = new Student("1", "name");
		// start the threads calling newStudent concurrently
		// on different SchoolController instances, so 'synchronized'
		// methods in the controller will not help...
		List<Thread> threads = IntStream.range(0, 10)
			.mapToObj(i -> new Thread(
				() -> 
				new SchoolController(studentView, studentRepository)
					.newStudent(student)))
			.peek(t -> t.start())
			.collect(Collectors.toList());
		// wait for all the threads to finish
		await().atMost(10, SECONDS)
			.until(() -> threads.stream().noneMatch(t -> t.isAlive()));
		// there should be a single element in the list
		assertThat(studentRepository.findAll())
			.containsExactly(student);
	}

}
