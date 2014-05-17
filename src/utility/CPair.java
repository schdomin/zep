package utility;

import java.util.Comparator;

//ds TODO implement more generic handling
public final class CPair< A, B >
{
    //ds attributes
    public A A;
    public B B;
    
    //ds constructor
    public CPair( final A A, final B B )
    {
        //ds consistency over bad style
        this.A = A;
        this.B = B;
    }

    //ds comparator class
    public static final class CComparatorDecreasing implements Comparator< CPair< ?, ? > >
    {
        //ds comparing by probabilities
        public int compare( CPair< ?, ? > p_cPair1, CPair< ?, ? > p_cPair2 )
        {
            //ds check if the values are numerical - integer
            if( p_cPair1.B instanceof Integer && p_cPair2.B instanceof Integer )
            {
                //ds get integer values for comparison
                final int iB1 = ( Integer ) p_cPair1.B;
                final int iB2 = ( Integer ) p_cPair2.B;
                
                //ds check if the second elements are equal
                if( iB1 == iB2 ){ return 0; }
                
                //ds check if bigger
                if( iB1 > iB2 ){ return -1; }
                
                //ds only smaller left
                return 1;
            }
            
            //ds check if the values are numerical - double
            if( p_cPair1.B instanceof Double && p_cPair2.B instanceof Double )
            {
                //ds get integer values for comparison
                final double dB1 = ( Double ) p_cPair1.B;
                final double dB2 = ( Double ) p_cPair2.B;
                
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
        if( p_cObject instanceof CPair< ?, ? > )
        {
            //ds cast to pair
            CPair< ?, ? > cPair = ( CPair< ?, ? > ) p_cObject;
            
            //ds call equals method of first element
            return cPair.A.equals( this.A );
        }
        else
        {
            //ds no comparison
            return false;
        }
    }
}
