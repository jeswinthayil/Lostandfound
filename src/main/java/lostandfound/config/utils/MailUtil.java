package lostandfound.config.utils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;

// inside config:

public class MailUtil {
    private static final MailClient mailClient;

    static {
        MailConfig config = new MailConfig()
                .setHostname("smtp.gmail.com")
                .setPort(587)
                .setStarttls(StartTLSOptions.REQUIRED)
                .setUsername("your_email@gmail.com")       // ✅ change this
                .setPassword("your_app_password_here");    // ✅ use app password

        mailClient = MailClient.createShared(Vertx.vertx(), config, "mailPool");
    }

    public static void sendVerificationEmail(Vertx vertx, String to, String token) {
        String verifyLink = "http://localhost:8888/api/auth/verify/" + token;

        MailMessage message = new MailMessage()
                .setFrom("Lost & Found <your_email@gmail.com>")
                .setTo(to)
                .setSubject("Verify your email")
                .setText("Click the link to verify your email:\n" + verifyLink);

        mailClient.sendMail(message, result -> {
            if (result.succeeded()) {
                System.out.println("Verification email sent to: " + to);
            } else {
                System.out.println("Failed to send email: " + result.cause().getMessage());
            }
        });
    }

    public static void sendContactMessage(Vertx vertx, String to, String from, String itemTitle, String userMessage) {
        MailMessage message = new MailMessage()
                .setFrom("Lost & Found <your_email@gmail.com>")
                .setTo(to)
                .setSubject("Someone responded to your Lost & Found post")
                .setText("Message from: " + from + "\n\n" +
                        "Regarding: " + itemTitle + "\n\n" +
                        "Message:\n" + userMessage);

        mailClient.sendMail(message, result -> {
            if (result.succeeded()) {
                System.out.println("Contact email sent successfully.");
            } else {
                System.err.println("Failed to send contact email: " + result.cause());
            }
        });
    }

}
