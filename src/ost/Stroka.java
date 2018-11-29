/*
 * Copyright (c) 2018. Aleksey Eremin
 * 02.10.18 13:44
 */

/*
  Строка данных выводимая на таблицу
 */
package ost;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Stroka {
  private final ObjectProperty<LocalDateTime> dat;
  private final StringProperty                val;
  private final StringProperty                ost;
  private final StringProperty                dolg;
  private final ObjectProperty<LocalDate>     dopl;  // дата когда оплачено
  private final ObjectProperty<LocalDate>     dend;  // дата срок оплаты

  // https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
  private final static DateTimeFormatter fmtldt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); //ISO_LOCAL_DATE_TIME; // 2011-12-03T10:15:30
  private final static DateTimeFormatter fmtld  = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //ISO_LOCAL_DATE; // 2011-12-03

  public LocalDateTime getDat() {
    return dat.get();
  }

  public ObjectProperty<LocalDateTime> datProperty() {
    return dat;
  }

  public void setDat(LocalDateTime dat) {
    //this.dat.set(dat);
    this.dat.setValue(dat);
  }

  public void setDat(String dat) {
    if(dat == null) {
      setDat((LocalDateTime) null);
    } else {
      // http://proglang.su/java/regular-expressions
      if(dat.matches("\\d+-\\d+-\\d+$"))
        dat = dat + " 00:00:00";
      LocalDateTime ld = LocalDateTime.parse(dat, fmtldt);
      this.dat.set(ld);
    }
  }

  public String getVal() {
    return val.get();
  }

  public StringProperty valProperty() {
    return val;
  }

  public void setVal(String val) {
    this.val.set(val);
  }

  public String getOst() {
    return ost.get();
  }

  public StringProperty ostProperty() {
    return ost;
  }

  public void setOst(String ost) {
    this.ost.set(ost);
  }

  public String getDolg() {
    return dolg.get();
  }

  public StringProperty dolgProperty() {
    return dolg;
  }

  public void setDolg(String dolg) {
    this.dolg.set(dolg);
  }

  // дата оплаты
  public LocalDate getDopl() {
    return dopl.get();
  }

  public ObjectProperty<LocalDate> doplProperty() {
    return dopl;
  }

  public void setDopl(LocalDate dopl) {
    //this.dopl.set(dopl);
    this.dopl.setValue(dopl);
  }

  public void setDopl(String dopl) {
    if(dopl == null) {
      setDopl((LocalDate)null);
    } else {
      LocalDate ld = LocalDate.parse(dopl, fmtld);
      setDopl(ld);
    }
  }

  // дата конечного срока
  public LocalDate getDend() {
    return dend.get();
  }

  public ObjectProperty<LocalDate> dendProperty() {
    return dend;
  }

  public void setDend(LocalDate dend) {
    //this.dend.set(dend);
    this.dend.setValue(dend);
  }

  public void setDend(String dend) {
    if(dend == null) {
      setDend((LocalDate)null);
    } else {
      LocalDate ld = LocalDate.parse(dend, fmtld);
      setDend(ld);
    }
  }


  public Stroka(String dat, String val, String ost, String dolg, String dopl, String dend)
  {
    //LocalDate d = LocalDate.now();
    this.dat = new SimpleObjectProperty<>();
    setDat(dat);
    this.val = new SimpleStringProperty(val);
    this.ost = new SimpleStringProperty(ost);
    this.dolg = new SimpleStringProperty(dolg);
    this.dopl = new SimpleObjectProperty<>();
    setDopl(dopl);
    this.dend = new SimpleObjectProperty<>();
    setDend(dend);
  }

}

