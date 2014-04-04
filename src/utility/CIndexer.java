package utility;

import java.util.Vector;

public final class CIndexer
{
    //ds creates an index vector from m to n
    public static Vector< Integer > getIndexVector( int p_iM, int p_iN )
    {
        //ds allocate an empty vector
        Vector< Integer > vecIndexed = new Vector< Integer >( );
        
        //ds loop over the vector and add increasing indexes
        for( int u = p_iM; u <= p_iN; ++u )
        {
            //ds add the element
            vecIndexed.add( u );
        }
        
        //ds return the vector
        return vecIndexed;
    }
}
