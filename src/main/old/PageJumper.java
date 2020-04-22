import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.net.ssl.SSLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class PageJumper {

    private String HomePageStr;
    private WebPageExt Root; // стартовая страница + сторонние сайты
    private ChromeDriver Driver;
    private long StartTime;
    private OutputFile FileToWrite;
    private int AllPageNum;
    private int ErrPageNum;
    private int ReportPageNum;


    public int TreeLevelMax;


    private String AllPgFile;
    private String ErrPgFile;
    private String TestResults;


    // параметры теста задаются до старта теста
    boolean get_params_as_new_pages;      // если true, то программа считает URL типа www.example.ru/page?parameter1=123 отличным от www.example.ru/page, иначе эти URL сводятся к www.example.ru/page
    boolean export_sub_pages_in_file;     // если true, то программа экспортирует ссылки со страницы в файл
    boolean test_out_of_homep_urls;       // если true, то программа переходит на сторонние сайты, но без сохранения ссылок на них (что-бы случайно не протестировать весь интернет), иначе не переходит
    boolean print_in_console;             // вывод в консоль всякой информации
    boolean print_subp_in_console;        // вывод в консоль ссылок на странице



    void Init(String drvPath) {
        // Путь к драйверу в PATH
        System.setProperty("webdriver.chrome.driver", drvPath);

        // открыть браузер и развернуть на весь экран
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        options.addArguments("incognito");
        //options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        this.Driver = new ChromeDriver(options);
        this.Driver.manage().timeouts().pageLoadTimeout(600, TimeUnit.SECONDS);
        //this.Driver.manage().window().maximize();
    }



    boolean Start(String home_page_url) throws Exception {

        // время старта теста
        this.StartTime = System.currentTimeMillis();


        // нормализация URL
        if(!WebPageExt.isValidURL(home_page_url)) return true;
        this.HomePageStr = WebPageExt.NormalizeURL(home_page_url,this.get_params_as_new_pages);


        // сохранение стартовой страницы в список
        WebPageExt HomePage = this.FindPageOrCreate(this.HomePageStr);
        HomePage.SetShouldTestThisPage(true);
        HomePage.SetShouldSearchLinks(true);



        // генерация имени файла для экспорта
        this.AllPgFile = this.HomePageStr;
        this.ErrPgFile = "";

        if(this.AllPgFile.startsWith("https://")) this.AllPgFile = this.AllPgFile.substring(8);
        else if(this.AllPgFile.startsWith("http://")) this.AllPgFile = this.AllPgFile.substring(7);
        if(this.AllPgFile.startsWith("www.")) this.AllPgFile = this.AllPgFile.substring(4);
        if(this.AllPgFile.contains("?")) this.AllPgFile = this.AllPgFile.split("\\?",2)[0];
        if(this.AllPgFile.contains("/")) this.AllPgFile = this.AllPgFile.replaceAll("/",".");
        if(this.AllPgFile.endsWith(".")) this.AllPgFile = this.AllPgFile.substring(0,this.AllPgFile.length()-1);
        this.ErrPgFile = this.AllPgFile + ".error-pages.csv";
        this.AllPgFile = this.AllPgFile + ".all-pages.csv";


        if(this.print_in_console)
            System.out.printf("Начало теста %s. Вывод в %s. Ошибки в %s\n",  this.HomePageStr, this.AllPgFile, this.ErrPgFile);


        // удаление файлов, если такие уже есть
        if(OutputFile.is_file_exist(this.AllPgFile)) OutputFile.delete_file(this.AllPgFile);
        if(OutputFile.is_file_exist(this.ErrPgFile)) OutputFile.delete_file(this.ErrPgFile);


        // класс для вывода инфы в файл
        this.FileToWrite = new OutputFile();


        // проход по всему списку страниц
        boolean Result = false;


        // общий тест
        this.AllPageNum = 0;
        this.ErrPageNum = 0;
        int child_pg_number = 0;
        if(!this.Root.GetChildList().isEmpty()) do {
            this.Go(this.Root.GetChildList().get(child_pg_number), this.Root);
            child_pg_number++;
        } while (child_pg_number < this.Root.GetChildList().size());



        // общее время теста
        long TestTime_l = System.currentTimeMillis() - this.StartTime;
        float TestTime_f = (float)TestTime_l / 1000.0f;
        String TestTime_s = "";

        { // форматирование времени
            Date date = new Date(TestTime_l);
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            TestTime_s = formatter.format(date);
        }

        if(this.print_in_console) System.out.printf("\nОбщее время теста = %s для %d из %d ссылок\n", TestTime_s, this.ReportPageNum, this.AllPageNum);


        // вывод в отчет
        if(this.AllPageNum > 0){
            this.FileToWrite.new_file(this.AllPgFile);
            if(this.export_sub_pages_in_file)
                this.FileToWrite.push_back("URL,HTTP status,Load time,Sub pages,Comment\n");
            else
                this.FileToWrite.push_back("URL,HTTP status,Load time\n");

            this.Root.PrintInFile(this.FileToWrite, false);
        }

        this.FileToWrite.push_back("\nTotal test time of " + this.HomePageStr + " (" + this.ReportPageNum + " out of " + this.AllPageNum + " URL's),," + TestTime_s + "\n");
        this.TestResults = "Total test time of " + this.HomePageStr + " (" + this.ReportPageNum + " out of " + this.AllPageNum + " URL's) = " + TestTime_s;


        // вывод страниц, вызвавших ошибку
        // в последний момент, т.к. необходим полный список страниц, с которых можно перейти на страницы из этого списка (родительских страниц)
        // и так-же удобнее просматривать в отчёте, т.к. все страницы с ошибками в конце
        if(this.ErrPageNum > 0) {
            this.FileToWrite.new_file(this.ErrPgFile);
            this.FileToWrite.push_back("URL,HTTP status,Parent pages\n");

            this.Root.PrintInFile(this.FileToWrite, true);
        }

        return Result;
    }



    boolean Go(WebPageExt ThisPage, WebPageExt PrevPage) throws Exception {
        this.AllPageNum++;


        if(this.print_in_console) {
            if(this.print_subp_in_console)
                System.out.printf("\n%d | %s;", this.AllPageNum, ThisPage.GetFullAddress());
            else
                System.out.printf("%d | %s;", this.AllPageNum, ThisPage.GetFullAddress());
        }


        if(ThisPage.isTested()) {
            if (this.print_in_console) System.out.printf(" Ответ (%d);", ThisPage.GetHttpStatus());
            String PageLoad_Time_Str = "";
            { // форматирование времени
                Date date = new Date(ThisPage.GetTime());
                DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                PageLoad_Time_Str = formatter.format(date);
            }
            if (this.print_in_console) System.out.printf(" Время = %s", PageLoad_Time_Str);
            return false;
        }


        if(this.print_in_console) {
            System.out.printf("; (%d of %d)\n", ThisPage.TreeLevel, this.TreeLevelMax);
        }


        if(!ThisPage.isShouldTestThisPage()) {
            if (this.print_in_console) System.out.print("\n");
            ThisPage.SetTested(true);
            //this.ReportPageNum++;
            if (this.print_in_console && this.print_subp_in_console && (ThisPage.GetParent() != null)) System.out.printf("\nВозврат к %s\n", ThisPage.GetParent().GetFullAddress());
            return false;
        }


        /*else if(ThisPage.isTested() && ThisPage.isShouldTestThisPage()) {
            if (this.print_in_console) System.out.printf(" Ответ (%d);", ThisPage.GetHttpStatus());
            String PageLoad_Time_Str = "";
            { // форматирование времени
                Date date = new Date(ThisPage.GetTime());
                DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                PageLoad_Time_Str = formatter.format(date);
            }
            if (this.print_in_console) System.out.printf(" Время = %s (проход по ссылкам)\n", PageLoad_Time_Str);

            for(WebPageExt child_link : ThisPage.GetChildList()) {
                if(!child_link.GetParentLinkList().isEmpty())
                    this.Go(child_link);
            }
        }*/


        // получение HTTP статуса
        long PageLoad_Start = System.currentTimeMillis();

        int http_resp = this.HttpResponds(ThisPage.GetFullAddress());
        if(this.print_in_console) System.out.printf(" Ответ (%d);", http_resp);
        ThisPage.SetHttpStatus(http_resp);

        if(http_resp >= 300) {
            this.ErrPageNum++;
        } else if(http_resp <= 0) {
            if (this.print_in_console) System.out.print(" Время = 0\n");
            ThisPage.SetTested(true);
            ThisPage.SetShouldTestThisPage(false);
            this.ReportPageNum++;
            this.ErrPageNum++;
            if (this.print_in_console && this.print_subp_in_console && (ThisPage.GetParent() != null)) System.out.printf("\nВозврат к %s\n", ThisPage.GetParent().GetFullAddress());
            return false;
        }


        // если получать список ссылок не нужно, то не нужно и загружать страницу. Просто возврат
        if(ThisPage.isShouldSearchLinks()) {
            // загрузка страницы, подсчет времени
            try {
                this.Driver.get(ThisPage.GetFullAddress());
            } catch (TimeoutException e) {
                ThisPage.SetHttpStatus(-1);
                this.ErrPageNum++;
                throw new Exception("time out");
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("driver get err");
            }
        }


        long PageLoad_Time_l = System.currentTimeMillis() - PageLoad_Start;
        ThisPage.SetTime(PageLoad_Time_l);


        String PageLoad_Time_Str = "";
        { // форматирование времени
            Date date = new Date(PageLoad_Time_l);
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            PageLoad_Time_Str = formatter.format(date);
        }


        if (this.print_in_console) System.out.printf(" Время = %s\n", PageLoad_Time_Str);


        ThisPage.SetTested(true);
        ThisPage.SetShouldTestThisPage(false);
        this.ReportPageNum++;


        if(!ThisPage.isShouldSearchLinks()) {
            if (this.print_in_console && this.print_subp_in_console && (ThisPage.GetParent() != null)) System.out.printf("\nВозврат к %s\n", ThisPage.GetParent().GetFullAddress());
            return false;
        }



        // получение элементов страницы
        List<WebElement> ele_list_a;
        List<String> ele_hrefs = new ArrayList<String>();
        try {
            ele_list_a = Driver.findElements(By.tagName("a"));
        } catch (Exception e) {
            e.printStackTrace();
            ThisPage.SetTested(false);
            throw new Exception("driver findElements err");
        }


        // получение ссылок
        for(WebElement ele_a : ele_list_a) {
            String ele_href = null;
            try {
                ele_href = ele_a.getAttribute("href");
            } catch (Exception e) {
                e.printStackTrace();
                if (e.getMessage().startsWith("chrome not reachable") ||
                        e.getMessage().startsWith("no such window")) {
                    ThisPage.SetTested(false);
                    throw new Exception("driver getAttribute err");
                }
                continue;
            }

            // отсееваем невалидные урлы
            if(!WebPageExt.isValidURL(ele_href)) continue;
            ele_href = WebPageExt.NormalizeURL(ele_href,this.get_params_as_new_pages);


            // добавление ссылок в страницу перед основным циклом
            WebPageExt SubPage = null;

            // поиск страницы в списке дочерних у родителя
            if(!ThisPage.GetChildLinkList().isEmpty()) for(WebPageExt search_result : ThisPage.GetChildLinkList()) {
                if(search_result.GetFullAddress().equals(ele_href)) SubPage = search_result;
            }

            if(SubPage != null) {continue;}

            ele_hrefs.add(ele_href);
            SubPage = this.FindPageOrCreate(ele_href);
            ThisPage.AddChildLink(SubPage);
        }


        // перебор ссылок
        for(String ele_href : ele_hrefs) {
            WebPageExt SubPage = this.FindPageOrCreate(ele_href);


            if(this.print_in_console && this.print_subp_in_console) System.out.printf(" - найден %s:",ele_href);
            if(SubPage.isTested()) {
                if (this.print_in_console && this.print_subp_in_console)
                    System.out.print(" уже проверен\n");
                continue;
            }


            if( ele_href.startsWith(this.HomePageStr) ) {
                if(this.print_in_console && this.print_subp_in_console) System.out.print(" уходит на проверку\n");
                SubPage.SetShouldTestThisPage(true);
                SubPage.SetShouldSearchLinks(true);
                this.Go(SubPage, ThisPage);
            } else {
                if(this.test_out_of_homep_urls) { // если стоит флажок на тестирование сторонних сайтов и т.п. то сохраняем его в список, но без поиска ссылок
                    if(this.print_in_console && this.print_subp_in_console) System.out.print(" не в пределах стартовой страницы (тестируется без переходов)\n");
                    SubPage.SetShouldTestThisPage(true);
                    SubPage.SetShouldSearchLinks(false);
                    this.Go(SubPage, ThisPage);
                } else {
                    if(this.print_in_console && this.print_subp_in_console) System.out.print(" не в пределах стартовой страницы (не тестируется)\n");
                    SubPage.SetShouldTestThisPage(false);
                    SubPage.SetShouldSearchLinks(false);
                }
            }
        }
        if (this.print_in_console && this.print_subp_in_console && (ThisPage.GetParent() != null)) System.out.printf("\nВозврат к %s\n", PrevPage.GetFullAddress());
        return false;
    }



    int HttpResponds(String url) {
        int Result = 0;

        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");

            int status = 0;
            boolean redirect = true;
            int redirects_num = 0;
            while(redirect) {
                if(redirects_num > 24) {status = -6; break;}
                redirect = false;

                status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }

                if (redirect) {
                    String newUrl = connection.getHeaderField("Location");
                    connection.disconnect();
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestMethod("HEAD");
                    status = connection.getResponseCode();
                    redirects_num++;
                }
            }

            connection.disconnect();
            Result = status;
        } catch (UnknownHostException e) {
            Result = -2;
        } catch (SSLException e) {
            Result = -3;
        } catch (Exception e) {
            Result = -5;
            e.printStackTrace();
        }
        return Result;
    }



    String GetStartPage() {
        return this.HomePageStr;
    }



    void SetStartPage(String pg) {
        this.HomePageStr = pg;
    }



    WebPageExt GetRoot() {
        return this.Root;
    }



    public void SetRoot(WebPageExt p) {
        this.Root = p;
    }



    String GetAllPgFile() {
        return this.AllPgFile;
    }



    String GetErrPgFile() {
        return this.ErrPgFile;
    }



    String GetTestResults() {
        return this.TestResults;
    }



    WebPageExt FindPageOrCreate(String url) {
        WebPageExt t_pg = this.Root;

        String ActualPage = "";
        String GetParams = "";


        // отделяем гет параметры, есл иони есть
        if(url.contains("?")) {
            String [] s_l_first = url.split("\\?",2);
            ActualPage = s_l_first[0];
            GetParams = s_l_first[1];
        } else {
            ActualPage = url;
        }


        String [] s_l_first = ActualPage.split("://",2); // отделяем протокол
        String [] s_l_second = s_l_first[1].split("/"); // делим по слэшу


        // максимальное количество вложенностей
        int get_param_level = GetParams.isEmpty() ? 0 : 1;
        int current_level = s_l_second.length + get_param_level;
        this.TreeLevelMax = this.TreeLevelMax < current_level ? current_level : this.TreeLevelMax;


        for(String s_l_address_part : s_l_second) {


            // если это первый сабп, то добавляем спереди протокол
            if(t_pg == this.Root) s_l_address_part = s_l_first[0] + "://" + s_l_address_part;


            WebPageExt t_sub_pg = null;


            // поиск страницы в списке дочерних у родителя
            if(!t_pg.GetChildList().isEmpty()) for(WebPageExt search_result : t_pg.GetChildList()) {
                if(search_result.GetAddress().equals(s_l_address_part)) t_sub_pg = search_result;
            }


            // создаем страницу, если не нашли
            if(t_sub_pg == null) {
                t_sub_pg = new WebPageExt();
                t_sub_pg.SetAddress(s_l_address_part);
                t_sub_pg.SetGetParameter(false);
                t_pg.AddChild(t_sub_pg);
            }


            // рекурсия
            t_pg = t_sub_pg;
        }


        // если есть гет параметр, то обавляем в список
        if(GetParams.length() > 0) {
            WebPageExt t_sub_pg = null;


            // поиск страницы в списке дочерних у родителя
            if(!t_pg.GetChildList().isEmpty()) for(WebPageExt search_result : t_pg.GetChildList()) {
                if(search_result.GetAddress().equals(GetParams)) t_sub_pg = search_result;
            }


            // создаем страницу, если не нашли
            if(t_sub_pg == null) {
                t_sub_pg = new WebPageExt();
                t_sub_pg.SetAddress(GetParams);
                t_sub_pg.SetGetParameter(true);
                t_pg.AddChild(t_sub_pg);
            }


            t_pg = t_sub_pg;
        }

        t_pg.TreeLevel = current_level;
        return t_pg;
    }



    void CommentsToLinks(WebPageExt pg) {

        System.out.print("\nЧтение (" + pg.GetFullAddress() + ") " + pg.isTested() + "; " + pg.isShouldTestThisPage() + ", " + pg.isShouldSearchLinks() + "\n");

        if(!pg.GetComment().isEmpty()) {
            String [] comments = pg.GetComment().split(";");
            //System.out.print(" - комменты (" + pg.GetComment() + ")\n");
            // список ссылок
            for(String comment_s : comments) {
                String [] indices = comment_s.split("/");

                WebPageExt SubPg = this.Root;

                //System.out.print(" - коммент (" + comment_s + ")\n");
                // список индексов
                for(String index_s : indices) {
                    //System.out.print(" - индекс (" + index_s + ")\n");
                    SubPg = SubPg.GetChildList().get(Integer.parseInt(index_s));
                }

                System.out.print(" - найден (" + SubPg.GetFullAddress() + ")\n");
                pg.AddChildLink(SubPg);
            }
        }


        // то-же самое для дочерних
        for(WebPageExt page : pg.GetChildList()) {
            this.CommentsToLinks(page);
        }
    }



    void Clear() {
        this.Root.Clear();

        this.HomePageStr = "";
        this.AllPageNum = 0;
        this.ErrPageNum = 0;
        this.ReportPageNum = 0;
    }



    PageJumper() {
        this.StartTime = 0;
        this.AllPageNum = 0;
        this.ErrPageNum = 0;
        this.ReportPageNum = 0;
        this.TreeLevelMax = 0;
        this.Root = new WebPageExt();
    }



    void Destroy() {
        this.Clear();
        Driver.quit();
    }
}
