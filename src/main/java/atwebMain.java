public class atwebMain {
    public static atwebInterface currentInterface;

    public static void main(String[] args) throws Exception {

        String propsFile = "robot.properties";
        if( args != null ) if(args.length > 0) {
            propsFile = args[0];
        }


        // вэб интерфейс
        atwebMain.currentInterface = new atwebInterface();
        atwebMain.currentInterface.LoadProps(propsFile);


        // результат тестирования
        boolean Result = false;


        // инициация
        try {
            atwebMain.currentInterface.Init();
        } catch(Exception e) {
            System.out.println("Unhandled error during test session!");
            e.printStackTrace();
            return;
        }


        // std modules
        atwebMain.currentInterface.RegisterModule(new moduleTest());
        atwebMain.currentInterface.RegisterModule(new moduleMail());
        atwebMain.currentInterface.RegisterModule(new moduleFile());
        atwebMain.currentInterface.RegisterModule(new moduleForm());


        // ext modules for testing
        atwebMain.currentInterface.RegisterModule(new testMultiPageForm());
        atwebMain.currentInterface.RegisterModule(new testUrlJumper());


        // invoke main method
        String[] main_method = atwebMain.currentInterface.GetProp("main_method").split(":");
        Result = atwebMain.currentInterface.GetModule(main_method[0]).Invoke(main_method[1]);


        if(Result) System.out.println("Unhandled error during test session!");


        // освобождение памяти и выход
        atwebMain.currentInterface.Destroy();
    }

}
