package test;

import java.sql.SQLException;

import org.sqlite.Function;

public class regexQuery extends Function {

    @Override
    protected void xFunc() throws SQLException {
        if (args() != 2) {
            throw new SQLException("IsSameDay(date1,date2): Invalid argument count. Requires 2, but found " + args());
        }
        try {
            DateTime t1 = DateTime.parse(value_text(0).replace(" ", "T"));
            DateTime t2 = DateTime.parse(value_text(1).replace(" ", "T"));
            if (t1.getYear() == t2.getYear() && t1.getDayOfYear() == t2.getDayOfYear()) {
                result(1);
            } else {
                result(0);
            }
        } catch (Exception exception) {
            throw new SQLDataException("IsSameDay(date1,date2): One of Arguments is invalid: " + exception.getLocalizedMessage());
        }
    }
}
