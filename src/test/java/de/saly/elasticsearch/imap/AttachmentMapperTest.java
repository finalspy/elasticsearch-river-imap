package de.saly.elasticsearch.imap;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.river.RiverSettings;
import org.junit.Assert;
import org.junit.Test;

import de.saly.elasticsearch.support.IMAPUtils;

public class AttachmentMapperTest extends AbstractIMAPRiverUnitTest{

	@Test
	public void testAttachments() throws Exception{

		final RiverSettings settings = riverSettings("/river-imap-attachments.json");

		final Properties props = new Properties();
		final String user = XContentMapValues.nodeStringValue(settings.settings().get("user"), null);
		final String password = XContentMapValues.nodeStringValue(settings.settings().get("password"), null);

		for (final Map.Entry<String, Object> entry : settings.settings().entrySet()) {

			if (entry != null && entry.getKey().startsWith("mail.")) {
				props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}

		registerRiver("imap_river", "river-imap-attachments.json");

		final Session session = Session.getInstance(props);
		final Store store = session.getStore();
		store.connect(user, password);
		checkStoreForTestConnection(store);
		final Folder inbox = store.getFolder("INBOX");
		inbox.open(Folder.READ_WRITE);



		final MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(EMAIL_TO));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_USER_ADDRESS));
		message.setSubject(EMAIL_SUBJECT + "::attachment test");
		message.setSentDate(new Date());

		BodyPart bp = new MimeBodyPart();
		bp.setText("Text");
		Multipart mp = new MimeMultipart();
		mp.addBodyPart(bp);

		bp = new MimeBodyPart();
		DataSource ds = new ByteArrayDataSource(this.getClass().getResourceAsStream("/httpclient-tutorial.pdf"), "application/pdf");
		bp.setDataHandler(new DataHandler(ds));
		bp.setFileName("httpclient-tutorial.pdf");
		mp.addBodyPart(bp);
		message.setContent(mp);

		inbox.appendMessages(new Message[]{message});
		IMAPUtils.close(inbox);
		IMAPUtils.close(store);

		//let the river index
		Thread.sleep(20*1000);

		esSetup.client().admin().indices().refresh(new RefreshRequest()).actionGet();


		SearchResponse get =  esSetup.client().prepareSearch("imapriverdata").setTypes("mail").execute().actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());

		//BASE64 content httpclient-tutorial.pdf
		Assert.assertTrue(get.getHits().hits()[0].getSourceAsString().contains("JVBERi0xLjQKJaqrrK0KNCAwIG9iag"));

		get =  esSetup.client().prepareSearch("imapriverdata").addFields("attachments.content").setTypes("mail").setQuery(QueryBuilders.matchQuery("attachments.content", "wrapping")).execute().actionGet();//search(new SearchRequest("imapriverdata").types("mail")).actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());
		Assert.assertEquals(1, get.getHits().hits()[0].field("attachments.content").getValues().size());
		Assert.assertTrue(get.getHits().hits()[0].field("attachments.content").getValue().toString().contains("wrapping"));

	}



	@Test
	public void testAttachments2() throws Exception{

		final RiverSettings settings = riverSettings("/river-imap-attachments-2.json");

		final Properties props = new Properties();
		final String user = XContentMapValues.nodeStringValue(settings.settings().get("user"), null);
		final String password = XContentMapValues.nodeStringValue(settings.settings().get("password"), null);

		for (final Map.Entry<String, Object> entry : settings.settings().entrySet()) {

			if (entry != null && entry.getKey().startsWith("mail.")) {
				props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}

		registerRiver("imap_river", "river-imap-attachments-2.json");

		final Session session = Session.getInstance(props);
		final Store store = session.getStore();
		store.connect(user, password);
		checkStoreForTestConnection(store);
		final Folder inbox = store.getFolder("INBOX");
		inbox.open(Folder.READ_WRITE);

		final MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(EMAIL_TO));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_USER_ADDRESS));
		message.setSubject(EMAIL_SUBJECT + "::attachment test");
		message.setSentDate(new Date());

		BodyPart bp = new MimeBodyPart();
		bp.setText("Text");
		Multipart mp = new MimeMultipart();
		mp.addBodyPart(bp);

		bp = new MimeBodyPart();
		DataSource ds = new ByteArrayDataSource(this.getClass().getResourceAsStream("/httpclient-tutorial.pdf"), "application/pdf");
		bp.setDataHandler(new DataHandler(ds));
		bp.setFileName("httpclient-tutorial.pdf");
		mp.addBodyPart(bp);
		message.setContent(mp);


		bp = new MimeBodyPart();
		ds = new ByteArrayDataSource(this.getClass().getResourceAsStream("/testWORD.docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		bp.setDataHandler(new DataHandler(ds));
		bp.setFileName("testWORD.docx");
		mp.addBodyPart(bp);
		message.setContent(mp);

		inbox.appendMessages(new Message[]{message});
		IMAPUtils.close(inbox);
		IMAPUtils.close(store);


		//let the river index
		Thread.sleep(20*1000);

		esSetup.client().admin().indices().refresh(new RefreshRequest()).actionGet();

		SearchResponse get =  esSetup.client().prepareSearch("imapriverdata").setTypes("mail").execute().actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());

		//BASE64 content httpclient-tutorial.pdf
		Assert.assertTrue(get.getHits().hits()[0].getSourceAsString().contains("JVBERi0xLjQKJaqrrK0KNCAwIG9iag"));

		//BASE64 content testWORD.docx
		Assert.assertTrue(get.getHits().hits()[0].getSourceAsString().contains("UEsDBAoAAAAAAHqEbD0AAAAAAAAAAAAAAAAGABwAX"));

		get =  esSetup.client().prepareSearch("imapriverdata").addFields("attachments.content").setTypes("mail").setQuery(QueryBuilders.matchQuery("attachments.content", "wrapping")).execute().actionGet();//search(new SearchRequest("imapriverdata").types("mail")).actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());
		Assert.assertEquals(2, get.getHits().hits()[0].field("attachments.content").getValues().size());

		//first value is httpclient-tutorial.pdf
		Assert.assertTrue(get.getHits().hits()[0].field("attachments.content").getValue().toString().contains("wrapping"));

		get =  esSetup.client().prepareSearch("imapriverdata").addFields("attachments.content").setTypes("mail").setQuery(QueryBuilders.matchQuery("attachments.content", "This paragraph is in the default text style")).execute().actionGet();//search(new SearchRequest("imapriverdata").types("mail")).actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());
		Assert.assertEquals(2, get.getHits().hits()[0].field("attachments.content").getValues().size());

		//second value is testWORD.docx
		Assert.assertTrue(get.getHits().hits()[0].field("attachments.content").getValues().get(1).toString().contains("This paragraph is in the default text style"));

	}

	@Test
	public void testAttachments3() throws Exception{

		final RiverSettings settings = riverSettings("/river-imap-attachments-3.json");

		final Properties props = new Properties();
		final String user = XContentMapValues.nodeStringValue(settings.settings().get("user"), null);
		final String password = XContentMapValues.nodeStringValue(settings.settings().get("password"), null);

		for (final Map.Entry<String, Object> entry : settings.settings().entrySet()) {

			if (entry != null && entry.getKey().startsWith("mail.")) {
				props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}

		registerRiver("imap_river", "river-imap-attachments-3.json");

		final Session session = Session.getInstance(props);
		final Store store = session.getStore();
		store.connect(user, password);
		checkStoreForTestConnection(store);
		final Folder inbox = store.getFolder("INBOX");
		inbox.open(Folder.READ_WRITE);

		final MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(EMAIL_TO));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_USER_ADDRESS));
		message.setSubject(EMAIL_SUBJECT + "::attachment test");
		message.setSentDate(new Date());

		BodyPart bp = new MimeBodyPart();
		bp.setText("Text");
		Multipart mp = new MimeMultipart();
		mp.addBodyPart(bp);

		bp = new MimeBodyPart();
		DataSource ds = new ByteArrayDataSource(this.getClass().getResourceAsStream("/httpclient-tutorial.pdf"), "application/pdf");
		bp.setDataHandler(new DataHandler(ds));
		bp.setFileName("httpclient-tutorial.pdf");
		mp.addBodyPart(bp);
		message.setContent(mp);


		bp = new MimeBodyPart();
		ds = new ByteArrayDataSource(this.getClass().getResourceAsStream("/testWORD.docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		bp.setDataHandler(new DataHandler(ds));
		bp.setFileName("testWORD.docx");
		mp.addBodyPart(bp);
		message.setContent(mp);

		inbox.appendMessages(new Message[]{message});
		IMAPUtils.close(inbox);
		IMAPUtils.close(store);


		//let the river index
		Thread.sleep(20*1000);

		esSetup.client().admin().indices().refresh(new RefreshRequest()).actionGet();

		SearchResponse get =  esSetup.client().prepareSearch("imapriverdata").setTypes("mail").execute().actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());

		//BASE64 content httpclient-tutorial.pdf
		Assert.assertTrue(get.getHits().hits()[0].getSourceAsString().contains("JVBERi0xLjQKJaqrrK0KNCAwIG9iag"));

		//BASE64 content testWORD.docx
		Assert.assertTrue(get.getHits().hits()[0].getSourceAsString().contains("UEsDBAoAAAAAAHqEbD0AAAAAAAAAAAAAAAAGABwAX"));

		get =  esSetup.client().prepareSearch("imapriverdata").addFields("attachments.content").setTypes("mail").setQuery(QueryBuilders.matchQuery("attachments.content", "wrapping")).execute().actionGet();//search(new SearchRequest("imapriverdata").types("mail")).actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());
		Assert.assertEquals(2, get.getHits().hits()[0].field("attachments.content").getValues().size());

		//first value is httpclient-tutorial.pdf
		Assert.assertTrue(get.getHits().hits()[0].field("attachments.content").getValue().toString().contains("wrapping"));

		get =  esSetup.client().prepareSearch("imapriverdata").addFields("attachments.content").setTypes("mail").setQuery(QueryBuilders.matchQuery("attachments.content", "This paragraph is in the default text style")).execute().actionGet();//search(new SearchRequest("imapriverdata").types("mail")).actionGet();
		Assert.assertEquals(1, get.getHits().totalHits());
		Assert.assertEquals(2, get.getHits().hits()[0].field("attachments.content").getValues().size());

		//second value is testWORD.docx
		Assert.assertTrue(get.getHits().hits()[0].field("attachments.content").getValues().get(1).toString().contains("This paragraph is in the default text style"));

	}

}
