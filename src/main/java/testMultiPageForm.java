import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class testMultiPageForm extends moduleDefault {

    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = testMultiPageForm.class.getDeclaredMethods();
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


    testMultiPageForm() {
        this.name = "multipageform";
    }


    public boolean Run() {
        /*String mainPage = this.webInterface.GetProp("main_page");
        String[] subPages = this.webInterface.GetProp("sub_page").split(";");
        String jsonFormDescription = this.webInterface.GetProp("json_form_description");
        String jsonFormValues = this.webInterface.GetProp("json_form_values");
        atwebInterface.Module formModule = this.webInterface.GetModule("form");

        for(String url : subPages) {
            atwebUrl nextURL = null;

            try {
                nextURL = this.webInterface.ConnectToURL(mainPage + url, true, true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(nextURL != null) this.webInterface.LoadPage(nextURL);

            formModule.Invoke("GetForm",jsonFormDescription);
            formModule.Invoke("SendForm",jsonFormValues);
        }
*/
        return false;
    }

}
