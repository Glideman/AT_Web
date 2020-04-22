import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Properties;

public class moduleMail extends moduleDefault {


    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = moduleMail.class.getDeclaredMethods();
        this.methodMap = new HashMap<>();

        System.out.print("Init module class \"" + this.name + "\" with methods");

        for(Method m : m_list) {
            m.setAccessible(true);
            methodMap.put(m.getName(),m);

            System.out.print("\n  " + m.getName() + " :");

            for( Class param : m.getParameterTypes()) {
                System.out.print(" " + param.getName());
            }
        }

        System.out.print("\n\n");
    }


    moduleMail() {
        this.name = "mail";
    }


    public boolean SendMail(String mailBody, String mailFiles) throws Exception {


        // Пропсы почты из настроек
        String mailHeader = webInterface.GetProp("mail_header");
        String mailFrom = webInterface.GetProp("mail_from");
        String mailTo = webInterface.GetProp("mail_to");
        String mailCopy = webInterface.GetProp("mail_copy");
        String mailHost = webInterface.GetProp("smtp_host");
        String mailPort = webInterface.GetProp("smtp_port");
        String mailUsr = webInterface.GetProp("smtp_user");
        String mailPwd = webInterface.GetProp("smtp_pass");


        // Если получателей нет - ошибка
        if(mailTo.isEmpty()) return true;


        // Пропсы для рассылки
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailHost);
        props.put("mail.debug", "false");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", mailPort);
        props.put("mail.smtp.socketFactory.port", mailPort);
        props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.user", mailUsr);
        props.put("mail.password", mailPwd);


        // Get the default Session object.
        atwebSmtpAuthenticator authentication = new atwebSmtpAuthenticator(mailUsr,mailPwd);
        Session session = Session.getDefaultInstance(props,authentication);


        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);
        Multipart multipart = new MimeMultipart();


        // Отправитель
        if(!mailFrom.isEmpty())
            message.setFrom(new InternetAddress(mailFrom));


        // Получатели
        String [] mailToArr = mailTo.split(";");
        for(String mTo : mailToArr)
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(mTo));


        // Копия
        if(!mailCopy.isEmpty()) {
            String[] mailCopyArr = mailCopy.split(";");
            for (String mCopy : mailCopyArr)
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(mCopy));
        }


        // Заголовок
        if(!mailHeader.isEmpty())
            message.setSubject(mailHeader);


        // Основная часть
        if(!mailBody.isEmpty()) {
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(mailBody);
            multipart.addBodyPart(messageBodyPart);
        }


        // Прикрепляемые файлы
        if(!mailFiles.isEmpty()) {
            String [] mailFilesArr = mailCopy.split(";");
            for (String mFile : mailFilesArr) {
                if (!moduleFile.IsFileExist(mFile)) continue;
                BodyPart fileBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(mFile);
                fileBodyPart.setDataHandler(new DataHandler(source));
                fileBodyPart.setFileName(mFile);
                multipart.addBodyPart(fileBodyPart);
            }
        }


        // Отсыл сообщений
        message.setContent(multipart);
        Transport.send(message);


        return false;
    }

}
