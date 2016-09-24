package co.flock.bootstrap.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = DbConstants.Table.USER)
public class User
{
    @DatabaseField(id = true, columnName = DbConstants.Fields.USER_ID, canBeNull = false)
    private String _id;
    @DatabaseField(columnName = DbConstants.Fields.TOKEN, canBeNull = false)
    private String _userToken;

    public User()
    {
    }

    public User(String id, String userToken)
    {
        _id = id;
        _userToken = userToken;
    }

    public String getId()
    {
        return _id;
    }

    public String getToken()
    {
        return _userToken;
    }

    @Override
    public String toString()
    {
        return "User{" +
                "_id='" + _id + '\'' +
                ", _userToken='" + _userToken + '\'' +
                '}';
    }
}
