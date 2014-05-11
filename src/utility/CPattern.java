package utility;

import java.util.Comparator;
import java.util.Vector;

public final class CPattern
{
    //ds linked id
    private final int m_iID;
    
    //ds attributes
    private final String m_strTitle;
    
    //ds learning related
    private final boolean m_bIsAnimated;
    private final boolean m_bIsPhoto;
    private final boolean m_bIsText;
    private final boolean m_bIsLiked;
    private final boolean m_bIsHot;
    
    //ds probability of like
    private double m_dLikeliness = 0.0;
    
    //ds random field (always on if not classified)
    private boolean m_bIsRandom = true;
    
    //ds tags vector
    private final Vector< CTag > m_vecTags;
    
    //ds constructor
    public CPattern( final int p_iID, 
                     final String p_strTitle,
                     final boolean p_bIsAnimated,
                     final boolean p_bIsPhoto,
                     final boolean p_bIsText,
                     final boolean p_bIsLiked,
                     final boolean p_bIsHot,
                     final Vector< CTag > p_vecTags )
    {
        //ds set the fields
        m_iID         = p_iID;
        m_strTitle    = p_strTitle;
        m_bIsAnimated = p_bIsAnimated;
        m_bIsPhoto    = p_bIsPhoto;
        m_bIsText     = p_bIsText;
        m_bIsLiked    = p_bIsLiked;
        m_bIsHot      = p_bIsHot;
        m_vecTags     = new Vector< CTag >( p_vecTags );
    }
    
    //ds private constructor for cloning
    private CPattern( final int p_iID, 
                      final String p_strTitle,
                      final boolean p_bIsAnimated,
                      final boolean p_bIsPhoto,
                      final boolean p_bIsText,
                      final boolean p_bIsLiked,
                      final boolean p_bIsHot,
                      final Vector< CTag > p_vecTags,
                      final double p_dLikeliness,
                      final boolean p_bIsRandom )
    {
        //ds set the fields
        m_iID         = p_iID;
        m_strTitle    = p_strTitle;
        m_bIsAnimated = p_bIsAnimated;
        m_bIsPhoto    = p_bIsPhoto;
        m_bIsText     = p_bIsText;
        m_bIsLiked    = p_bIsLiked;
        m_bIsHot      = p_bIsHot;
        m_vecTags     = new Vector< CTag >( p_vecTags );
        m_dLikeliness = p_dLikeliness;
        m_bIsRandom   = p_bIsRandom; 
    }
    
    //ds copy constructor
    public CPattern( final CPattern p_cDataPoint )
    {
        //ds set the fields
        m_iID         = p_cDataPoint.m_iID;
        m_strTitle    = p_cDataPoint.m_strTitle;
        m_bIsAnimated = p_cDataPoint.m_bIsAnimated;
        m_bIsPhoto    = p_cDataPoint.m_bIsPhoto;
        m_bIsText     = p_cDataPoint.m_bIsText;
        m_bIsLiked    = p_cDataPoint.m_bIsLiked;
        m_bIsHot      = p_cDataPoint.m_bIsHot;
        m_vecTags     = new Vector< CTag >( p_cDataPoint.m_vecTags );
        m_dLikeliness = p_cDataPoint.m_dLikeliness;
        m_bIsRandom   = p_cDataPoint.m_bIsRandom;  
    }
    
    //ds comparator class
    public static final class CComparatorDecreasing implements Comparator< CPattern >
    {
        //ds comparing by probabilities
        public int compare( CPattern p_cPattern1, CPattern p_cPattern2 )
        {
            //ds check if equal
            if( p_cPattern1.m_dLikeliness == p_cPattern2.m_dLikeliness ){ return 0; }
            
            //ds check if bigger
            if( p_cPattern1.m_dLikeliness > p_cPattern2.m_dLikeliness ){ return -1; }
            
            //ds only smaller left
            return 1;
        }
    }
    
    //ds clone method
    public CPattern clone( )
    {
        return new CPattern( m_iID, m_strTitle, m_bIsAnimated, m_bIsPhoto, m_bIsText, m_bIsLiked, m_bIsHot, new Vector< CTag >( m_vecTags ), m_dLikeliness, m_bIsRandom );
    }
    
    //ds getters
    public final int getID( ){ return m_iID; }
    public final String getTitle( ){ return m_strTitle; }
    public final boolean isAnimated( ){ return m_bIsAnimated; }
    public final boolean isPhoto( ){ return m_bIsPhoto; }
    public final boolean isText( ){ return m_bIsText; }
    public final boolean isLiked( ){ return m_bIsLiked; }
    public final boolean isHot( ){ return m_bIsHot; }
    public final Vector< CTag > getTags( ){ return new Vector< CTag >( m_vecTags ); } //ds no manipulation possible
    public final Vector< CTag > getTagsModifiable( ){ return m_vecTags; }             //ds open for external manipulation
    
    //ds probability
    public final double getLikeliness( ){ return m_dLikeliness; }
    public final void setLikeliness( final double p_dLikeliness ){ m_dLikeliness = p_dLikeliness; }
    
    //ds randomness
    public final boolean isRandom( ){ return m_bIsRandom; }
    public final void setRandom( final boolean p_bIsRandom ){ m_bIsRandom = p_bIsRandom; }
}
