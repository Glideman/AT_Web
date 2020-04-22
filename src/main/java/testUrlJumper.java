import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class testUrlJumper extends moduleDefault {

    protected long startTime;
    protected long totalTime;

    protected List<atwebSite> siteList;
    protected List<atwebUrl> urlList;

    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = testUrlJumper.class.getDeclaredMethods();
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


    testUrlJumper() {
        this.name = "urljumper";
        this.siteList = new ArrayList<>();
        this.urlList = new ArrayList<>();
    }


    public boolean Run() {

        // список сайтов
        String[] startingAddressArray = null;
        String startingAddressStr = this.webInterface.GetProp("starting_pages");
        if(!startingAddressStr.equals("")) {
            startingAddressArray = startingAddressStr.split(";");
        } else {
            System.out.println("There is no sites to test (sites = \"" + startingAddressStr + "\"");
            return true;}


        // переход на сайт(ы)
        for( String startingAddress : startingAddressArray ) {
            // время старта теста
            this.startTime = System.currentTimeMillis();

            System.out.println("starting with address " + startingAddress);

            // создание родительского обьекта если подключение к странице возможно
            atwebUrl startingUrl = new atwebUrl(startingAddress);
            this.urlList.add(startingUrl);

            // проход по всем страницам в списке страниц
            for(atwebUrl url : this.urlList) {
                this.goTo(url);
            }
        }

        // Письмо на почту

        return false;
    }



    protected void goTo(atwebUrl url) {
        System.out.print("goTo " + url.urlStarting);

        url.connect(true, true);
        System.out.println(" response " + url.httpResponseCode + " destination " + url.urlDestination);

        atwebPage page = this.pageFindOrCreate(url.urlDestination);

        if(page.isDone()) { return; }


        //atwebPage startingPage = this.pageFindOrCreate(startingUrl.urlDestination);
        //startingPage.setUrlOnce(startingUrl);
        //atwebSite currentSite = startingPage.site;
        //currentSite.setStartingUrl(startingUrl);


        //if(!page.isResponseOnly()) { page.setDone(true); return; }

        // child list



    }



    protected atwebPage pageFindOrCreate(String url) {

        String siteAddress = atwebUrl.normalizeUrl(url);

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

            if(atwebUrl.normalizeUrl(siteAddress).equals(atwebUrl.normalizeUrl(url)))
                currentSite.pageList.add(currentSite.root);
        }

        atwebPage resultPage = currentSite.root;

        if(secondSplit.length > 1)
            resultPage = currentSite.pageFindOrCreate(secondSplit[1]);

        return resultPage;
    }

}
