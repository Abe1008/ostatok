/*
 * Copyright (c) 2018. Aleksey Eremin
 * 25.09.18 11:00
 */

/*
   Извлечение из Paradox данных в текстовый файл
 */

package ost;

import ae.Database;
import ae.DatabaseParadox;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Paradox {

  Paradox()  {;}

  /**
   * Извлекает из архива с Paradox данные по нужной базе в выходной файл
   * @param zipFile архив с базами Paradox
   * @param db      БД, куда записываем результат в таблицу vals
   * @return кол-во записей
   */
  public int  copyData(String zipFile, Database db)
  {
    db.ExecSql("DELETE FROM vals");
    // извлечь файлы Paradox в выходной каталог
    String outPdxDir = extractPdx(zipFile);
    if(outPdxDir == null)
      return 0;
    // БД Paradox, указываем каталог с файлами Paradox, название файла = название таблицы
    Database dbpdx = new DatabaseParadox(outPdxDir);
    int cnt = 0;
    try {
      String sql;
      Connection conn = dbpdx.getDbConnection();
      conn.setReadOnly(true);
      Statement stm = dbpdx.getDbStatement();
      // Прочитаем данные из Paradox
      // Tb_Hist название таблицы БД Paradox, которая нужна для извлечения данных (это имя файла в каталоге Paradox)
      // Id_Account = 4  - отслеживаемый счет
      sql = "SELECT Date_Real, Date_Create, Dt, Ct FROM Tb_Hist WHERE Id_Account=4";
      ResultSet rst = stm.executeQuery(sql);
      int id = 0;
      db.ExecSql("BEGIN TRANSACTION;");
      sql = "INSERT INTO vals(id,dat,tim,val) VALUES(?,?,?,?)";
      PreparedStatement prs = db.getDbConnection().prepareStatement(sql);
      while (rst.next()) {
        String datr = rst.getString(1);  // взять i-ый столбец;
        String datc = rst.getString(2);
        String sdt = rst.getString(3);
        String sct = rst.getString(4);
        String dat = datr.substring(0, 10);   // дата
        String tim = datc.substring(11, 19);  // время (условное для сортировки)
        double dt = 0;
        double ct = 0;
        // https://o7planning.org/ru/10175/java-regular-expressions-tutorial
        // https://proselyte.net/tutorials/java-core/regular-expressions/
        final String regdbl = "\\d+[.]?\\d*"; // выражение для числа
        if(sdt.matches(regdbl)) {
          dt = Double.parseDouble(sdt);
        }
        if(sct.matches(regdbl)) {
          ct = Double.parseDouble(sct);
        }
        // баланс + пополнение, - расход
        double val = dt - ct;
        id++;
        prs.setInt(1,    id);
        prs.setString(2, dat);
        prs.setString(3, tim);
        prs.setDouble(4, val);
        int a = prs.executeUpdate();
        cnt += a;
        //System.out.println("Record: " + cnt + ") | " + s1 + " | " + dat + " | " + tim + " | " + s2 + " | " + s3);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    dbpdx.close();
    db.ExecSql("COMMIT;");
    // удалить каталог с базой Paradox
    deleteDirFile(new File(outPdxDir));
    return cnt;
  }

  /**
   * Создает временный каталог, куда извлекает таблицы Paradox
   * из входного файла.
   * @param zipFile входной файл с архивом таблиц Paradox
   * @return название выходного каталога
   */
  private String extractPdx(String zipFile)
  {
    String  outDir= "ost";
    try {
      // создадим временный каталог
      Path basedir = Files.createTempDirectory(outDir);
      outDir = basedir.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    if( !unZipFile(zipFile, outDir) ) {
      System.err.println("?-Error-невозможно распаковать архив " + zipFile + " в каталог " + outDir);
      return null;
    }
    return outDir;
  }

  /**
   * Удаляет файл или каталог (рекурсивно со всеми вложенными файлами и каталогами)
   * @param element файл или каталог
   */
  private void deleteDirFile(File element)
  {
    if(element.isDirectory()) {
      for (File sub : element.listFiles()) {
        deleteDirFile(sub);
      }
    }
    element.delete();
  }

  /**
   * Распаковать архив в выходной каталог
   * https://examples.javacodegeeks.com/core-java/util/zip/zipinputstream/java-unzip-file-example/
   * @param zipFile   архив
   * @param outputDir выходной каталог
   * @return  признак распаковки
   */
  private boolean unZipFile(String zipFile, String outputDir)
  {
    File directory = new File(outputDir);

    // if the output directory doesn't exist, create it
    if(!directory.exists())
      directory.mkdirs();

    // buffer for read and write data to file
    byte[] buffer = new byte[2048];

    try {
      FileInputStream fInput = new FileInputStream(zipFile);
      ZipInputStream zipInput = new ZipInputStream(fInput);

      ZipEntry entry = zipInput.getNextEntry();

      while(entry != null){
        String entryName = entry.getName();
        File file = new File(outputDir + File.separator + entryName);

        ///System.out.println("Unzip file " + entryName + " to " + file.getAbsolutePath());

        // create the directories of the zip directory
        if(entry.isDirectory()) {
          File newDir = new File(file.getAbsolutePath());
          if(!newDir.exists()) {
            boolean success = newDir.mkdirs();
            if(!success) {
              System.err.println("Problem creating Folder");
            }
          }
        }
        else {
          FileOutputStream fOutput = new FileOutputStream(file);
          int count;
          while ((count = zipInput.read(buffer)) > 0) {
            // write 'count' bytes to the file output stream
            fOutput.write(buffer, 0, count);
          }
          fOutput.close();
        }
        // close ZipEntry and take the next one
        zipInput.closeEntry();
        entry = zipInput.getNextEntry();
      }

      // close the last ZipEntry
      zipInput.closeEntry();

      zipInput.close();
      fInput.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

} // end of class
