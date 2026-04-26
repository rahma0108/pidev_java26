package interfaces;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {

    void add(T t) throws SQLException;
    List<T> getAll() throws SQLException;
    void update(T t) throws SQLException;
    void delete(int id) throws SQLException;
}