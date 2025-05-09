package org.kickmyb.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.kickmyb.transfer.HomeItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.*;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {

    @Autowired
    private MUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTask serviceTask;

    @Test
    void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);

        assertEquals(1, serviceTask.home(u.id).size());
    }

    @Test
    void testAddTaskEmpty()  {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Empty");
        } catch (Exception e) {
            assertEquals(ServiceTask.Empty.class, e.getClass());
        }
    }

    @Test
    void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "o";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.TooShort");
        } catch (Exception e) {
            assertEquals(ServiceTask.TooShort.class, e.getClass());
        }
    }

    @Test
    void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Bonne tâche";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Existing");
        } catch (Exception e) {
            assertEquals(ServiceTask.Existing.class, e.getClass());
        }
    }

    @Test
    public void testDeleteTaskWithCorrectId() {
        MUser user = new MUser();
        user.username = "Alice";
        user.password = "password";
        userRepository.save(user);
        AddTaskRequest atr = new AddTaskRequest();
        atr.name ="Test Task";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try {
            serviceTask.addOne(atr, user);
        } catch (ServiceTask.Empty | ServiceTask.TooShort | ServiceTask.Existing e) {
            fail("Exception should not be thrown");
        }

        MUser retrievedUser = userRepository.findById(user.id).orElse(null);
        assertNotNull(retrievedUser, "User should exist in the database");
        List<HomeItemResponse> tasks = serviceTask.home(retrievedUser.id);
        assertEquals(1, tasks.size(), "Task should be added");

        long taskId = tasks.get(0).id;
        serviceTask.deleteOne(taskId, retrievedUser);
        List<HomeItemResponse> updatedTasks = serviceTask.home(retrievedUser.id);
        assertEquals(0, updatedTasks.size(), "Task should be deleted");

    }

    @Test
    public void testDeleteTaskWithWrongId() {
        MUser user = new MUser();
        user.username = "Alice";
        user.password = "password";
        userRepository.save(user);
        AddTaskRequest atr = new AddTaskRequest();
        atr.name ="Test Task";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try {
            serviceTask.addOne(atr, user);
        } catch (ServiceTask.Empty | ServiceTask.TooShort | ServiceTask.Existing e) {
            fail("Exception should not be thrown");
        }

        MUser retrievedUser = userRepository.findById(user.id).orElse(null);
        assertNotNull(retrievedUser, "User should exist in the database");
        List<HomeItemResponse> tasks = serviceTask.home(retrievedUser.id);
        assertEquals(1, tasks.size(), "Task should be added");

        long taskId = tasks.get(0).id;

        try {
            serviceTask.deleteOne(taskId + 1, retrievedUser);
        }
        catch (IllegalArgumentException e)
        {

        }
        List<HomeItemResponse> updatedTasks = serviceTask.home(retrievedUser.id);
        assertEquals(1, updatedTasks.size(), "Task should be deleted");

    }


    @Test
    public void testDeleteTaskWithWrongUser() {
        MUser user = new MUser();
        user.username = "Alice";
        user.password = "password";
        userRepository.save(user);

        MUser wrongUser = new MUser();
        wrongUser.username = "Bob"; // Fixed: Set username on wrongUser, not user
        wrongUser.password = passwordEncoder.encode("password"); // Fixed: Encode password
        userRepository.saveAndFlush(wrongUser);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Test Task";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try {
            serviceTask.addOne(atr, user);
        } catch (ServiceTask.Empty | ServiceTask.TooShort | ServiceTask.Existing e) {
            fail("Exception should not be thrown");
        }

        MUser retrievedUser = userRepository.findById(user.id)
                .orElseThrow(() -> new AssertionError("User should exist in the database"));
        List<HomeItemResponse> tasks = serviceTask.home(retrievedUser.id);
        assertEquals(1, tasks.size(), "Task should be added");

        MUser retrievedBob = userRepository.findById(wrongUser.id)
                .orElseThrow(() -> new AssertionError("Bob should exist in the database"));

        long taskId = tasks.get(0).id;
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            serviceTask.deleteOne(taskId, retrievedBob);
        });
        assertEquals("Task with id " + taskId + " not found in user tasks", exception.getMessage(),
                "Bob should not be able to delete Alice's task");

        List<HomeItemResponse> updatedTasks = serviceTask.home(retrievedUser.id);
        assertEquals(1, updatedTasks.size(), "Task should not be deleted");
    }




}
