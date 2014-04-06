package utility;

import java.net.URL;
import java.util.Vector;

public final class CDataPoint
{
    //ds linked id
    private final int m_iID;
    
    //ds attributes
    private final URL m_cURL;
    private final String m_strTitle;
    private final String m_strType;
    private final int m_iLikes;
    private final int m_iDislikes;
    private final int m_iCountComments;
    private final int m_iCountTags;
    private final boolean m_bIsPhoto;
    private final double m_dTextAmount;
    
    private final Vector< String > m_vecTags;
    
    //ds constructor
    public CDataPoint( final int p_iID, 
    		final URL p_cURL, 
    		final String p_strTitle, 
    		final String p_strType, 
    		final int p_iLikes,
    		final int p_iDislikes,
    		final int p_iCountComments,
    		final int p_iCountTags,
    		final boolean p_bIsPhoto,
    		final double p_dTextAmount,
    		final Vector< String > p_vecTags )
    {
        //ds set the fields
        m_iID        	 = p_iID;
        m_cURL      	 = p_cURL;
        m_strTitle   	 = p_strTitle;
        m_strType   	 = p_strType;
        m_iLikes      	 = p_iLikes;
        m_iDislikes  	 = p_iDislikes;
        m_iCountComments = p_iCountComments;
        m_iCountTags     = p_iCountTags;
        m_bIsPhoto       = p_bIsPhoto;
        m_dTextAmount 	 = p_dTextAmount;
        m_vecTags     	 = p_vecTags;
    }
    
    //ds copy constructor
    public CDataPoint( final CDataPoint p_cDataPoint )
    {
        //ds set the fields
        m_iID        	 = p_cDataPoint.m_iID;
        m_cURL       	 = p_cDataPoint.m_cURL;
        m_strTitle 	  	 = p_cDataPoint.m_strTitle;
        m_strType 	  	 = p_cDataPoint.m_strType;
        m_iLikes    	 = p_cDataPoint.m_iLikes;
        m_iDislikes	  	 = p_cDataPoint.m_iDislikes;
        m_iCountComments = p_cDataPoint.m_iCountComments;
        m_iCountTags     = p_cDataPoint.m_iCountTags;
        m_bIsPhoto       = p_cDataPoint.m_bIsPhoto;
        m_dTextAmount	 = p_cDataPoint.m_dTextAmount;
        m_vecTags    	 = p_cDataPoint.m_vecTags;
    }
    
    //ds getters
    public final int getID( ){ return m_iID; }
    public final URL getURL( ){ return m_cURL; }
    public final String getTitle( ){ return m_strTitle; }
    public final String getType( ){ return m_strType; }
    public final int getLikes( ){ return m_iLikes; }
    public final int getDislikes( ){ return m_iDislikes; }
    public final int getCountComments( ){ return m_iCountComments; }
    public final int getCountTags( ){ return m_iCountTags; }
    public final boolean isPhoto( ){ return m_bIsPhoto; }
    public final double getTextAmount( ){ return m_dTextAmount; }
    public final Vector< String > getTags( ){ return m_vecTags; }
}
