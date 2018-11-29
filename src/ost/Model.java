/*
 * Copyright (c) 2018. Aleksey Eremin
 * 24.09.18 15:04
 */

package ost;

import ae.Database;
import ae.DatabaseSqlite;
import ae.R;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Model {
  private Database db;
  // строка создания таблицы
  private static final String createTable =
      "CREATE TABLE vals(" +
      "id   INTEGER," + // ид.записи
      "dat  DATE," +    // дата рсахода(оплаты)
      "tim  TIME," +    // время
      "val  REAL," +    // сумма (- расход, + оплата)
      "ost  REAL," +    // остаток на счете
      "opl  REAL," +    // оплата (служебное поле)
      "dolg REAL," +    // долг, остаток от расхода
      "dopl DATE," +    // дата погашения задолженности
      "dend DATE)";     // дата оплаты (без процентов)

  private final DateTimeFormatter formatDate = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"

  public Model()
  {
    //db = new DatabaseSqlite("c:/tmp/ostatok.db");
    //db = new DatabaseSqlite(":memory:");  //file:memdb1?mode=memory&cache=shared
    db = new DatabaseSqlite(""); // база в памяти, при необходимости переезжает на диск
    existDb();
  }

  public Database getDB()
  {
    return db;
  }

  /**
   * Проверим начличие БД, если нет базы и/или таблицы vals, то они создаются.
   */
  private void existDb()
  {
    String str= db.Dlookup("SELECT COUNT(*) FROM vals");
    if(str == null) {
      // похоже нет таблицы?! создадим ее
      db.ExecSql(createTable);
    }
  }

  /**
   * Загрузка данных
   * @return кол-во загруженных строк данных
   */
  public int  loadData()
  {
    //
    int a;
    // прочитать из Paradox в Sqlite
    a = read2db();
    System.out.println("Прочитано строк данных: " + a);
    if (a > 0) {
      // просчитать данные
      // остаток и даты оплаты
      calcOstDend();
      // рассчитать даты фактических олплат и остатки долга
      calcOpl();
    }
    return a;
  }

  /**
   * Читать данные из Paradox в Sqlite.
   * Файл хранится в параметре R.ZipFile.
   * @return кол-во загруженных строк данных
   */
  private int read2db()
  {
    // конвертировать данные бух учета в БД
    Paradox pdx = new Paradox();
    // имя файла с текстовыми данными из Parodox-a
    int cnt = pdx.copyData(R.ZipFile, db);
    return cnt;
  }

  /**
   * Просчитать остаток на счете, расставить оплаты и определить даты оплаты
   */
  private void  calcOstDend()
  {
    double d = 0; // остаток по счету
    // расчет остатка на счета и даты оплаты для каждого расхода
    // массив изменения id, ost, dat
    ArrayList<String[]> ar = db.DlookupArray("SELECT id,dat,val FROM vals");
    db.ExecSql("BEGIN TRANSACTION;");
    for(String[] s: ar) {
      String id  = s[0];  // ид.
      String dat = s[1];  // дата
      String val = s[2];  // сумма
      double dv  = Double.parseDouble(val); // сумма
      d += dv;  // накапливаем остаток
      String sd;
      if(dv < 0) {
        // если расход, то определим дату погашения
        sd = "dend='" + datOplat(dat) + "'";  // дата расхода -> дата оплаты
      } else {
        sd = "opl=" + val;                    // поле opl - внесена оплата
      }
      String sq = "UPDATE vals SET " + sd + ", ost=" + d2s(d) + " WHERE id=" + id;
      int a = db.ExecSql(sq);
      // if(a > 0) System.out.print(".");
    }
    db.ExecSql("COMMIT;");
    System.out.println(" ");
  }

  /**
   * Расчет оплаты
   */
  private void  calcOpl()
  {
    // начальные суммы оплаты - уже проставлены
    //db.ExecSql("UPDATE vals SET opl=val WHERE val>0");
    // расчет остатка на счета и даты оплаты для каждого расхода
    // массив изменения id, ost, dat

    // массив расходов
    ArrayList<String[]> ras =
        db.DlookupArray("SELECT id,val,dat FROM vals " + // ,dat,opl,dolg
        "WHERE val<0 ORDER BY dat,tim");

    // массив оплат
    ArrayList<String[]> rop =
        db.DlookupArray("SELECT id,dat,opl FROM vals " + // ,val,dolg
            "WHERE opl>0 ORDER BY dat,tim");

    // TODO
    db.ExecSql("BEGIN TRANSACTION;");
    int i0 = 0; // начальный индекс для просмотра массива оплат rop
    for(int j = 0; j < ras.size(); j++) {
      String[] s = ras.get(j);
      String id1  = s[0]; // ид.
      String val1 = s[1]; // сумма
      double dr = 0 - Double.parseDouble(val1); // сумма расхода
      // дата оплаты платежа, по дате последней потребовавшейся оплаты
      String dat2 = null;
      // перебираем оплату
      for (int i = i0; i < rop.size() && dr > 0; i++) {
        String[] p = rop.get(i);
        String id2  = p[0]; // ид. платежа
        dat2        = p[1]; // дата платежа
        String opl2 = p[2]; // оплата
        double opl = Double.parseDouble(opl2);
        if(opl > 0) {
          double d;
          if (dr > opl) {
            dr = dr - opl;
            d = 0.0;
            i0 = i + 1; // сместим начальный индекс в массивае оплат
          } else {
            d = opl - dr;
            dr = 0.0;
          }
          String sd = d2s(d);
          p[2] = sd; // остаток оплаты заносим в массив rop
          db.ExecSql("UPDATE vals SET opl=" + sd + " WHERE id=" + id2);
        }
      }
      String upda;
      if (dr > 0) {
        upda = "dolg=" + d2s(dr);
      } else {
        upda = "dopl='" + dat2 + "'";
      }

      String sq = "UPDATE vals SET " + upda + " WHERE id=" + id1;
      int a = db.ExecSql(sq);
      // if (a > 0) System.out.print(".");
    }
    // TODO
    db.ExecSql("COMMIT;");
    System.out.println(" ");
    // добавим к остатку кредитный лимит
    String sql;
    sql = "UPDATE vals SET ost=ost+" + R.Limit;
    db.ExecSql(sql);
  }

  /**
   * Преобразовать действительное число в строку с 2 знаками после точки
   * @param d действительное число
   * @return  строка с 2 знаками после точки
   */
  public static String  d2s(double d)
  {
    return String.format("%.2f", d).replace(",", ".");
  }

  /**
   * Расчет даты льготного периода погашения задолженности
   * @param sdat  дата расхода "YYYY-MM-DD"
   * @return  дата льготного периода "YYYY-MM-DD" строка
   */
  private String datOplat(String sdat)
  {
    final int xReport = R.DayReport;  // число даты очета
    final int xPlus = R.DayPlus;      // льготный период
    LocalDate dat = LocalDate.parse(sdat, formatDate);
    int d = dat.getDayOfMonth();      // число дня расхода
    //int m = dat.getMonthValue();
    //int y = dat.getYear();
    LocalDate dp; // дата расплаты
    try {
      dp = dat.withDayOfMonth(xReport);
    } catch (DateTimeException e) {
      // дата вышла за границу дозволенного
      dp = dat.withDayOfMonth(1);
      dp = dp.plusMonths(1).minusDays(1); // последний день месяца
    }
    if(d >= xReport) {
      dp = dp.plusMonths(1);
    }
    LocalDate dop;
    dop = dp.plusDays(xPlus); // прибавим льготный период
    String  out;
    out = dop.format(formatDate);
    return out;
  }

}
