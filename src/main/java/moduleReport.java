import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class moduleReport extends moduleDefault {

    protected List<atwebSite> siteList;
    protected List<atwebPage> pageList;
    protected List<atwebUrl> urlList;

    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = moduleReport.class.getDeclaredMethods();
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


    moduleReport() {
        this.name = "report";
        this.siteList = new ArrayList<>();
        this.pageList = new ArrayList<>();
        this.urlList = new ArrayList<>();
    }


    public boolean Run() {

        try {
            readRawReport("target/raw.report.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        reportHTML("rep.html");
        //reportPagesOnSite_2("travel.csv");
        //reportDocumentsWithLinks("bl-docs-links.csv");
        //reportDocuments("bl-docs.csv");
        //reportNon200("pagano-error.csv");
        //reportAllLinksToSitesExcept("pagano-sites.csv", "www.pagano.ru");
        //reportAllLinksRedirects("pagano-redirects.csv");
        //reportPagesOnSite("pages.csv", "http://avtodor-tr.gva.dev.one-touch.ru");
        //report404("404.csv");
        //report404_2("upd.csv");
        //reportAllPages("travel.csv");

        /*System.out.println("\n\n\nsites dump:");
        for(atwebSite site : this.siteList) {
            site.dump();
        }*/

        return false;
    }


    protected void reportHTML(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> StrList = new ArrayList<String>();

        // Страница - начало
        StrList.add("" +
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "    <head>\n" +
                "        <meta charset=\"UTF-8\">\n" +
                "        <title>(site name) report</title>\n" +
                "        <!-- стили отображения отчета -->\n" +
                "        <style type=\"text/css\">\n" +
                "        </style>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        <section>\n" +
                "            <h1>Отчет о проверке сайтов.</h1>\n" +
                "            <p>Список сайтов:</p>\n" +
                "            <ul>\n" +
                "                <li>(site name 1)</li>\n" +
                "                <li>(site name 2)</li>\n" +
                "            </ul>\n" +
                "            <hr>\n" +
                "            <p>Сортировка</p>\n" +
                "            <form>\n" +
                "                <select>\n" +
                "                    <option>Пункт 1</option>\n" +
                "                    <option>Пункт 2</option>\n" +
                "                </select>\n" +
                "                <input type=\"submit\" value=\"Sort\">\n" +
                "                <input type=\"reset\" value=\"Reset\">\n" +
                "                <input type=\"button\" value=\"Export\">\n" +
                "                <select>\n" +
                "                    <option>.csv</option>\n" +
                "                </select>\n" +
                "            </form>\n" +
                "            <hr>\n" +
                "            <div class=\"js-result-container\">\n" +
                "                Result\n" +
                "            </div>\n" +
                "        </section>\n" +
                "        <!-- скрипты (фильтр и тп). функциональная часть -->\n" +
                "        <script type=\"text/javascript\">\n"+
                "let dataRaw = [\n");

        for(atwebSite site : siteList) {
            StrList.add("[\"" + site.protocol + "\",\"" + site.name + "\",");

            addPageToHTML(site.root, StrList);

            StrList.add("],");
        }

        // Классы JS
        StrList.add("];\n" +
                "            class atwebSite {\n" +
                "                constructor() {\n" +
                "                    this.protocol = \"\";\n" +
                "                    this.name = \"\";\n" +
                "                    this.root = new atwebPage();\n" +
                "                    this.root.site = this;\n" +
                "                    this.pageList = new Array();\n" +
                "                }\n" +
                "            }\n" +
                "            class atwebPage {\n" +
                "                constructor() {\n" +
                "                    this.alias = \"\";\n" +
                "                    this.comment = \"\";\n" +
                "                    this.clientTime = 0;\n" +
                "                    this.responseOnly = true;\n" +
                "                    this.done = false;\n" +
                "                    this.getParameter = false;\n" +
                "                    this.anchor = false;\n" +
                "                    this.formList = new Array();\n" +
                "                    this.urlOnPageList = new Array();\n" +
                "                    this.urlToPageList = new Array();\n" +
                "                    this.childList = new Array();\n" +
                "                    this.parent = null;\n" +
                "                    this.site = null;\n" +
                "                }\n" +
                "            }\n" +
                "            class atwebUrl {\n" +
                "                constructor(url) {\n" +
                "                    this.destinationPage = null;\n" +
                "                    this.page = null;\n" +
                "                    this.urlStarting = url;\n" +
                "                    this.urlDestination = \"\";\n" +
                "                    this.numRedirects = 0;\n" +
                "                    this.httpResponseCode = 0;\n" +
                "                    this.httpFirstResponseCode = 0;\n" +
                "                    this.serverTimeAll = 0;\n" +
                "                    this.serverTimeDst = 0;\n" +
                "                    this.connection = null;\n" +
                "                    this.contentType = \"\";\n" +
                "                    this.comment = \"\";\n" +
                "                }\n" +
                "            }\n" +
                "        </script>\n" +
                "    </body>\n" +
                "</html>\n");

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void addPageToHTML(atwebPage page, List<String> text) {
        if(page != null) {

            // Данные сайта
            String line = "[\"" +
                    page.alias + "\",\"" +
                    page.comment + "\"," +
                    page.clientTime + "," +
                    page.getParameter + "," +
                    page.anchor + ",[";
            text.add(line);

            // список урлов
            for(atwebUrl url : page.getUrlOnPageList()) {
                line = "[\"" +
                        url.urlStarting + "\",\"" +
                        url.urlDestination + "\"," +
                        url.numRedirects + "," +
                        url.httpResponseCode + "," +
                        url.httpFirstResponseCode + "," +
                        url.serverTimeAll + "," +
                        url.serverTimeDst + ",\"" +
                        url.contentType + "\",\"" +
                        url.comment + "\"],";
                text.add(line);
            }

            // дети
            text.add("],[");
            for(atwebPage child : page.getChildList()) {
                addPageToHTML(child,text);
            }
            text.add("]],");
        }
    }


    protected void reportPagesOnSite(String fileName, String siteName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;URL\n");
        int counter = 0;

        for(atwebUrl url : urlList) {
            if( url.urlDestination.startsWith(siteName) &&
                    url.httpResponseCode != 200 ) {
                counter++;
                StrList.add(counter + ";" + url.httpResponseCode + ";" + url.urlDestination + "\n");
            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void reportPagesOnSite_2(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> StrList = new ArrayList<String>();
        StrList.add("URL\n");
        Integer counter = 0;

        for(atwebSite site : this.siteList) {
            addPageListToStr(site.root,StrList);
        }

        /*for(atwebUrl url : urlList) {
            if(url.page != null)
            if(!url.page.getUrlToPageList().isEmpty()) {
                counter++;
                StrList.add(counter + ";" + url.httpResponseCode + ";" + url.urlDestination + "\n");
            }
        }*/

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void addPageListToStr(atwebPage page, List<String> text) {
        if(page != null) {
            if( !page.getUrlToPageList().isEmpty() &&
                !page.getParameter &&
                !page.anchor ) {
                String line = page.getFullAddress() + "\n";
                System.out.print(line);
                text.add(line);
            }

            for(atwebPage child : page.getChildList()) {
                addPageListToStr(child,text);
            }
        }
    }


    protected void reportAllLinksToSitesExcept(String fileName, String site) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;Page;URL;Type\n");
        int counter = 0;

        for(atwebUrl url : urlList) {
            ///System.out.println(" -- " + url.urlStarting + " : " + url.page.site.name);
            if( !url.destinationPage.site.name.equals(site) ) {
                for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                    counter++;
                    StrList.add(counter + ";" + urlTo.httpResponseCode + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + ";" + urlTo.contentType + "\n");
                }
            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void reportDocuments(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;URL;Type\n");
        int counter = 0;


        for(atwebSite site : this.siteList) {
            addDocListToStr(site.root,StrList);
        }


        /*for(atwebUrl url : urlList) {
            if( !url.contentType.equals("text/html") && !url.contentType.equals("null") ) {
                for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                    counter++;
                    StrList.add(counter + ";" + urlTo.urlStarting + ";" + urlTo.contentType + "\n");
                }
            }
        }*/

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void addDocListToStr(atwebPage page, List<String> text) {
        if(page != null) {
            if( !page.getUrlToPageList().isEmpty() &&
                    !page.getParameter &&
                    !page.anchor ) {

                atwebUrl url = page.getUrlToPageList().get(0);

                if(!url.contentType.equals("text/html") && !url.contentType.equals("null")) {
                    //for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                        //counter++;
                        //StrList.add(counter + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + ";" + urlTo.contentType + "\n");
                        String line = (text.size()) + ";" + url.urlStarting + ";" + url.contentType + "\n";
                        System.out.print(line);
                        text.add(line);
                    //}
                }


            }

            for(atwebPage child : page.getChildList()) {
                addDocListToStr(child,text);
            }
        }
    }


    protected void reportDocumentsWithLinks(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Page;URL;Type\n");
        int counter = 0;


        for(atwebSite site : this.siteList) {
            addDocListToStrWithLinks(site.root,StrList);
        }

        /*for(atwebUrl url : urlList) {
            if( !url.contentType.equals("text/html") && !url.contentType.equals("null") ) {
                for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                    counter++;
                    StrList.add(counter + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + ";" + urlTo.contentType + "\n");
                }
            }
        }*/

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void addDocListToStrWithLinks(atwebPage page, List<String> text) {
        if(page != null) {
            if( !page.getUrlToPageList().isEmpty() &&
                    !page.getParameter &&
                    !page.anchor ) {

                atwebUrl url = page.getUrlToPageList().get(0);

                if(!url.contentType.equals("text/html") && !url.contentType.equals("null")) {
                    for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                        //counter++;
                        //StrList.add(counter + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + ";" + urlTo.contentType + "\n");
                        String line = (text.size()) + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + ";" + urlTo.contentType + "\n";
                        System.out.print(line);
                        text.add(line);
                    }
                }


            }

            for(atwebPage child : page.getChildList()) {
                addDocListToStrWithLinks(child,text);
            }
        }
    }



    protected void reportAllLinksRedirects(String fileName) { // отчет в файл
/*
        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;Page;URL;Destination;Chain length\n");
        int counter = 0;

        for(atwebUrl url : urlList) {
            if( url.httpFirstResponseCode >= 300 && url.httpFirstResponseCode < 400 ) {
                for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                    counter++;
                    StrList.add(counter + ";" + urlTo.httpFirstResponseCode + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + ";" + urlTo.urlDestination + ";" + urlTo.numRedirects + "\n");
                }
            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();*/
    }


    protected void reportNon200(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;Page;URL\n");
        int counter = 0;

        for(atwebUrl url : urlList) {
            if( url.httpResponseCode != 200 ) {

                //if(url.destinationPage != null) {
                for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                    counter++;
                    //if(urlTo.page != null) {
                    StrList.add(counter + ";" + urlTo.httpResponseCode + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + "\n");
                        /*} else {
                            StrList.add(counter + ";" + url.httpResponseCode + ";-;" + url.urlDestination + "\n");
                        }*/
                }
                //} else {
                //    counter++;
                //    StrList.add(counter + ";" + url.httpResponseCode + ";-;" + url.urlDestination + "\n");
                //}

            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void report404(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;Page;URL\n");
        int counter = 0;

        for(atwebUrl url : urlList) {
            if( url.httpResponseCode == 404 ) {

                //TODO костылище для автодора

                /*if(
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account") ||
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account/") ||
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account/feedback") ||
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account/feedback/")
                ) continue;*/


                //if(url.destinationPage != null) {
                    for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                        counter++;
                        //if(urlTo.page != null) {
                            StrList.add(counter + ";" + urlTo.httpResponseCode + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + "\n");
                        /*} else {
                            StrList.add(counter + ";" + url.httpResponseCode + ";-;" + url.urlDestination + "\n");
                        }*/
                    }
                //} else {
                //    counter++;
                //    StrList.add(counter + ";" + url.httpResponseCode + ";-;" + url.urlDestination + "\n");
                //}

            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void report404_2(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;Page;URL\n");
        int counter = 0;

        for(atwebUrl url : urlList) {
            if( /*url.urlDestination.startsWith("http://avtodor-tr.gva.dev.one-touch.ru") &&*/
                    url.httpResponseCode == 404 ) {

                //TODO костылище для автодора

                if(
                        url.urlStarting.startsWith("http://avtodor-tr.gva.dev.one-touch.ru/account") ||
                        url.urlDestination.startsWith("https://zakupki.gov.ru/")
                ) continue;


                //if(url.destinationPage != null) {
                for(atwebUrl urlTo : url.destinationPage.getUrlToPageList()) {
                    counter++;
                    //if(urlTo.page != null) {
                    StrList.add(counter + ";" + urlTo.httpResponseCode + ";" + urlTo.page.getFullAddress() + ";" + urlTo.urlStarting + "\n");
                        /*} else {
                            StrList.add(counter + ";" + url.httpResponseCode + ";-;" + url.urlDestination + "\n");
                        }*/
                }
                //} else {
                //    counter++;
                //    StrList.add(counter + ";" + url.httpResponseCode + ";-;" + url.urlDestination + "\n");
                //}

            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected void reportAllPages(String fileName) {

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> StrList = new ArrayList<String>();
        StrList.add("№;Code;Page\n");
        int counter = 0;

        for(atwebPage page : this.pageList) {
            if( page.site.name.equals("avtodor-tr.gva.dev.one-touch.ru") && !page.getUrlToPageList().isEmpty()) {
                counter++;
                StrList.add(counter + ";" + page.getUrlToPageList().get(0).httpResponseCode + ";" + page.getFullAddress() + "\n");
            }
        }

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }


    protected atwebUrl urlFindOrCreate(atwebPage page, String url) {
        // поиск урла в глобальном списке урлов
        atwebUrl global_search_result = null;
        if(!this.urlList.isEmpty()) for(atwebUrl existingChildUrl : this.urlList) {
            if(existingChildUrl.urlStarting.equals(url)) global_search_result = existingChildUrl;
        }

        // если урла нет, то создаём объект, добавляем в глобальный список урлов
        if(global_search_result == null) {
            global_search_result = new atwebUrl(url);
            this.urlList.add(global_search_result);
        }

        // поиск урла в списке урлов страницы
        //atwebUrl search_result = null;
        //if(!page.getUrlOnPageList().isEmpty()) for(atwebUrl existingChildUrl : page.getUrlOnPageList()) {
        //    if(existingChildUrl.urlStarting.equals(url)) search_result = existingChildUrl;
        //}

        // добавляем на страницу урл, если его там нет
        //if(search_result == null) {
            // только в отчете - одинаковые урлы считаются уникальными
            // это требуется для поиска страниц с некорректными урлами
            // если урл будет только один общий, то в отчете будет некорректная инфа
            global_search_result = new atwebUrl(url);
            page.addUrlOnPage(global_search_result);
        //}

        return global_search_result;
    }



    protected atwebPage pageFindOrCreate(String url) {

        String normAddress = atwebUrl.normalizeUrl(url);
        String siteAddress = normAddress;

        String [] firstSplit = siteAddress.split("(:\\/\\/)",2); // отделяем протокол
        String [] secondSplit = firstSplit[1].split("\\/",2); // делим по слэшу

        siteAddress = firstSplit[0] + "://" + secondSplit[0];

        atwebSite currentSite = null;
        for(atwebSite site : this.siteList)
            if(site.name.equals(secondSplit[0]))
                currentSite = site;

        if(currentSite == null) {
            //System.out.println("NEW SITE " + siteAddress + " : " + secondSplit[0]);
            currentSite = new atwebSite();
            currentSite.setAddress(siteAddress);
            //System.out.println("btw " + currentSite + " : " + currentSite.name);
            this.siteList.add(currentSite);
        }

        atwebPage resultPage = currentSite.root;


        if(siteAddress.equals(normAddress))
            currentSite.pageList.add(currentSite.root);

        if(secondSplit.length > 1)
            resultPage = currentSite.pageFindOrCreate(secondSplit[1]);

        //System.out.println("btw2 " + resultPage + " : " + resultPage.site.name);
        return resultPage;
    }



    protected boolean readRawReport(String fileName) throws Exception {
        File file = new File(fileName);
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                result = true;
            } else {
                System.out.print("Can't open file " + fileName + ": directory located on this path");
                result = false;
            }
        } else {
            result = false;
        }

        if(!result) return false;


        FileReader reader = new FileReader(fileName);
        Scanner scan = new Scanner(reader);

        atwebPage currentPage = null;
        atwebUrl currentUrl = null;

        while(scan.hasNextLine()) {
            String nextLine = scan.nextLine();
            String [] splitLine = nextLine.split(";");


            if(splitLine[0].equals("p")) {

                String pageAddress = splitLine[1];
                Integer pageClientTime = Integer.parseInt(splitLine[2]);

                currentPage = this.pageFindOrCreate(pageAddress);
                currentPage.clientTime = pageClientTime;
                this.pageList.add(currentPage);
            }

            else if(splitLine[0].equals("u")) {
                currentUrl = new atwebUrl(splitLine[1]); //urlFindOrCreate(currentPage,splitLine[1]);
                currentUrl.urlDestination = splitLine[2];
                currentUrl.numRedirects = Integer.parseInt(splitLine[3]);
                currentUrl.httpResponseCode = Integer.parseInt(splitLine[4]);
                currentUrl.httpFirstResponseCode = Integer.parseInt(splitLine[5]);
                currentUrl.serverTimeAll = Integer.parseInt(splitLine[6]);
                currentUrl.serverTimeDst = Integer.parseInt(splitLine[7]);
                currentUrl.contentType = splitLine[8];
                currentUrl.page = currentPage;
                currentUrl.destinationPage = pageFindOrCreate(currentUrl.urlDestination);
                //System.out.println("AAAAAA2 " + currentUrl.destinationPage.site.name);
                currentUrl.destinationPage.addUrlToPage(currentUrl);

                // поиск урла в списке урлов страницы
                atwebUrl search_result = null;
                if(!currentPage.getUrlOnPageList().isEmpty()) for(atwebUrl existingChildUrl : currentPage.getUrlOnPageList()) {
                    if(existingChildUrl.urlStarting.equals(currentUrl.urlStarting)) search_result = existingChildUrl;
                }

                // добавляем на страницу урл, если его там нет
                if(search_result == null) {
                    currentPage.addUrlOnPage(currentUrl);
                }

                // поиск урла в глобальном списке урлов
                atwebUrl global_search_result = null;
                if(!this.urlList.isEmpty()) for(atwebUrl existingChildUrl : this.urlList) {
                    if(existingChildUrl.urlStarting.equals(currentUrl.urlStarting)) global_search_result = existingChildUrl;
                }

                // если урла нет, то создаём объект, добавляем в глобальный список урлов
                if(global_search_result == null) {
                    this.urlList.add(currentUrl);
                    //System.out.println("Added url " + currentUrl.urlStarting + " : " + currentUrl.page.site + " : " + currentUrl.page.site.name);
                }
            }

            else {

            }
        }

        reader.close();

        return result;
    }



    protected boolean createRawDataFile(String fileName) throws Exception {
        File file = new File(fileName);
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                if(file.delete()) {
                    result = file.createNewFile();
                } else {
                    System.out.print("Can't create new file " + fileName);
                }
            } else {
                System.out.print("Can't create new file " + fileName + ": directory located on this path");
                result = false;
            }
        } else {
            result = file.createNewFile();
        }

        return result;
    }



    protected void putRawDataInFile(String fileName, List<String> text) throws Exception {
        FileWriter writer = new FileWriter(fileName,true);
        for(String txt : text) {
            writer.write(txt);
        }
        writer.close();
    }



    protected void makeRawReport(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(atwebSite site : this.siteList) {
            pageToRawReport(fileName, site.root);
        }

    }



    protected void pageToRawReport(String fileName, atwebPage page) { // отчет в файл

        if(!page.getUrlToPageList().isEmpty()){
            List<String> StrList = new ArrayList<String>();

            StrList.add("p;" + page.getFullAddress() + ";" + page.clientTime + "\n");

            for (atwebUrl pageurl : page.getUrlOnPageList()) {
                StrList.add("u;" + pageurl.urlStarting + ";" + pageurl.urlDestination + ";"
                        + pageurl.numRedirects + ";" + pageurl.httpResponseCode + ";" + pageurl.httpFirstResponseCode + ";"
                        + pageurl.serverTimeAll + ";" + pageurl.serverTimeDst + ";"
                        + pageurl.contentType + "\n");
            }

            try {
                this.putRawDataInFile(fileName, StrList);
            } catch (Exception e) {
                e.printStackTrace();
            }

            StrList.clear();
        }

        if(!page.getChildList().isEmpty()) for(atwebPage pagechild : page.getChildList()) {
            pageToRawReport(fileName, pagechild);
        }
    }
}
