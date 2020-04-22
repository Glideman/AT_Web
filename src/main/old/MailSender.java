import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;


public class MailSender {

    String MailHost;
    String MailPort;
    String MailUsr;
    String MailPwd;


    public boolean SendTo(String [] e_mails, String mail_body, String [] files) throws Exception {
        // Recipient's email ID needs to be mentioned.
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", this.MailHost);
        props.put("mail.debug", "false");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", this.MailPort);
        props.put("mail.smtp.socketFactory.port", this.MailPort);
        props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.user", this.MailUsr);
        props.put("mail.password", this.MailPwd);

        // Get the default Session object.
        SmtpAuthenticator authentication = new SmtpAuthenticator(this.MailUsr,this.MailPwd);
        Session session = Session.getDefaultInstance(props,authentication);

        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);
        Multipart multipart = new MimeMultipart();


        // Отправитель
        message.setFrom(new InternetAddress("Autotest"));


        // Получатели
        for(String msgTo : e_mails)
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(msgTo));


        // Заголовок
        message.setSubject("Autotest is done! Reports inside.");


        // Основная часть
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(mail_body);
        multipart.addBodyPart(messageBodyPart);


        // Прикрепляемые файлы
        for(String filen : files) {
            if( !OutputFile.is_file_exist(filen) ) continue;
            BodyPart fileBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(filen);
            fileBodyPart.setDataHandler(new DataHandler(source));
            fileBodyPart.setFileName(filen);
            multipart.addBodyPart(fileBodyPart);
        }


        // Отсыл сообщений
        message.setContent(multipart);
        Transport.send(message);


        return false;
    }

    public void SetAuthProps(String host, String port, String usr, String pwd) {
        this.MailHost = host;
        this.MailPort = port;
        this.MailUsr = usr;
        this.MailPwd = pwd;
    }

}
