package azadev.backt.mailer

import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object Mailer
{
	var debug = true


	fun send(
			host: String,
			port: Int,
			user: String,
			pass: String,
			to: String,
			subject: String,
			body_text: String
			/*body_html: String
			bodyParts: List*/
	) {
		val session = getSession(user, pass)

		val msg = MimeMessage(session)
		msg.setFrom(InternetAddress(user))
		msg.setRecipients(Message.RecipientType.TO, to)
		msg.subject = subject
		msg.sentDate = Date()

		msg.setText(body_text, Charsets.UTF_8.toString())

		val transport = session.getTransport("smtps")
		transport.connect(host, port, user, pass)
		transport.sendMessage(msg, msg.allRecipients)
		transport.close()
	}

	fun receive(
			host: String,
			port: Int,
			user: String,
			pass: String,
			folderName: String = "INBOX",
			limit: Int = Int.MAX_VALUE
	): Array<out Message> {
		val session = getSession(user, pass)

		val store = session.getStore("imaps")
		store.connect(host, port, user, pass)

		val folder = store.getFolder(folderName)
		folder.open(Folder.READ_ONLY)

		val count = folder.messageCount
		return folder.getMessages(Math.max(count-limit+1, 1), count)
	}


	fun appendMessageToFolder(
			host: String,
			port: Int,
			user: String,
			pass: String,
			message: Message,
			folderName: String,
			createIfNotExists: Boolean = false,
			createWithType: Int = Folder.HOLDS_FOLDERS
	) {
		val session = getSession(user, pass)

		val store = session.getStore("imaps")
		store.connect(host, port, user, pass)

		val folder = store.getFolder(folderName)
		if (!folder.exists() && createIfNotExists)
			folder.create(createWithType)

		folder.open(Folder.READ_WRITE)
		folder.appendMessages(arrayOf(message))

		store.close()
	}


	private fun getSession(user: String, pass: String): Session {
		val props = Properties()

		if (debug)
			props.put("mail.debug", debug)

		return Session.getInstance(props, object : Authenticator() {
			override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
		})
	}
}
