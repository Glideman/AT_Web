import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Scanner;

public class OutputFile {
    private String file_path;


    public boolean push_back(String text) throws Exception {
        FileWriter writer = new FileWriter(this.file_path,true);
        writer.write(text);
        writer.close();
        return false;
    }


    public boolean push_back_list(List<String> text) throws Exception {
        FileWriter writer = new FileWriter(this.file_path,true);
        for(String txt : text) {
            writer.write(txt);
        }
        writer.close();
        return false;
    }


    public boolean new_file(String path) throws Exception {
        File file = new File(path);
        this.file_path = path;
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                if(file.delete()) {
                    result = file.createNewFile();
                } else {
                    System.out.printf("OutputFile::open: %s - нельзя создать новый файл.\n", path);
                }
            } else {
                System.out.printf("OutputFile::open: %s - это папка.\n", path);
                result = false;
            }
        } else {
            result = file.createNewFile();
        }

        return !result; // инвертируем результат (если true, то ошибка)
    }


    public boolean bk_dump(PageJumper Jpr, String f_Name) throws Exception {

        File file = new File(f_Name);
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                if(file.delete()) {
                    result = file.createNewFile();
                } else {
                    System.out.printf("OutputFile::dump_bk: %s - нельзя создать новый файл.\n", f_Name);
                }
            } else {
                System.out.printf("OutputFile::dump_bk: %s - это папка.\n", f_Name);
                result = false;
            }
        } else {
            result = file.createNewFile();
        }

        if(!result) return true;

        FileWriter writer = new FileWriter(f_Name,true);

        String get_params_as_new_pages_s = Jpr.get_params_as_new_pages ? "t" : "f";
        String export_sub_pages_in_file_s = Jpr.export_sub_pages_in_file ? "t" : "f";
        String test_out_of_homep_urls_s = Jpr.test_out_of_homep_urls ? "t" : "f";
        String print_in_console_s = Jpr.print_in_console ? "t" : "f";
        String print_subp_in_console_s = Jpr.print_subp_in_console ? "t" : "f";

        writer.write(get_params_as_new_pages_s + ";");
        writer.write(export_sub_pages_in_file_s + ";");
        writer.write(test_out_of_homep_urls_s + ";");
        writer.write(print_in_console_s + ";");
        writer.write(print_subp_in_console_s + ";");
        writer.write(Jpr.GetStartPage() + "\n");

        for(WebPageExt t_pg : Jpr.GetRoot().GetChildList()) {
            bk_dump_pg(t_pg, writer);
        }

        writer.close();
        return false;
    }


    public void bk_dump_pg(WebPageExt pg, FileWriter f) throws Exception {
        Integer http_status = pg.GetHttpStatus();
        String t_pg_tested = pg.calcTested() ? "t" : "f";
        if(pg.isGetParameter())
            f.write("g;" + http_status.toString() + ";" + pg.GetTime() + ";" + t_pg_tested + ";" + pg.GetAddress() + ";" + "\n");
        else
            f.write("p;" + http_status.toString() + ";" + pg.GetTime() + ";" + t_pg_tested + ";" + pg.GetAddress() + ";" + "\n");

        for(WebPageExt t_pg_child : pg.GetChildLinkList()) {
            f.write("l;" + t_pg_child.GetPackedAddress() + ";" + "\n");
        }

        for(WebPageExt t_pg_child : pg.GetChildList()) {
            bk_dump_pg(t_pg_child, f);
        }
        f.write("r\n");
    }


    public boolean bk_get(PageJumper Jpr, String f_Name) throws Exception {

        File file = new File(f_Name);
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                result = true;
            } else {
                System.out.printf("OutputFile::get_bk: %s - это папка.\n", f_Name);
                result = false;
            }
        } else {
            result = false;
        }

        if(!result) return true;

        FileReader reader = new FileReader(f_Name);
        Scanner scan = new Scanner(reader);

        String n_line;
        String[] s_line;

        n_line = scan.nextLine();

        if(n_line != null) {
            s_line = n_line.split(";");

            Jpr.get_params_as_new_pages = s_line[0].equals("t");
            Jpr.export_sub_pages_in_file = s_line[1].equals("t");
            Jpr.test_out_of_homep_urls  = s_line[2].equals("t");
            Jpr.print_in_console = s_line[3].equals("t");
            Jpr.print_subp_in_console = s_line[4].equals("t");
            Jpr.SetStartPage(s_line[5]);
        } else return true;

        WebPageExt CurrentPg = Jpr.GetRoot();
        WebPageExt SubPg = null;
        while(scan.hasNextLine()) {
            n_line = scan.nextLine();
            //System.out.println(n_line);
            s_line = n_line.split(";");
            if(s_line[0].equals("p") || s_line[0].equals("g")) {
                SubPg = new WebPageExt();

                int http_status = Integer.parseInt(s_line[1]);
                boolean is_tested = s_line[3].equals("t");

                SubPg.SetHttpStatus(http_status);
                SubPg.SetTime(Long.parseLong(s_line[2]));
                SubPg.SetTested(is_tested);
                if(!n_line.endsWith(";"))
                    SubPg.SetAddress(s_line[4]);


                if(s_line[0].equals("g")) SubPg.SetGetParameter(true);


                CurrentPg.AddChild(SubPg);


                if( SubPg.GetFullAddress().startsWith(Jpr.GetStartPage()) ) {
                    SubPg.SetShouldTestThisPage(true);
                    SubPg.SetShouldSearchLinks(true);
                } else {
                    if(Jpr.test_out_of_homep_urls) { // если стоит флажок на тестирование сторонних сайтов и т.п. то сохраняем его в список, но без поиска ссылок
                        SubPg.SetShouldTestThisPage(true);
                        SubPg.SetShouldSearchLinks(false);
                    } else {
                        SubPg.SetShouldTestThisPage(false);
                        SubPg.SetShouldSearchLinks(false);
                    }
                }


                CurrentPg = SubPg;
            }

            else if(s_line[0].equals("l")) {
                String comment = "";
                if(!CurrentPg.GetComment().isEmpty()) comment = CurrentPg.GetComment() + ";";
                comment = comment.concat(s_line[1]);
                CurrentPg.SetComment(comment);
            }

            else if(s_line[0].equals("r")) {
                CurrentPg = CurrentPg.GetParent();
            }
        }

        reader.close();

        for(WebPageExt page : Jpr.GetRoot().GetChildList()) {
            Jpr.CommentsToLinks(page);
        }


        return false;
    }


    public boolean sitemap_gen(PageJumper Jpr, String f_Name) throws Exception {

        File file = new File(f_Name);
        this.file_path = f_Name;
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                if(file.delete()) {
                    result = file.createNewFile();
                } else {
                    System.out.printf("OutputFile::sitemap_gen: %s - нельзя создать новый файл.\n", f_Name);
                }
            } else {
                System.out.printf("OutputFile::sitemap_gen: %s - это папка.\n", f_Name);
                result = false;
            }
        } else {
            result = file.createNewFile();
        }

        if(!result) return true;

        FileWriter writer = new FileWriter(this.file_path,true);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        writer.close();

        Jpr.GetRoot().PrintInFile_xml(this, Jpr);

        writer = new FileWriter(this.file_path,true);
        writer.write("</urlset>");
        writer.close();

        return false;
    }


    public static boolean is_file_exist(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }


    public static void delete_file(String path) {
        File file = new File(path);
        if( file.exists() && file.isFile() ) file.delete();
    }
}
