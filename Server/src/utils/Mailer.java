package utils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Mailer {

        public static void sendMail(String to, String subject, String body, String sender) {

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", "127.0.0.1");

            // Get the default Session object.
            Session session = Session.getDefaultInstance(properties);

            // Send the message.
            try {
                javax.mail.Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(sender));
                message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
                message.setSubject("Invite to join group");
                message.setText("You have been invited to join a group. Please check your account to accept.");

                // Send message
                Transport.send(message);
                System.out.println("Sent message successfully....");
            } catch (MessagingException mex) {
                mex.printStackTrace();
            }
        }
}
