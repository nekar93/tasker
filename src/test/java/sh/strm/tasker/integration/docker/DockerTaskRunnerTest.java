package sh.strm.tasker.integration.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import sh.strm.tasker.TaskConfiguration;
import sh.strm.tasker.runner.DockerTaskRunner;
import sh.strm.tasker.runner.TaskExecutionResult;
import sh.strm.tasker.task.DockerTask;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DockerTaskRunnerTest {

	@Autowired
	private TaskConfiguration conf;

	@Autowired
	private DockerTaskRunner dockerRunner;

	@Test
	public void testDockerRunContainerAlwaysPull() throws Exception {
		DockerTask task = conf.getDockerTaskByName("helloAlwaysPull");
		TaskExecutionResult result = dockerRunner.executeTask(task);
		assertTrue(task.isAlwaysPull());
		assertEquals("green bar", result.getOutput());
	}

	@Test
	public void testDockerRunContainer() throws Exception {
		DockerTask task = conf.getDockerTaskByName("hello");
		TaskExecutionResult result = dockerRunner.executeTask(task);
		assertEquals("green bar", result.getOutput());
	}

	@Test
	public void testDockerRunContainerScript() throws Exception {
		DockerTask task = conf.getDockerTaskByName("helloScript");
		TaskExecutionResult result = dockerRunner.executeTask(task);
		assertEquals("green bar\ngreen barbar", result.getOutput());
	}

	@Test
	public void testDockerRunContainerScriptPipe() throws Exception {
		DockerTask task = conf.getDockerTaskByName("helloScriptPipe");
		TaskExecutionResult result = dockerRunner.executeTask(task);
		assertEquals("green bar\ngreen barbar", result.getOutput());
	}

	@Test
	public void testDockerRunContainerScriptStrict() throws Exception {
		DockerTask task = conf.getDockerTaskByName("helloScriptStrict");
		assertTrue(task.isScriptStrict());
		TaskExecutionResult result = dockerRunner.executeTask(task);
		assertEquals("green bar", result.getOutput());
	}

	@Test
	public void testDontAcceptDuplicatedTaskName() {
		try {
			DockerTask task1 = new DockerTask();
			task1.setName("task01");

			DockerTask task2 = new DockerTask();
			task2.setName("task02");

			TaskConfiguration config = new TaskConfiguration();

			config.setDocker(Arrays.asList(new DockerTask[] { task1, task2 }));

			// Must fail to add this;
			config.setDocker(Arrays.asList(new DockerTask[] { task1, task2, task2 }));

			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

}