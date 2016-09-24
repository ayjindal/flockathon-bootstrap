package co.flock.bootstrap.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "candidate")
public class Candidate
{
    @DatabaseField(id = true, columnName = "id", canBeNull = false)
    private String _id;
    @DatabaseField(columnName = "email", canBeNull = false)
    private String _email;

    @DatabaseField(columnName = "name", canBeNull = false)
    private String _name;

    public Candidate()
    {
    }

    public String get_id()
    {
        return _id;
    }

    public String get_email()
    {
        return _email;
    }

    public String get_name()
    {
        return _name;
    }
}
