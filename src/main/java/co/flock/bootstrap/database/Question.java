package co.flock.bootstrap.database;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import static co.flock.bootstrap.database.Candidate.*;

@DatabaseTable(tableName = DbConstants.Table.QUESTION)
public class Question
{
    @DatabaseField(columnName = DbConstants.Fields.QUESTION_ID, canBeNull = false, generatedId = true)
    private int _id;

    @DatabaseField(columnName = DbConstants.Fields.GROUP_ID)
    private String _groupID;

    @DatabaseField(columnName = DbConstants.Fields.TITLE, canBeNull = false)
    private String _title;

    @DatabaseField(columnName = DbConstants.Fields.TEXT, canBeNull = false)
    private String _text;

    @DatabaseField(columnName = DbConstants.Fields.ROLE, dataType = DataType.ENUM_INTEGER, canBeNull = false)
    private ROLE _role;

    @DatabaseField(columnName = DbConstants.Fields.LEVEL, dataType = DataType.ENUM_INTEGER, canBeNull = false)
    private LEVEL _level;

    public Question(String title, String text, ROLE role, LEVEL level)
    {
        _title = title;
        _text = text;
        _role = role;
        _level = level;
    }

    public Question()
    {
    }

    public int getId()
    {
        return _id;
    }

    public String getGroupID()
    {
        return _groupID;
    }

    public String getText()
    {
        return _text;
    }

    public ROLE getRole()
    {
        return _role;
    }

    public LEVEL getLevel()
    {
        return _level;
    }

    public String getTitle()
    {
        return _title;
    }

    public enum LEVEL
    {
        EASY, MEDIUM, HARD
    }

    @Override
    public String toString()
    {
        return "Question{" +
                "_id=" + _id +
                ", _groupID='" + _groupID + '\'' +
                ", _title='" + _title + '\'' +
                ", _text='" + _text + '\'' +
                ", _role=" + _role +
                ", _level=" + _level +
                '}';
    }
}
