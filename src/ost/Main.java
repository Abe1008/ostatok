/*
 * Copyright (c) 2018. Aleksey Eremin
 * 24.09.18 14:05
 */

/*
    Расчет остатка по кредитной карте Сбербанка на основе
    данных их CashFly
 */

package ost;

import ae.R;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.TimeZone;

// TODO
// посмотреть на будущее
// https://blog.ngopal.com.np/2011/10/19/dyanmic-tableview-data-from-database/comment-page-1/
// -

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
      Parent root = FXMLLoader.load(getClass().getResource("display.fxml"));
      // иконка приложения
      primaryStage.getIcons().add(new Image("/res/money.png"));
      // заголовок приложения
      primaryStage.setTitle("Остаток Сбера v " + R.Ver);
      // главная сцена
      primaryStage.setScene(new Scene(root, 600, 400));
      // стили прописываем в самом .fxml
      // primaryStage.getScene().getStylesheets().add("css/mystyle.css"); //подключим стили
      primaryStage.show();
    }

    public static void main(String[] args)
    {
      if(args.length > 0) {
        R.ZipFile = args[0];
      }
      //
      // https://stackoverflow.com/questions/2493749/how-to-set-a-jvm-timezone-properly
      // установим время UTC, чтобы правильно прочитать время из Paradox
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      //
      ae.R.loadDefault();
      //
      launch(args);
    }

}
