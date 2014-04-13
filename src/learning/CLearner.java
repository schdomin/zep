package learning;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

//ds custom imports
import utility.CIndexer;
import utility.CLogger;
import utility.CDataPoint;
import utility.CMySQLManager;
import exceptions.CZEPEoIException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;

public final class CLearner
{
    //ds enums
    public static enum ELearner{ LIKE, DISLIKE, IDC }
    
    //ds currently buffered data structure: [id][datapoint]
    private Map< Integer, CDataPoint > m_mapDataset = new HashMap< Integer, CDataPoint >( );
    
    //ds navigation attributes
    private int m_iCurrentImageID;
    
    //ds counts every image request
    private int m_iRequestCounter = 0;
    
    //ds vector with available image IDs and history
    private Vector< Integer > m_vecImageIDsAvailable = new Vector< Integer >( );
    private Vector< Integer > m_vecImageIDsPicked    = new Vector< Integer >( );
    
    //ds random number generator for first picks
    Random m_cRandomGenerator;
    
    //ds statistics: this vector keeps track of the LIKE/DISLIKE/.. sequence format: [id][label]
    private Map< Integer, ELearner > m_mapLabels  = new HashMap< Integer, ELearner >( );
    private int m_iNumberOfLikes    = 0;
    private int m_iNumberOfDislikes = 0;
    
    //ds mysql manager
    private final CMySQLManager m_cMySQLManager;
    
    //ds constructor
    public CLearner( final CMySQLManager p_cMySQLManager )
    {        
        //ds setup the manager
        m_cMySQLManager = p_cMySQLManager;

        //ds allocate a new random generator
        m_cRandomGenerator = new Random( );
        
        //ds initialize initial IDs with invalid indexes (since they will be chosen by a request)
        m_iCurrentImageID = -1;
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CLearner>(CLearner) CLearner instance allocated" );
    }
    
    //ds launcher
    public void launch( ) throws SQLException, CZEPMySQLManagerException, MalformedURLException
    {
        //ds check if empty
        if( m_cMySQLManager.isEmpty( ) )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CLearner>(launch) MySQL table is empty - have to wait for crawlers" );
        }
        else
        {
        	//ds obtain the complete dataset from the database
        	m_mapDataset = m_cMySQLManager.getDataset( 100 );
        	
        	//ds initialize the available indexes
        	m_vecImageIDsAvailable.addAll( CIndexer.getIndexVector( 1, m_mapDataset.size( ) ) );
        }
    }
    
    //ds simple getter - does not change the request counter for an update cycle
    public CDataPoint getCurrentDataPoint( )
    {
        //ds this call is safe since the image id gets only manipulated internally
        return new CDataPoint( m_mapDataset.get( m_iCurrentImageID ) );
    }
    
    //ds returns the next image
    public CDataPoint getNextDataPoint( final ELearner p_eFlag ) throws CZEPEoIException
    {
        //ds save the flag for the last image
        m_mapLabels.put( m_iCurrentImageID, p_eFlag );
        
        //ds check if LIKE/DISLIKE
        if(      ELearner.LIKE    == p_eFlag ){ ++m_iNumberOfLikes; }
        else if( ELearner.DISLIKE == p_eFlag ){ ++m_iNumberOfDislikes; }
        
    	//ds check if there are still IDs available
    	if( 0 != m_vecImageIDsAvailable.size( ) )
    	{
    		//ds get a new image id
    		m_iCurrentImageID = _getNewImageID( );
    		
    		//ds increase request counter
    		++m_iRequestCounter;
        
    		//ds return the new image
            return new CDataPoint( m_mapDataset.get( m_iCurrentImageID ) );
    	}
    	else
    	{
            
            //ds if we could fetch some data
            if( 0 != m_vecImageIDsAvailable.size( ) )
            {
                //ds get a new image id
                m_iCurrentImageID = _getNewImageID( );
                
                //ds increase request counter
                ++m_iRequestCounter;
            
                //ds return the new image
                return new CDataPoint( m_mapDataset.get( m_iCurrentImageID ) );                
            }
            else
            {
                //ds we cannot retrieve any further images
                throw new CZEPEoIException( "Reached end of image sequence" );    		
            }
    	}
    }
    
    //ds returns the previous image - does not change the request counter for an update cycle
    public CDataPoint getPreviousDataPoint( ) throws CZEPnpIException
    {
    	//ds check if there is a previous image available (means at least 2 images picked by GUI)
    	if( 1 < m_vecImageIDsPicked.size( ) )
    	{
    		//ds add the previous image to the vector again (if this call is used no labeling occurred)
    	    m_vecImageIDsAvailable.add( m_iCurrentImageID );
    	    
            //ds remove last LIKE/DISLIKE/..
            m_mapLabels.remove( m_iCurrentImageID );
    	    
            //ds set the id (the previous one)
            m_iCurrentImageID = m_vecImageIDsPicked.get( m_vecImageIDsPicked.size( )-2 );
    	    
    	    //ds and pop the last element of the picked ones
    	    m_vecImageIDsPicked.remove( m_vecImageIDsPicked.size( )-1 );
    		
            //ds decrease request counter
    		if( 0 < m_iRequestCounter ){ --m_iRequestCounter; }
        
    		//ds return last
            return new CDataPoint( m_mapDataset.get( m_iCurrentImageID ) );
    	}
    	else
    	{
    		//ds no previous picture available
    		throw new CZEPnpIException( "No previous image available" );
    	}
    }
    
    //ds reset function for the learner
    public void reset( )
    {
        //ds get a fresh index vector
        m_vecImageIDsAvailable = CIndexer.getIndexVector( 1, m_mapDataset.size( ) );
        
        //ds and clear the picked one
        m_vecImageIDsPicked.clear( );
        
        //ds reset internals
        m_iRequestCounter   = 0;
        m_iCurrentImageID   = -1;   
        m_iNumberOfLikes    = 0;
        m_iNumberOfDislikes = 0;
    }
    
    //ds statistic
    public int getNumberOfVisits( ){ return m_iRequestCounter-1; }
    public int getNumberOfLikes( ){ return m_iNumberOfLikes; }
    public int getNumberOfDislikes( ){ return m_iNumberOfDislikes; }
    public int getDatasetSize( ){ return m_mapDataset.size( ); }
    
    //ds retrieve a new image id
    private int _getNewImageID( )
    {
    	//ds first get a random index for the currently available images
    	final int iRandomIndex = m_cRandomGenerator.nextInt( m_vecImageIDsAvailable.size( ) );
    	
    	//ds access the element in the vector
    	final int iImageID = m_vecImageIDsAvailable.elementAt( iRandomIndex );
    	
    	//ds we remove the ID from the availability vector and add it to the picked ones
    	m_vecImageIDsAvailable.remove( iRandomIndex );
    	m_vecImageIDsPicked.add( iImageID );
    	
    	//ds return the ID
    	return iImageID;
    }
}
