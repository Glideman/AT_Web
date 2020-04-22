public class AT_Main {

    public static void main(String[] args) throws Exception {
        //boolean isBackup = false;

        //if( args != null ) for(String qwe : args) {
        //    if(qwe.equals("-backup")) isBackup = true;
        //}

        // вэб интерфейс
        AT_WebInterface webInterface = new AT_WebInterface();


        // класс для бэкапа
        //OutputFile backup = new OutputFile();


        // класс для карты
        //OutputFile sitemap = new OutputFile();


        //if(!isBackup) {
        //    backup.bk_get(Jumper,"url.test.txt");
        //    return;
        //}


        // создание файла настроек, если его нет
        /*if (!OutputFile.is_file_exist("robot.properties")) {
            OutputFile NewProps = new OutputFile();
            NewProps.new_file("robot.properties");
            NewProps.push_back("" +
                    "# props for test robot\r\n" +
                    "get_params_as_new_pages=true\r\n" +
                    "export_sub_pages_in_file=false\r\n" +
                    "test_out_of_homep_urls=false\r\n" +
                    "print_in_console=false\r\n" +
                    "print_subp_in_console=false\r\n" +
                    "\r\n" +
                    "# list of sites or pages to test\r\n" +
                    "sites=\r\n" +
                    "\r\n" +
                    "# list of emails for sending reports\r\n" +
                    "emails=\r\n" +
                    "\r\n" +
                    "# driver path\r\n" +
                    "drv=\r\n" +
                    "\r\n" +
                    "# mail auth props\r\n" +
                    "host=\r\n" +
                    "port=\r\n" +
                    "user=\r\n" +
                    "pass=\r\n");
        }*/


        //Properties RobotProps = new Properties();
        //RobotProps.load(new FileInputStream("robot.properties"));


        /*String[] EMails = null;
        String[] Sites = null;
        if(!isBackup) {
            // параметры теста
            Jumper.get_params_as_new_pages = RobotProps.get("get_params_as_new_pages").toString().compareTo("true") == 0;
            Jumper.export_sub_pages_in_file = RobotProps.get("export_sub_pages_in_file").toString().compareTo("true") == 0;
            Jumper.test_out_of_homep_urls = RobotProps.get("test_out_of_homep_urls").toString().compareTo("true") == 0;
            Jumper.print_in_console = RobotProps.get("print_in_console").toString().compareTo("true") == 0;
            Jumper.print_subp_in_console = RobotProps.get("print_subp_in_console").toString().compareTo("true") == 0;


            // список сайтов
            if (!RobotProps.get("sites").toString().equals(""))
                Sites = RobotProps.get("sites").toString().split(";");
            else {
                System.out.println("ОШИБКА! Список сайтов пуст! Пожалуйста, добавьте сайты в robot.properties.");
                return;
            }

        } else {
            backup.bk_get(Jumper,"url.test.backup.txt");
            Sites = new String[1];
            Sites[0] = Jumper.GetStartPage();
        }*/


        // список майлов
        /*if (!RobotProps.get("emails").toString().equals(""))
            EMails = RobotProps.get("emails").toString().split(";");
        else
            System.out.println("ПРЕДУПРЕЖДЕНИЕ! Список почтовых адресов пуст! Пожалуйста, добавьте сайты в robot.properties, если хотите получить отчёты на почту.");


        // Параметры аутентификации
        String MailHost = RobotProps.get("host").toString();
        String MailPort = RobotProps.get("port").toString();
        String MailUsr = RobotProps.get("user").toString();
        String MailPwd = RobotProps.get("pass").toString();
        String DrvPath = RobotProps.get("drv").toString();
*/

        // проверка параметров
        /*if((MailHost.equals("") || MailPort.equals("") || MailUsr.equals("") || MailPwd.equals("")) && (EMails != null)) {
            System.out.println("ОШИБКА! Не указан один или несколько параметров авторизации для сервера рассылки почты. Пожалуйста, отредактируйте robot.properties.");
            return;
        }


        if(DrvPath.equals("")) {
            System.out.println("ОШИБКА! Не указан файл драйвера браузера. Пожалуйста, отредактируйте robot.properties.");
            return;
        }*/


        // Текст сообщения в письме
        /*String MailBody = "";


        // прикрепляемые файлы
        List<String> Files = new ArrayList<String> ();*/


        // результат тестирования
        boolean Result = false;


        webInterface.SetProp("using_selenium","true");
        webInterface.SetProp("driver_path","chromedriver.exe");

        // инициация
        try {
            webInterface.Init();
        } catch(Exception e) {
            System.out.println("ОШИБКА! Ошибка в AT_WebInterface.Init()!");
            e.printStackTrace();
            return;
        }


        AT_TestModule m_test = new AT_TestModule();
        m_Jumper m_jumper = new m_Jumper();


        webInterface.RegisterModule(m_test);
        webInterface.RegisterModule(m_jumper);


        webInterface.RunModules();


        // переход на сайт(ы)
        /*for( String Site : Sites ) {
            try {
                Result |= Jumper.Start(Site);
            } catch(Exception e) {
                System.out.println("ОШИБКА! Ошибка при переходе на страницу!");
                e.printStackTrace();
                backup.bk_dump(Jumper,"url.test.backup.txt");
                Jumper.Destroy();
                return;
            }
            Files.add(Jumper.GetAllPgFile());
            Files.add(Jumper.GetErrPgFile());
            MailBody = MailBody + Jumper.GetTestResults() + "\n";
            backup.bk_dump(Jumper,"url.test.backup.txt");
            backup.sitemap_gen(Jumper,"sitemap.test.xml");
            Jumper.Clear();
        }*/


        // Письмо на почту
        /*if(EMails != null) {
            MailSender sender = new MailSender();
            sender.SetAuthProps(MailHost, MailPort, MailUsr, MailPwd);

            try {
                sender.SendTo(EMails, MailBody, Files.toArray(new String[]{}));
            } catch(Exception e) {
                System.out.println("ОШИБКА! Ошибка при отправке письма!");
                e.printStackTrace();
                Jumper.Destroy();
                return;
            }
        }*/


        if(Result) System.out.println("ОШИБКА! Возникла какая-то ошибка!");


        // освобождение памяти и выход
        webInterface.Destroy();


// попытка изменить движок с селениума на какой-то другой, шоб браузер не грузить
/*
        int connection_status = 0;


        URL url = new URL("https://pascal-med.ru");

        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try {
            boolean redirect = true;
            int redirects_num = 0;
            while(redirect) {
                if(redirects_num > 24) {connection_status = -6; break;}
                redirect = false;

                connection_status = connection.getResponseCode();
                if (connection_status != HttpURLConnection.HTTP_OK) {
                    if (connection_status == HttpURLConnection.HTTP_MOVED_TEMP
                            || connection_status == HttpURLConnection.HTTP_MOVED_PERM
                            || connection_status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }

                if (redirect) {
                    String newUrl = connection.getHeaderField("Location");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection_status = connection.getResponseCode();
                    redirects_num++;
                }
            }
        } catch (UnknownHostException e) {
            connection_status = -2;
        } catch (SSLException e) {
            connection_status = -3;
        } catch (Exception e) {
            connection_status = -5;
            e.printStackTrace();
        }



        StringBuilder builder = new StringBuilder();


        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line);
        }


        Document html = Jsoup.parse(builder.toString());
        Elements Tags = html.body().getElementsByTag("a");
        List<String> Attrs = Tags.eachAttr("href");

        for(String attr : Attrs){
            System.out.println(attr);
        }

        connection.disconnect();
*/


        return;}

}
