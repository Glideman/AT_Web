import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.Properties;



public class atwebInterface {
    private ChromeDriver driver;
    private Properties props;
    private atwebUrl CurrentPage;


    interface Module {
        String GetModuleName();
        void Init(atwebInterface webInterface);
        boolean Invoke(String cmd, String ... params);
        void Destroy();
    }


    private HashMap<String, Module> moduleList;


    void Init() {
        if(this.GetProp("using_selenium").equals("true")) {
            // Путь к драйверу в PATH
            System.setProperty("webdriver.chrome.driver", this.GetProp("driver_path"));

            // открыть браузер и развернуть на весь экран
            ChromeOptions options = new ChromeOptions();
            options.addArguments("start-maximized");
            options.addArguments("incognito");
            this.driver = new ChromeDriver(options);
            this.driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
        }


        this.moduleList = new HashMap<>();
        //this.root = new WebPageExt();


        this.props.setProperty("home_page","");
        this.props.setProperty("protocol","");
        this.props.setProperty("max_redirects","24");
    }


    void RegisterModule(Module module) {
        module.Init(this);
        this.moduleList.put(module.GetModuleName(), module);
    }


    Module GetModule(String name) {
        return this.moduleList.getOrDefault(name, null);
    }


    void LoadProps(String propsPath) throws Exception {
        FileInputStream file_is = new FileInputStream(propsPath);
        InputStreamReader reader_is = new InputStreamReader(file_is, StandardCharsets.UTF_8);
        this.props.load(reader_is);
    }


    String GetProp(String key) {
        return this.props.getProperty(key,"");
    }


    void SetProp(String key, String value) {
        this.props.setProperty(key,value);
    }


    ChromeDriver GetDriver() {
        return driver;
    }


    atwebUrl GetCurrentPage() {
        return CurrentPage;
    }





    void LoadPage(atwebUrl url) {
        this.driver.get(url.urlDestination);
        this.CurrentPage = url;
    }


    void Clear() {
        //this.root.Clear();
        this.props.clear();
        this.moduleList.clear();
    }


    atwebInterface() {
        this.driver = null;
        //this.root = null;
        this.props = new Properties();
    }


    void Destroy() {
        for(Module module : this.moduleList.values()) {module.Destroy(); }
        if(this.GetProp("using_selenium").equals("true")) {this.driver.quit();}
        this.Clear();
    }
}
