import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.HashMap;

public class moduleFile extends moduleDefault {
    private String filePath;


    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = moduleFile.class.getDeclaredMethods();
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


    moduleFile() {
        this.name = "file";
    }


    public static boolean IsFileExist(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }


    public static boolean DeleteFile(String path) {
        File file = new File(path);
        boolean result = true;
        if( file.exists() && file.isFile() ) result = !file.delete();
        return result;
    }


    public boolean NewFile(String path) throws Exception {
        File file = new File(path);
        this.filePath = path;
        boolean result = true;

        if(file.exists()) {
            if (file.isFile()) {
                if(file.delete()) {
                    result = !file.createNewFile();
                } else {
                    System.out.printf("m_File::NewFile: %s - нельзя создать новый файл.\n", path);
                }
            } else {
                System.out.printf("m_File::NewFile: %s - это папка.\n", path);
            }
        } else {
            result = !file.createNewFile();
        }

        return result; // инвертируем результат (если true, то ошибка)
    }


    public boolean PushBack(String text) throws Exception {
        FileWriter writer = new FileWriter(this.filePath,true);
        writer.write(text);
        writer.close();
        return false;
    }


    // TODO objects to method invoker
    /*public boolean PushBackList(List<String> text) throws Exception {
        FileWriter writer = new FileWriter(this.file_path,true);
        for(String txt : text) {
            writer.write(txt);
        }
        writer.close();
        return false;
    }*/

}
