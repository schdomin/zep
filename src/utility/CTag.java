package utility;

import java.util.Vector;

//ds triplet to hold the tag information: id/value/frequency
public final class CTag
{
    //ds attributes
    private final int    m_iID;
    private final String m_strValue;
    private final int    m_iFrequency;

    //ds ctor
    public CTag( final int p_iID, final String p_strValue, final int p_iFrequency )
    {
        //ds set values
        m_iID        = p_iID;
        m_strValue   = p_strValue;
        m_iFrequency = p_iFrequency;
    }
    
    //ds getters
    public final int getID( ){ return m_iID; }
    public final String getValue( ){ return m_strValue; }
    public final int getFrequency( ){ return m_iFrequency; }
    
    //ds get maximum frequency from tag set
    public static int getMaximumFrequency( final Vector< CTag > p_vecTags )
    {
        //ds current frequency
        int iMaximumFrequency = 0;
        
        //ds loop over tags
        for( CTag cTag: p_vecTags )
        {
            //ds get frequency
            final int iFrequency = cTag.getFrequency( );
            
            //ds if we are above maximum update it
            if( iMaximumFrequency < iFrequency ){ iMaximumFrequency = iFrequency; }
        }
        
        return iMaximumFrequency;
    }
}
