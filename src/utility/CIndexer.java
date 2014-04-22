package utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import exceptions.CZEPEoIException;

public final class CIndexer
{
    //ds available ids for draft
    List< Integer > m_lstAvailableIDs = null;
    
    //ds current number of drafts
    int m_iIndexNumberOfDrafts = -1;
    
    //ds ctor
    public CIndexer( final int p_iNumberOfPatterns )
    {
        //ds allocate list
        m_lstAvailableIDs = _getRandomIndexList( 1, p_iNumberOfPatterns );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CIndexer>(CIndexer) Instance allocated" );
    }
    
    //ds draw new id from the available vector
    public final int drawID( ) throws CZEPEoIException
    {
        //ds move one step further in the chain if possible
        if( m_iIndexNumberOfDrafts < m_lstAvailableIDs.size( ) )
        {
            //ds increase index
            ++m_iIndexNumberOfDrafts;
        }
        else
        {
            //ds escape
            throw new CZEPEoIException( "no more IDs available" );
        }
        
        //ds return the current element
        return m_lstAvailableIDs.get( m_iIndexNumberOfDrafts );
    }
    
    //ds reset the draft position
    public final void reset( )
    {
        //ds next draft will be the first element
        m_iIndexNumberOfDrafts = -1;
    }
    
    //ds creates an index vector from m to n with shuffled entries
    private final List< Integer > _getRandomIndexList( int p_iM, int p_iN )
    {
        //ds allocate the list to fill
        List< Integer > lstIndexesSorted = new ArrayList< Integer >( p_iN-p_iM+1 );
        
        //ds loop over the vector and add set the indexes
        for( int u = p_iM; u <= p_iN; ++u )
        {
            //ds set the element
            lstIndexesSorted.add( u );
        }
        
        //ds allocate a second list which will contain the randomized elements (same size)
        List< Integer > lstIndexesRandom = new ArrayList< Integer >( p_iN-p_iM+1 );
        
        //ds random number generator for picking the indexes
        Random m_cRandomGenerator = new Random( );
        
        //ds start picking random numbers as long as there are elements in the sorted list
        while( !lstIndexesSorted.isEmpty( ) )
        {
            //ds get a random index on the available elements (from 0 to size-1)
            final int iIndex = m_cRandomGenerator.nextInt( lstIndexesSorted.size( ) );
            
            //ds add the element
            lstIndexesRandom.add( lstIndexesSorted.get( iIndex ) );
            
            //ds remove element from sorted list
            lstIndexesSorted.remove( iIndex );
        }
        
        //ds return the random list
        return lstIndexesRandom;
    }
}
