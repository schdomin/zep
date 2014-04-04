package utility;

import java.util.Vector;

public final class CDataPoint
{
    //ds linked id
    private final int m_iLinkedID;
    
    //ds main attributes
    private final String m_strURL;
    private final String m_strTitle;
    private final String m_strType;
    private final Vector< String > m_vecTags;
    
    //ds constructor
    public CDataPoint( final int p_iLinkedID, final String p_strURL, final String p_strTitle, final String p_strType, final Vector< String > p_vecTags )
    {
        //ds set the fields
        m_iLinkedID = p_iLinkedID;
        m_strURL    = p_strURL;
        m_strTitle  = p_strTitle;
        m_strType   = p_strType;
        m_vecTags   = p_vecTags;
    }
    
    //ds copy constructor
    public CDataPoint( final CDataPoint p_cDataPoint )
    {
        //ds set the fields
        m_iLinkedID = p_cDataPoint.m_iLinkedID;
        m_strURL    = p_cDataPoint.m_strURL;
        m_strTitle  = p_cDataPoint.m_strTitle;
        m_strType   = p_cDataPoint.m_strType;
        m_vecTags   = p_cDataPoint.m_vecTags;
    }
    
    //ds getters
    public int getLinkedID( ){ return m_iLinkedID; }
    public String getURL( ){ return m_strURL; };
    public String getTitle( ){ return m_strTitle; };
    public String getType( ){ return m_strType; };
    public Vector< String > getTags( ){ return m_vecTags; };
}
