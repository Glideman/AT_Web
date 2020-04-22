import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


class WebPageExt {
    private String ExtAddress;
    private String Comment;
    private int HttpStatus;
    public int TreeLevel;
    private long Time;
    private boolean Tested;
    private boolean ShouldTestThisPage;
    private boolean ShouldSearchLinks;
    private boolean GetParameter;

    private WebPageExt ParentPage;
    private List<WebPageExt> ChildList;

    private List<WebPageExt> ChildLinkList;
    private List<WebPageExt> ParentLinkList;


    String GetAddress() {
        return this.ExtAddress;
    }


    String GetFullAddress() {
        if(this.ParentPage != null) {
            if(this.ParentPage.GetFullAddress().length() > 0) {
                if(this.isGetParameter())
                    return this.ParentPage.GetFullAddress() + "?" + this.ExtAddress;
                else
                    return this.ParentPage.GetFullAddress() + "/" + this.ExtAddress;
            }
            else return this.ExtAddress;
        }
        else return this.ExtAddress;
    }


    String GetPackedAddress() {
        boolean found = false;
        Integer index = 0;
        if(this.ParentPage != null) {

            for(WebPageExt child : this.ParentPage.ChildList) {
                if(child.ExtAddress.equals(this.ExtAddress)) {found = true; break;}
                else index++;
            }

            if(!found) index = -1; // у родителя нет в списке дочерних этой страницы (wut?!)

            if(this.ParentPage.GetPackedAddress().length() > 0) {
                return this.ParentPage.GetPackedAddress() + "/" + index;
            }
            else return index.toString();

        }
        else return "";
    }


    void SetAddress(String a) {
        this.ExtAddress = a;
    }


    long GetTime() {
        return this.Time;
    }


    void SetTime(long t) {
        this.Time = t;
    }


    int GetHttpStatus() {
        return this.HttpStatus;
    }


    void SetHttpStatus(int s) {
        this.HttpStatus = s;
    }


    boolean isTested() {
        return this.Tested;
    }


    boolean calcTested() {
        return ((this.Tested || (this.ParentLinkList.isEmpty() != (this.HttpStatus > 0) && (this.HttpStatus < 300)) ) && this.isChildsTested());
    }


    boolean isChildsTested() {
        for(WebPageExt child_pg : this.GetChildList()) {
            if( !child_pg.calcTested() ) return false;
        }
        return true;
    }


    void SetTested(boolean yesno) {
        this.Tested = yesno;
    }


    boolean isShouldTestThisPage() {
        return this.ShouldTestThisPage;
    }


    void SetShouldTestThisPage(boolean yesno) {
        this.ShouldTestThisPage = yesno;
    }


    boolean isShouldSearchLinks() {
        return this.ShouldSearchLinks;
    }


    void SetShouldSearchLinks(boolean yesno) {
        this.ShouldSearchLinks = yesno;
    }


    boolean isGetParameter() {
        return this.GetParameter;
    }


    void SetGetParameter(boolean yesno) {
        this.GetParameter = yesno;
    }


    String GetComment() {
        return this.Comment;
    }


    void SetComment(String c) {
        this.Comment = c;
    }


    WebPageExt GetParent() {
        return this.ParentPage;
    }


    void SetParent(WebPageExt p) {
        this.ParentPage = p;
    }


    List<WebPageExt> GetChildList() {
        return this.ChildList;
    }


    List<WebPageExt> GetChildLinkList() {
        return this.ChildLinkList;
    }


    List<WebPageExt> GetParentLinkList() {
        return this.ParentLinkList;
    }


    void AddChild(WebPageExt p) {
        this.ChildList.add(p);
        p.ParentPage = this;
    }


    void AddChildLink(WebPageExt p) {
        this.ChildLinkList.add(p);
        p.ParentLinkList.add(this);
    }


    void PrintInFile(OutputFile f, boolean errors_only) throws Exception {
        List<String> StrList = new ArrayList<String>();

        // вывод дочерних элементов
        for(WebPageExt printed_page : this.ChildList) {
            if(errors_only) {
                if(!printed_page.GetParentLinkList().isEmpty() && ((printed_page.GetHttpStatus() <= 0) || (printed_page.GetHttpStatus() >= 300))) {
                    StrList.add("\"" + printed_page.GetFullAddress() + "\"," + printed_page.GetHttpStatus() + "\n");
                    for(WebPageExt parent_link : printed_page.GetParentLinkList()) {
                        StrList.add(",,\"" + parent_link.GetFullAddress() + "\"\n");
                    }
                }
            } else  {
                String TestTime_s;

                { // форматирование времени
                    Date date = new Date(printed_page.GetTime());
                    DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    TestTime_s = formatter.format(date);
                }

                // выводить в отчёт, только если на эту страницу есьт переходы
                if(!printed_page.GetParentLinkList().isEmpty())
                    StrList.add("\""  + printed_page.GetFullAddress() + "\"," + printed_page.GetHttpStatus() + "," + TestTime_s + "\n");
            }
        }

        f.push_back_list(StrList);


        // рекурсия
        for(WebPageExt printed_page : this.ChildList) {
            printed_page.PrintInFile(f,errors_only);
        }
    }


    void PrintInFile_xml(OutputFile f, PageJumper Jpr) throws Exception {
        List<String> StrList = new ArrayList<String>();

        // вывод дочерних элементов
        for(WebPageExt printed_page : this.ChildList) {

            // выводить в отчёт, только если на эту страницу есьт переходы
            if(!printed_page.GetParentLinkList().isEmpty()) {

                if( printed_page.GetFullAddress().toLowerCase().endsWith(".jpg") ||
                    printed_page.GetFullAddress().toLowerCase().endsWith(".jpeg") ||
                    printed_page.GetFullAddress().toLowerCase().endsWith(".png") ||
                    printed_page.GetFullAddress().toLowerCase().endsWith(".bmp") ||
                    printed_page.GetFullAddress().toLowerCase().endsWith(".gif") ||
                    printed_page.GetFullAddress().toLowerCase().endsWith(".pdf") ||
                    printed_page.GetFullAddress().toLowerCase().endsWith(".svg") ||
                    !printed_page.GetFullAddress().startsWith(Jpr.GetStartPage()) ||
                    printed_page.isGetParameter()
                ) continue;

                String TestTime_s;

                { // форматирование времени
                    Date date = new Date();
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    TestTime_s = formatter.format(date).substring(0,22)+":00";
                }

                StrList.add("  <url>\n");
                StrList.add("    <loc>" + printed_page.GetFullAddress() + "</loc>\n");
                StrList.add("    <priority>" + (1.0 - 0.1 * (printed_page.TreeLevel-1)) + "</priority>\n");
                StrList.add("    <lastmod>" + TestTime_s + "</lastmod>\n");
                StrList.add("  </url>\n");
            }

        }

        f.push_back_list(StrList);


        // рекурсия
        for(WebPageExt printed_page : this.ChildList) {
            printed_page.PrintInFile_xml(f, Jpr);
        }
    }


    void Clear() {
        if(!this.ChildList.isEmpty()) {
            for( WebPageExt page : this.ChildList) {
                page.Clear();
            }
            this.ChildList.clear();
        }
        if(!this.ChildLinkList.isEmpty()) this.ChildLinkList.clear();
        if(!this.ParentLinkList.isEmpty()) this.ParentLinkList.clear();
    }


    static String NormalizeURL(String url, boolean get_params_as_new_pages) {
        String result = url;//.toLowerCase();

        //if(result.compareTo("") == 0) return result;
        //if(!result.startsWith("http")) return result; // отсееваем урлы, начинающиеся не с http

        // убираем якорь
        if(result.contains("#"))
            result = result.split("#",2)[0];

        // если проверка get параметров выключена, то удаляем эти параметры
        if(!get_params_as_new_pages) {
            if(result.contains("?")) {
                result = result.split("\\?",2)[0];
            }
        }

        // удаляем слэш в конце, если он есть
        result = DeleteLastSlash(result);


        if(result.startsWith("https://")) result = result.substring(8);
        else if(result.startsWith("http://")) result = result.substring(7);
        if(result.startsWith("www.")) result = result.substring(4);
        result = "http://" + result;


        return result;
    }


    static boolean isValidURL(String url) {
        if(url == null) return false;
        else if(url.compareTo("") == 0) return false;
        else if(url.contains(":")) {
            if(!url.startsWith("http")) return false;
        }

        return true;
    }


    static String DeleteLastSlash(String url) {
        // не во всех урлах есть слэш в конце. приводим их к одному виду чтоб проверить на наличие их в списке
        // то-есть убираем слэш
        String Result = url;
        if(Result.contains("?")) {
            String [] SpStr = Result.split("\\?",2);
            if(SpStr[0].endsWith("/")) Result = SpStr[0].substring(0, SpStr[0].length() - 1).concat("?").concat(SpStr[1]);
        }
        else if(Result.endsWith("/")) Result = Result.substring(0, Result.length() - 1);

        return Result;
    }


    WebPageExt() {
        this.ExtAddress = "";
        this.Comment = "";
        this.HttpStatus = 0;
        this.TreeLevel = 0;
        this.Time = 0;
        this.Tested = false;
        this.ShouldTestThisPage = false;
        this.ShouldSearchLinks = false;
        this.GetParameter = false;
        this.ParentPage = null;
        this.ChildList = new ArrayList<WebPageExt>();
        this.ChildLinkList = new ArrayList<WebPageExt>();
        this.ParentLinkList = new ArrayList<WebPageExt>();
    }

}
