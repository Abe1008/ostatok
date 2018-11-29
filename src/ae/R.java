/*
 * Copyright (c) 2017. Aleksey Eremin
 * 16.03.17 21:03
 */

package ae;

import java.io.*;
import java.util.Properties;

/**
 * Created by ae on 28.01.2017.
 * Ресурсный класс
*/
/*
Modify:
25.09.18  сохраняем промежуточные файлы во временном каталоге
02.10.18  шаблон web содержимого в ресурсном файле content.html
04.10.18  даты хранятся и отображаются через LocalDate[Time]
05.10.18  читаем Paradox JDBC драйвером
          https://github.com/leonhad/paradoxdriver/releases/tag/v1.4.0
06.10.18  входной файл можно задать в командной строке
          дата операции берется из поля Date_Real
10.10.18  при расчете оплаты calcOpl() проход по массиву rop начинаем с i0
18.10.18  загрузка данных после отображения окна приложения и повторно по кнопке
19.10.18  имя таблицы Paradox и номер счета впаяны в код класса Paradox

*/

public class R {
  public final static String Ver = "3.6"; // номер версии

  // файл архив с базами данных
  public static String  ZipFile  = "C:/Users/ae/YANDEX~1/4741~1/Eremin17.den"; // "D:/Users/AE/Downloads/Eremin17.den";
  //public static int Id_account  = 4;    // номер счета
  public static int DayReport   = 29;     // Дата отчета по карте
  public static int DayPlus     = 20;     // льготный прериод
  public static int Limit       = 30000;  // кредитный лимит

  /**
   * Загрузить параметры по-умолчанию из БД таблицы "_Info"
   */
  public static void loadDefault()
  {
    // прочитать из БД значения часов выдержки
    Properties props = new Properties();
    try {
      props.load(R.class.getResourceAsStream("/res/default.properties"));
      // прочитаем параметры из конфигурационного файла default.properties
      ZipFile   = r2s(props, "ZipFile",   ZipFile);
      Limit     = r2s(props, "Limit",     Limit);
      DayReport = r2s(props, "DayReport", DayReport);
      DayPlus   = r2s(props, "DayPlus",   DayPlus);
      //
      System.out.println("ZipFile:   " + ZipFile);
      System.out.println("Limit:     " + Limit);
      System.out.println("DayReport: " + DayReport);
      System.out.println("DayPlus:   " + DayPlus);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /*
   * Пауза выполнения программы
   * @param time   время задержки, мсек
   */
/*
  static void sleep(long time)
  {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
*/

  /**
   * Выдать строковое значение из файла свойств, либо, если там
   * нет такого свойства, вернуть значение по-умолчанию
   * @param p                     свойства
   * @param NameProp              имя свойства
   * @param strResourceDefault    значение по-умолчанию
   * @return  значение свойства, а если его нет, то значение по-умолчанию
   */
  private static String r2s(Properties p, String NameProp, String strResourceDefault)
  {
    String str = p.getProperty(NameProp);
    if(str == null) {
      str = strResourceDefault;
    }
    return str;
  }

  /**
   * Выдать числовое (int) значение из файла свойств, либо, если там
   * нет такого свойства, вернуть значение по-умолчанию
   * @param p                     свойства
   * @param NameProp              имя свойства
   * @param intResourceDefault    значение по-умолчанию
   * @return  значение свойства, а если его нет, то значение по-умолчанию
   */
  private static int r2s(Properties p, String NameProp, int intResourceDefault)
  {
    String str = r2s(p,NameProp, Integer.toString(intResourceDefault));
    return Integer.parseInt(str);
  }

  /*
   * Поместить ресурс в байтовый массив
   * @param nameRes - название ресурса (относительно каталога пакета)
   * @return - байтовый массив
   */
/*
  public static ByteArrayOutputStream readResB(String nameRes)
  {
    try {
      // Get current classloader
      InputStream is = R.class.getClass().getResourceAsStream(nameRes);
      if(is == null) {
        System.out.println("Not found resource: " + nameRes);
        return null;
      }
      // https://habrahabr.ru/company/luxoft/blog/278233/ п.8
      BufferedInputStream bin = new BufferedInputStream(is);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      int len;
      byte[] buf = new byte[512];
      while((len=bin.read(buf)) != -1) {
        bout.write(buf,0,len);
      }
      return bout;
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }
*/

  /*
   * Записать в файл ресурсный файл
   * @param nameRes   имя ресурса (от корня src)
   * @param fileName  имя файла, куда записывается ресурс
   * @return  true - запись выполнена, false - ошибка
   */
/*
  public static boolean writeRes2File(String nameRes, String fileName)
  {
    boolean b = false;
    ByteArrayOutputStream buf = readResB(nameRes);
    if(buf != null) {
      try {
        FileOutputStream fout = new FileOutputStream(fileName);
        buf.writeTo(fout);
        fout.close();
        b = true;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return b;
  }
*/

  /**
   * Загружает текстовый ресурс в заданной кодировке
   * @param name      имя ресурса
   * @param code_page кодировка, например "Cp1251"
   * @return          строка ресурса
   */
  public static String getText(String name, String code_page)
  {
    StringBuilder sb = new StringBuilder();
    try {
      InputStream is = R.class.getClass().getResourceAsStream(name);  // Имя ресурса
      BufferedReader br = new BufferedReader(new InputStreamReader(is, code_page));
      String line;
      while ((line = br.readLine()) !=null) {
        sb.append(line);  sb.append("\n");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return sb.toString();
  }

  /*
   * Выполнить команду ОС, ожидать её завершения
   * Вызывать стоит через CMD:"CMD /C file.bat"
   * @param command команды на выполнение
   * @return  текст выданный командной строкой
   */
/*
  public static String execOS(String command)
  {
    String result="";
    Runtime r = Runtime.getRuntime();
    try {
      //Process p = r.exec("cmd /c "+command);
      Process p = r.exec(command);
      p.waitFor();
      BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = b.readLine()) != null) {
        result += line;
      }
      b.close();
    }
    catch(Exception ex) {
      ex.printStackTrace();
      return "?-ERROR-" + command;
    }
    return result;
  }
*/

} // end of class
