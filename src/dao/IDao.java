package dao;

import java.util.List;

import javax.sql.DataSource;

public interface IDao {

    void setDataSource(DataSource ds);

    void create(String firstName, String lastName);

    List<T> select(String firstname, String lastname);

    List<T> selectAll();

    void deleteAll();

    void delete(String firstName, String lastName);

}