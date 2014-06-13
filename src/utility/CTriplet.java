package utility;

import java.util.Comparator;

//ds TODO implement more generic handling
public final class CTriplet< A, B, C >
{
    //ds attributes
    public A A;
    public B B;
    public C C;
    
    //ds constructor
    public CTriplet( final A A, final B B, final C C )
    {
        //ds consistency over bad style
        this.A = A;
        this.B = B;
        this.C = C;
    }

    //ds comparator class
    public static final class CComparatorDecreasing implements Comparator< CTriplet< ?, ?, ? > >
    {
        //ds comparing by probabilities
        public int compare( CTriplet< ?, ?, ? > p_cTriplet1, CTriplet< ?, ?, ? > p_cTriplet2 )
        {
            //ds check if the values are numerical - integer
            if( p_cTriplet1.B instanceof Integer && p_cTriplet2.B instanceof Integer )
            {
                //ds get integer values for comparison
                final int iB1 = ( Integer ) p_cTriplet1.B;
                final int iB2 = ( Integer ) p_cTriplet2.B;
                
                //ds check if the second elements are equal
                if( iB1 == iB2 ){ return 0; }
                
                //ds check if bigger
                if( iB1 > iB2 ){ return -1; }
                
                //ds only smaller left
                return 1;
            }
            
            //ds check if the values are numerical - double
            if( p_cTriplet1.B instanceof Double && p_cTriplet2.B instanceof Double )
            {
                //ds get integer values for comparison
                final double dB1 = ( Double ) p_cTriplet1.B;
                final double dB2 = ( Double ) p_cTriplet2.B;
                
                //ds check if the second elements are equal
                if( dB1 == dB2 ){ return 0; }
                
                //ds check if bigger
                if( dB1 > dB2 ){ return -1; }
                
                //ds only smaller left
                return 1;
            }
            
            //ds no ordering possible
            return 0;
        }
    }
    
    //ds equals method
    public boolean equals( Object p_cObject )
    {
        //ds check if we have a pair
        if( p_cObject instanceof CTriplet< ?, ?, ? > )
        {
            //ds cast to triplet
            CTriplet< ?, ?, ? > cTriplet = ( CTriplet< ?, ?, ? > ) p_cObject;
            
            //ds call equals method of first element
            return cTriplet.A.equals( this.A );
        }
        else
        {
            //ds no comparison
            return false;
        }
    }
}
