import java.lang.reflect.Method;
import java.util.HashMap;

public class moduleDefault implements atwebInterface.Module {
    protected atwebInterface webInterface;
    protected HashMap<String, Method> methodMap;
    protected String name;


    @Override
    public String GetModuleName() {
        return this.name;
    }


    @Override
    public boolean Invoke(String methodName, String ... methodParams) {

        Method m = this.methodMap.get(methodName);
        boolean result = false;

        if(m != null) {

            try {
                if(methodParams != null) result = (Boolean) m.invoke(this, methodParams);
                else result = (Boolean) m.invoke(this);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return result;}


    @Override
    public void Init(atwebInterface webInterface) {
        /*this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = moduleTest.class.getDeclaredMethods();
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

        System.out.print("\n\n");*/
    }


    @Override
    public void Destroy() {
    }


    moduleDefault() {
        this.webInterface = null;
        this.methodMap = null;
        this.name = null;
    }


}
