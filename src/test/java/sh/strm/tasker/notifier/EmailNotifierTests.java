package sh.strm.tasker.notifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import sh.strm.tasker.TaskConfiguration;
import sh.strm.tasker.notifier.NotifierAllTests.CustomTestYamlInitialization;
import sh.strm.tasker.notify.EmailNotifier;
import sh.strm.tasker.runner.DockerTaskRunner;
import sh.strm.tasker.runner.TaskExecutionResult;
import sh.strm.tasker.task.DockerTask;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = CustomTestYamlInitialization.class)
public class EmailNotifierTests {

	@Autowired
	private TaskConfiguration conf;

	@Autowired
	private DockerTaskRunner dockerRunner;

	private GreenMail server;

	@Before
	public void testSmtpInit() {
		this.server = new GreenMail(ServerSetupTest.SMTP);
		this.server.setUser("test@server.local", "test@server.local", "test");
		this.server.start();
	}

	@After
	public void cleanup() {
		this.server.stop();
	}

	@Test
	public void testEmailNotification() throws MessagingException {
		String taskName = "helloNotifyEmail";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		MimeMessage message = checkEmailNotification(taskName, "green bar");

		assertThat(getFrom(message)).isEqualTo(notifier.getSender());
		assertThat(message.getSubject()).isEqualTo(notifier.getSubject());

		assertThat(message.getAllRecipients()).containsAll(getReceipients(notifier));

		assertThat(GreenMailUtil.getBody(message)).isEqualTo(notifier.getContent());
	}

	@Test
	public void testEmailNotificationMultipleRecipients() throws MessagingException {
		String taskName = "helloNotifyEmailMultipleRecipients";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		MimeMessage message = checkEmailNotification(taskName, "green bar");

		assertThat(getFrom(message)).isEqualTo(notifier.getSender());
		assertThat(message.getSubject()).isEqualTo(notifier.getSubject());

		assertThat(message.getAllRecipients()).containsAll(getReceipients(notifier));

		assertThat(GreenMailUtil.getBody(message)).isEqualTo(notifier.getContent());
	}

	@Test
	public void testEmailNotificationMultilineContent() throws MessagingException, IOException {
		String taskName = "helloNotifyEmailMultilineContent";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		MimeMessage message = checkEmailNotification(taskName, "green bar");

		assertThat(getFrom(message)).isEqualTo(notifier.getSender());
		assertThat(message.getSubject()).isEqualTo(notifier.getSubject());

		assertThat(message.getAllRecipients()).containsAll(getReceipients(notifier));

		assertThat(getMesssageContent(message)).isEqualTo(notifier.getContent());
	}

	@Test
	public void testEmailNotificationWithoutSender() throws MessagingException, IOException {
		String taskName = "helloNotifyEmailWithoutSender";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		MimeMessage message = checkEmailNotification(taskName, "green bar");

		assertThat(getFrom(message)).isEqualTo(notifier.getUsername());
		assertThat(message.getSubject()).isEqualTo(notifier.getSubject());

		assertThat(message.getAllRecipients()).containsAll(getReceipients(notifier));

		assertThat(getMesssageContent(message)).isEqualTo(notifier.getContent());
	}

	@Test
	public void testEmailNotificationWithoutSenderUnit() throws MessagingException, IOException {
		EmailNotifier emailNotifier = new EmailNotifier();
		String username = "username@domain.com";
		String sender = "sender@domain.com";

		// No sender
		assertThat(emailNotifier.getSender()).isNull();

		// Set the username, as there is no sender, use username as sender
		emailNotifier.setUsername(username);
		assertThat(emailNotifier.getSender()).isEqualTo(username);

		// Set the sender, and assert that it is used as a sender
		emailNotifier.setSender(sender);
		assertThat(emailNotifier.getSender()).isEqualTo(sender);

		// Set the username and expect the sender to not change
		emailNotifier.setUsername(username);
		assertThat(emailNotifier.getSender()).isEqualTo(sender);
	}

	@Test
	public void testEmailNotificationTemplate() throws MessagingException {
		String taskName = "helloNotifyEmailTemplate";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		MimeMessage message = checkEmailNotification(taskName, "green bar");

		assertThat(getFrom(message)).isEqualTo(notifier.getSender());
		assertThat(message.getSubject()).isEqualTo(notifier.getSubject());

		assertThat(message.getAllRecipients()).containsAll(getReceipients(notifier));

		assertThat(GreenMailUtil.getBody(message)).isEqualTo("The task helloNotifyEmailTemplate was successful");
	}

	// Connection

	@Test
	public void testEmailServerTestConnection() throws MessagingException {
		String taskName = "helloNotifyEmail";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		notifier.testConnection();
	}

	@Test(expected = MessagingException.class)
	public void testEmailInvalidEmailServer() throws MessagingException {
		String taskName = "helloNotifyEmailServerConnectionProblem";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		notifier.testConnection();
	}

	@Test(expected = MessagingException.class)
	public void testEmailInvalidUsername() throws MessagingException {
		String taskName = "helloNotifyEmailServerAuthProblem";

		EmailNotifier notifier = getEmailNotifierForTask(taskName);
		notifier.testConnection();
	}

	// Parse

	@Test(expected = IllegalArgumentException.class)
	public void testEmailParseSenderInvalidEmail() {
		EmailNotifier notifier = new EmailNotifier();
		notifier.setSender("invalid sender");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmailParseRecipientEmailInvalidEmailAddress() {
		EmailNotifier notifier = new EmailNotifier();
		notifier.setRecipients(Arrays.asList("invalid sender"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmailParseRecipientEmailInvalidEmailAddressNullList() {
		EmailNotifier notifier = new EmailNotifier();
		notifier.setRecipients(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmailInvalidEmailServerInvalidPortBellow() {
		EmailNotifier notifier = new EmailNotifier();
		notifier.setPort(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmailInvalidEmailServerInvalidPortAbove() {
		EmailNotifier notifier = new EmailNotifier();
		notifier.setPort(0xFFFF + 1);
	}

	//////////////////////////////////////////////////////////////////////////////////
	public MimeMessage checkEmailNotification(String taskName, String expectedOutput) {
		DockerTask task = conf.getDockerTaskByName(taskName);
		TaskExecutionResult result = dockerRunner.execute(task);

		if (expectedOutput != null) {
			assertThat(result.getOutput()).isEqualTo("green bar");
			assertThat(result.isSuccessful()).isTrue();
		} else {
			assertThat(result.isSuccessful()).isFalse();
		}

		MimeMessage[] messages = this.server.getReceivedMessages();
		assertThat(messages.length).isGreaterThan(0);
		MimeMessage message = messages[0];
		assertNotNull(message);

		return message;
	}

	private EmailNotifier getEmailNotifierForTask(String taskName) {
		DockerTask task = conf.getDockerTaskByName(taskName);
		assertThat(task).isNotNull();

		assertThat(task.getNotifiers().size()).isEqualTo(1);
		EmailNotifier notifier = (EmailNotifier) task.getNotifiers().get(0);
		assertNotNull(notifier);
		return notifier;
	}

	private String getFrom(MimeMessage message) throws MessagingException {
		return ((InternetAddress) message.getFrom()[0]).getAddress();
	}

	private List<InternetAddress> getReceipients(EmailNotifier notifier) throws AddressException {
		List<InternetAddress> addresses = new ArrayList<>();
		for (String recipientEmail : notifier.getRecipients()) {
			addresses.add(new InternetAddress(recipientEmail));
		}
		return addresses;
	}

	private String getMesssageContent(MimeMessage message) throws IOException, MessagingException {
		return message.getContent().toString().replaceAll("\r\n", "\n");
	}

}
