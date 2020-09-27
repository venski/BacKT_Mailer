//@file:Suppress("unused")

package azadev.backt.mailer

import java.util.*
import javax.mail.*
import javax.mail.internet.*
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication


object Mailer
{
	var debug = true

	var connectionTimeout = 1000*60
	var ioTimeout = 1000*60*10


	fun send(
			host: String,
			port: Int,
			user: String,
			pass: String,
			to: String,
			subject: String,
			body_text: String? = null,
			body_html: String? = null
	) {
		val session = getSession(user, pass, "smtps")

		val msg = MimeMessage(session)
		msg.setFrom(InternetAddress(user))
		msg.setRecipients(Message.RecipientType.TO, to)
		msg.subject = subject
		msg.sentDate = Date()

		when {
			body_text != null && body_html != null -> {
				val multipart = MimeMultipart("alternative")
				multipart.addBodyPart(MimeBodyPart().apply { setContent(body_text, "text/plain; charset=utf-8") })
				multipart.addBodyPart(MimeBodyPart().apply { setContent(body_html, "text/html; charset=utf-8") })
				msg.setContent(multipart)
			}
			body_text != null -> msg.setContent(body_text, "text/plain; charset=utf-8")
			body_html != null -> msg.setContent(body_html, "text/html; charset=utf-8")
		}

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
		val session = getSession(user, pass, "imaps")

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
		val session = getSession(user, pass, "imaps")

		val store = session.getStore("imaps")
		store.connect(host, port, user, pass)

		val folder = store.getFolder(folderName)
		if (!folder.exists() && createIfNotExists)
			folder.create(createWithType)

		folder.open(Folder.READ_WRITE)
		folder.appendMessages(arrayOf(message))

		store.close()
	}


	private fun getSession(user: String, pass: String, protocol: String): Session {
		val props = Properties()

		if (debug)
			props.put("mail.debug", debug)

		props.put("mail.$protocol.connectiontimeout", connectionTimeout)
		props.put("mail.$protocol.timeout", ioTimeout)

		return Session.getInstance(props, object : Authenticator() {
			override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
		})
	}
}
