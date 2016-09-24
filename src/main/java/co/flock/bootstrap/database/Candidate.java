package co.flock.bootstrap.database;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = DbConstants.Table.CANDIDATE)
public class Candidate
{
    @DatabaseField(id = true, columnName = DbConstants.Fields.EMAIL, canBeNull = false)
    private String _email;

    @DatabaseField(columnName = DbConstants.Fields.NAME, canBeNull = false)
    private String _name;

    @DatabaseField(columnName = DbConstants.Fields.CREATOR_ID, canBeNull = false)
    private String _creatorId;

    @DatabaseField(columnName = DbConstants.Fields.CV_LINK, canBeNull = false)
    private String _cvLink;

    @DatabaseField(columnName = DbConstants.Fields.ROLE, dataType = DataType.ENUM_INTEGER, canBeNull = false)
    private ROLE _role;

    @DatabaseField(columnName = DbConstants.Fields.GROUP_ID, canBeNull = false)
    private String _groupId;

    @DatabaseField(columnName = DbConstants.Fields.CREATOR_NAME, canBeNull = false)
    private String _creatorName;

    public Candidate()
    {
    }

    public Candidate(String email, String name, String creatorId, String creatorName, String cvLink, ROLE role, String groupId)
    {
        _email = email;
        _name = name;
        _creatorId = creatorId;
        _creatorName = creatorName;
        _cvLink = cvLink;
        _role = role;
        _groupId = groupId;
    }

    public String getEmail()
    {
        return _email;
    }

    public String getName()
    {
        return _name;
    }

    public String getCreatorId()
    {
        return _creatorId;
    }

    public String getCreatorName()
    {
        return _creatorName;
    }

    public String getCvLink()
    {
        return _cvLink;
    }

    public ROLE getRole()
    {
        return _role;
    }

    public String getGroupId()
    {
        return _groupId;
    }

    public enum ROLE
    {
        PLATFORM, APPLICATION;
    }

    @Override
    public String toString()
    {
        return "Candidate{" +
                "_email='" + _email + '\'' +
                ", _name='" + _name + '\'' +
                ", _creatorId='" + _creatorId + '\'' +
                ", _cvLink='" + _cvLink + '\'' +
                ", _role=" + _role +
                ", _groupId='" + _groupId + '\'' +
                ", _creatorName='" + _creatorName + '\'' +
                '}';
    }
}
