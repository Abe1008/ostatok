/*
 * Copyright (c) 2017. Aleksey Eremin
 * 28.01.17 21:26
 */

package ae;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by ae on 28.01.2017.
 * База данных Paradox - это каталог с файлами-таблицами
 * https://github.com/leonhad/paradoxdriver
 */
public class DatabaseParadox extends Database
{
    private String f_databaseName;  // имя файлы базы данных
    
    /**
     * Конструктор
     * в нем формируется имя базы данных
     */
    public DatabaseParadox(String databaseName)
    {
      f_databaseName = databaseName;
    }
    
    /**
     * Возвращает соединение к базе данных
     * @return соединение к БД
     */
    @Override
    public synchronized Connection getDbConnection()
    {
        if(f_connection == null) {
            try {
                Class.forName("com.googlecode.paradox.Driver");
                f_connection = DriverManager.getConnection("jdbc:paradox:/" + f_databaseName);
                //Class.forName("com.hxtt.sql.paradox.ParadoxDriver");
                //f_connection = DriverManager.getConnection("jdbc:Paradox:/" + f_databaseName);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
        return f_connection;
    }

} // end of class
