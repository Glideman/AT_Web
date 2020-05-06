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
        this.urlList = new ArrayList<>();
    }


    public boolean Run() {

        try {
            readRawReport("atr.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        reportPagesOnSite("pages.csv", "http://avtodor-tr.gva.dev.one-touch.ru");
        report404("404.csv");

        /*System.out.println("\n\n\nsites dump:");
        for(atwebSite site : this.siteList) {
            site.dump();
        }*/

        return false;
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

                if(
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account") ||
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account/") ||
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account/feedback") ||
                        url.urlStarting.equals("http://avtodor-tr.gva.dev.one-touch.ru/account/feedback/")
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
            if(site.getAddress().equals(siteAddress))
                currentSite = site;

        if(currentSite == null) {
            currentSite = new atwebSite();
            currentSite.setAddress(siteAddress);
            this.siteList.add(currentSite);
        }

        atwebPage resultPage = currentSite.root;

        if(siteAddress.equals(normAddress))
            currentSite.pageList.add(currentSite.root);

        if(secondSplit.length > 1)
            resultPage = currentSite.pageFindOrCreate(secondSplit[1]);

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
