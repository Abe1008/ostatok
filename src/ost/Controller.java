package ost;

import ae.Database;
import ae.R;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public class Controller
{
  @FXML
  private WebView           web_view;
  @FXML
  private Button            btn_reloadData;
  @FXML
  private TableView<Stroka> tbl_table;
  @FXML
  private TableColumn<Stroka, LocalDateTime>  col_dat;
  @FXML
  private TableColumn<Stroka,String>  col_val;
  @FXML
  private TableColumn<Stroka,String>  col_ost;
  @FXML
  private TableColumn<Stroka,String>  col_dolg;
  @FXML
  private TableColumn<Stroka,LocalDate>  col_dopl;
  @FXML
  private TableColumn<Stroka,LocalDate>  col_dend;

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private ObservableList<Stroka> usersData = FXCollections.observableArrayList();

  //private Label empty_label = new Label("Идет загрузка данных ...");
  /*
   предыдущая версия initialize
   public class Controller implements Initializable  {
     ...
      @Override
      public void initialize(URL location, ResourceBundle resources) {

   */
  // вызывается FXMLLoader'ом
  // http://qaru.site/questions/134029/javafx-fxml-controller-constructor-vs-initialize-method
  // https://docs.oracle.com/javase/8/javafx/api/javafx/fxml/doc-files/introduction_to_fxml.html#controllers
  //  .
  @FXML
  public void initialize()
  {
    // loadData();
    // отобразить другую надпись
    // https://stackoverflow.com/questions/24765549/remove-the-default-no-content-in-table-text-for-empty-javafx-table
    //
    //tbl_table.setPlaceholder(new Label("Загружаются данные..."));
    // загрузим данные, когда отрисуется окно
    //tbl_table.setPlaceholder(empty_label);
    Platform.runLater(() -> loadData());
  }

  public void onclick_btn_reloadData(ActionEvent e)
  {
    usersData.clear();  // очистить таблицу от данных
    loadData();         // загрузить данные
  }

  /**
   * Загрузить данные из БД Paradox
   */
  private void  loadData()
  {
    long t1 = new Date().getTime();       // время старт программы в мсек UNIX epoch

    Model model   = new Model();
    int a = model.loadData();
    if(a > 0) {
      // данные получены, будем отображать
      Database db = model.getDB();

      initData(db);
      col_dat.setCellValueFactory(cellData -> cellData.getValue().datProperty());
      col_val.setCellValueFactory(cellData -> cellData.getValue().valProperty());
      col_ost.setCellValueFactory(cellData -> cellData.getValue().ostProperty());
      col_dolg.setCellValueFactory(cellData -> cellData.getValue().dolgProperty());
      col_dopl.setCellValueFactory(cellData -> cellData.getValue().doplProperty());
      col_dend.setCellValueFactory(cellData -> cellData.getValue().dendProperty());
      // форматируем дату для ячеек таблицы
      // http://qaru.site/questions/1645726/formatting-an-objectpropertylocaldatetime-in-a-tableview-column
      col_dat.setCellFactory(col -> new TableCell<Stroka,LocalDateTime>() {
        @Override
        protected void updateItem(LocalDateTime item, boolean empty) {
          super.updateItem(item, empty);
          if(empty || null==item) setText(null);
          else                    setText(item.format(formatter));
        }
      });
      col_dopl.setCellFactory(col -> new TableCell<Stroka,LocalDate>() {
        @Override
        protected void updateItem(LocalDate item, boolean empty) {
          super.updateItem(item, empty);
          if(empty || item==null) setText(null);
          else                    setText(item.format(formatter));
        }
      });
      col_dend.setCellFactory(col -> new TableCell<Stroka,LocalDate>() {
        @Override
        protected void updateItem(LocalDate item, boolean empty) {
          super.updateItem(item, empty);
          if(empty || item==null) setText(null);
          else                    setText(item.format(formatter));
        }
      });

      // заполняем таблицу данными
      tbl_table.setItems(usersData);
      // создадим таблицу "задолженности"
      StringBuilder sb = new StringBuilder();
      ArrayList<String[]> arr =
          db.DlookupArray("SELECT dend, Sum(dolg) " +
              "FROM vals WHERE dolg not null GROUP BY dend");
      for(String[] s : arr) {
        sb.append("<tr><td width=40%>");
        sb.append(s2dat(s[0]));       // дата
        sb.append("</td><td align=right>");
        sb.append(s2ds(s[1], true));  // число
        sb.append("</td></tr>");
      }
      // загрузим содержимое страницы "задолженности"
      // в содержимом ищем шаблон @table@ и заменяем
      // его на таблицу
      String content = R.getText("/res/content.html", "UTF-8");
      content = content.replace("@table@", sb);
      // выведим страницу в web-окно
      WebEngine weng = web_view.getEngine();
      weng.loadContent(content);
    }

    long t2 = new Date().getTime();       // время стопа программы в мсек UNIX epoch
    System.out.println("execution time (s) " + (t2-t1)*0.001);
  }

  /**
   * Инициализировать таблицу данными
   * @param db  база данных
   */
  private void  initData(Database db)
  {
    ArrayList<String[]> arr = db.DlookupArray("SELECT dat,tim,val,ost,dolg,dopl,dend FROM vals ORDER BY dat DESC,tim DESC");
    for(String[] r: arr) {
      String dat  = r[0] + " " + r[1];
      String val  = s2ds(r[2],false);
      String ost  = s2ds(r[3],true);
      String dolg = s2ds(r[4], true);
      String dopl = r[5];
      String dend = r[6];
      usersData.add(new Stroka(dat,val,ost,dolg,dopl,dend));
    }
  }

  /**
   * Привести строку с числом к виду строки с числом и с 2 знаками после точки
   * @param str строка с числом
   * @param zeroCollapse  признак схлопывания 0
   * @return  выходная строка
   */
  private String  s2ds(String str, boolean zeroCollapse)
  {
    String out ="";
    try {
      double d = Double.parseDouble(str);
      if(Math.abs(d) > 0.009) {
        out = String.format("%.2f", d).replace(",", ".");
      } else {
        if(!zeroCollapse) out ="0";
      }
    } catch (Exception e) {
      //
    }
    return out;
  }

  /**
   * Преобразование даты из вида YYYY-MM-DD в DD.MM.YYYY
   * @param str строка с датой вида 2018-10-18
   * @return строка даты в виде 18.10.2018
   */
  private String  s2dat(String str)
  {
    if(str == null) return "";
    String[] par = str.split("-");
    return par[2] + "." + par[1] + "." + par[0];
  }

} // end of class
